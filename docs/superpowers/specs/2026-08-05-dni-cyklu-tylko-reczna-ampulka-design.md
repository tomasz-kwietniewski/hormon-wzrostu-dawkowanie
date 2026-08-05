# Dni cyklu: nowa ampułka wyłącznie po ręcznym oznaczeniu

Data: 2026-08-05
Status: zatwierdzone do wdrożenia

## Problem

Model zakładał, że ampułka 10 mg wystarcza dokładnie na 12 dni po 0,8 mg (11 × 0,8 + 1,2 mg
resztki). Rzeczywistość jest inna: dozownik jest niedokładny i realnie udaje się podać więcej,
niż ampułka teoretycznie zawiera. Cykl regularnie trwa 13, czasem 14 dni.

Dotychczasowa logika (`walk()` w `CycleTimeline.kt`) startowała nowy cykl, gdy symulowana
zawartość ampułki spadła do zera **albo** gdy użytkownik ręcznie oznaczył nową ampułkę.
Pierwszy warunek fałszował historię.

Realny przebieg z eksportu `hormon_Jeremiasz_2026-08-05.xlsx`:

| data | dzień wg aplikacji | podano | teoretyczna reszta | uwagi |
|---|---|---|---|---|
| 2026-07-30 | 11 | 0,8 | 1,2 | |
| 2026-07-31 | 12 | 0,8 (plan 1,2) | 0,4 | użytkownik zawsze podaje 0,8 |
| 2026-08-01 | pominięto | - | 0,4 | wyjazd |
| 2026-08-02 | 13 | 0,8 (plan 0,4) | -0,4 | model uznaje ampułkę za pustą |
| 2026-08-03 | **1** (błąd, ma być 14) | 0,2 | - | realna końcówka ampułki |
| 2026-08-04 | 1 | 1,2 | - | ręczne „nowa ampułka" |

Ten sam wzorzec wystąpił 2026-06-20 i 2026-07-03 - oba dni opisane w komentarzu jako „koniec
ampułki", oba fałszywie ponumerowane jako dzień 1.

Drugi problem: w dniu 12 aplikacja sama podstawiała 1,2 mg do pola dawki. Użytkownik często
klika „podano" bez patrzenia, więc automat zapisywał dawkę, której faktycznie nie podano.

## Zasady docelowe

1. **Dzień 1 nowego cyklu wyłącznie wtedy, gdy użytkownik oznaczy „nowa ampułka".**
   Wyczerpanie teoretycznej zawartości nie przesuwa licznika - dni lecą 13, 14, 15...
2. **Dawka proponowana zawsze równa dawce dziennej** (np. 0,8 mg), niezależnie od stanu
   ampułki. Żadna liczba nie wpisuje się sama do pola dawki.
3. **Stan ampułki to informacja, nie sterowanie.** Sugestia „możesz podać więcej i zamknąć
   ampułkę" trafia do treści powiadomienia oraz do UI, ale nigdy nie podmienia kwoty.

## Model

### Stan ampułki

Liczony z symulowanej zawartości **przed** daną dawką (`remainingBefore`), przy dawce dziennej `d`:

| stan | warunek | znaczenie |
|---|---|---|
| `NORMAL` | `remainingBefore >= 2d` | zwykły dzień |
| `LAST_FULL` | `d <= remainingBefore < 2d` | ostatnia pełna dawka; resztę można dociągnąć |
| `REMNANT` | `0 < remainingBefore < d` | w ampułce została już tylko końcówka |
| `EMPTY` | `remainingBefore <= 0` | ampułka powinna być pusta |

`remainingBefore` może być ujemne i takie zostaje - jest miarą tego, o ile realne podania
przekroczyły teoretyczną pojemność. Zeruje się wyłącznie przy re-kotwicy.

### Zmiany w `CycleTimeline.kt`

- `walk()`: nowy cykl tylko przy `isAmpouleAnchor(date)`; znika warunek `remaining <= 0L`.
- `plannedTh` zawsze równe `p.dailyTh` (znika `if (remaining >= 2 * dailyTh) dailyTh else remaining`).
- `DoseEvent`: pole `isLastInCycle: Boolean` zastąpione przez `remainingBeforeMg: Double`
  i `ampouleState: AmpouleState` (stan wyliczony przed podaniem tej dawki).
- `NextDose`: analogicznie - `isLastInCycle` ustępuje miejsca `remainingBeforeMg`
  i `ampouleState`.

`Schedule.daysPerCycle` i `lastDayDoseMg` zostają bez zmian - opisują teoretyczną pojemność
ampułki i nadal są poprawne jako informacja w ustawieniach oraz jako mianownik w „dzień 14/~12".

## Migracja danych

Nowa reguła znosi auto-przeskoki, a część granic ampułek w istniejących danych to właśnie
auto-przeskoki (np. 2026-07-19), nie zapisane kliknięcia. Bez migracji licznik po takiej
granicy poleciałby dalej (14, 15, 16...) i cała historia by się rozjechała.

Nowy plik `CycleMigration.kt` z czystą funkcją:

```
fun legacyAutoAnchors(
    schedule: Schedule,
    intake: Set<String>,
    doses: Map<String, Double>,
    ampouleStarts: Set<String>,
): Set<String>
```

Odtwarza starą logikę (auto-start przy `remaining <= 0`, dawka planowana = reszta gdy
`remaining < 2d`), zbiera daty auto-przeskoków i zwraca je jako nowe re-kotwice, **pomijając
te, po których najbliższe kolejne podanie ma już ręczną re-kotwicę**. To reguła, która
naprawia trzy fałszywe „dni 1" i zachowuje prawdziwe granice.

Wynik na danych użytkownika:

- 2026-06-20 -> dzień 13 (pominięte, bo 2026-06-21 ma ręczną re-kotwicę)
- 2026-07-03 -> dzień 13 (pominięte, bo 2026-07-04 ma ręczną re-kotwicę)
- 2026-07-19 -> dzień 1 (zapisane jako re-kotwica - brak ręcznej nazajutrz)
- 2026-08-03 -> dzień 14 (pominięte, bo 2026-08-04 ma ręczną re-kotwicę)

Uruchomienie: `ScheduleRepository.migrateAmpouleAnchorsIfNeeded()`, jednorazowo pod flagą
`ampoule_anchor_migration_v2` w SharedPreferences. Wywoływane przy starcie (inicjalizacja
`MainViewModel`) oraz po imporcie kopii w wersji niższej niż 5.

`Backup.version` rośnie z 4 do 5. Import kopii `version < 5` uruchamia migrację; import
kopii `version >= 5` jej nie potrzebuje (dane są już w nowym modelu).

## UI i powiadomienia

Trzy miejsca pokazują ten sam komunikat wynikający z `ampouleState`:

1. karta „Dzisiejsza dawka" (`MainScreen.kt`),
2. dialog edycji dnia (`Calendar.kt`) - jako tekst pomocniczy pod polem dawki,
3. wieczorne powiadomienie (`ReminderReceiver.kt`) - jako sufiks treści.

Teksty (PL; `values` zawiera wersję EN):

- `LAST_FULL`: „⚠ W ampułce jest jeszcze %s mg - możesz podać całość i zamknąć ampułkę."
- `REMNANT`: „⚠ W ampułce zostało ok. %s mg - to już końcówka."
- `EMPTY`: „⚠ Ampułka powinna być pusta. Po otwarciu nowej zaznacz „Nowa ampułka"."

Dotychczasowe `last_dose_warning` i `notif_last_suffix` znikają - ich treść („ostatnia dawka,
jutro otwórz nową") była nieprawdziwa: realnie z ampułki szło jeszcze jedno lub dwa podania.

Kwota w powiadomieniu i wartość wstępna pola dawki to zawsze dawka dzienna. Przy `REMNANT`
oznacza to celową rozbieżność: tytuł „dziś 0,8 mg", treść „zostało ok. 0,4 mg". Liczba
w tytule jest propozycją do zatwierdzenia, tekst niżej opisuje stan ampułki.

Format licznika `dzień %d/~%d` zostaje bez zmian - przekroczenie widać wprost jako „dzień 14/~12".

## Testy

`CycleTimelineTest` - nowe i zmienione przypadki:

- realny przebieg 2026-07-19 .. 2026-08-04 z pominięciami 07-29 i 08-01: dni 1..13,
  a 08-03 to dzień 14 tej samej ampułki; 08-04 z re-kotwicą to dzień 1;
- dawka planowana równa dziennej także wtedy, gdy w ampułce zostało 1,2 mg lub 0,2 mg;
- brak auto-startu cyklu po przekroczeniu pojemności (dzień 20 wciąż w tej samej ampułce);
- progi `AmpouleState` na granicach `2d`, `d` i `0`.

`CycleMigrationTest` - nowy:

- auto-przeskok, po którym nazajutrz jest ręczna re-kotwica -> pominięty;
- auto-przeskok bez ręcznej re-kotwicy w kolejnym podaniu -> zapisany;
- dane bez auto-przeskoków -> pusty wynik;
- pominięte dni między auto-przeskokiem a ręczną re-kotwicą nie psują dopasowania
  (liczy się najbliższe kolejne **podanie**, nie kolejny dzień kalendarzowy).

Uruchomienie lokalne bez Gradle: `~\.hormon-kotlin-runner\run-core-tests.ps1 -Repo <worktree>`
(runner wymaga dopisania `CycleMigration.kt` do listy źródeł i nowej klasy testowej do mapy).
Pełny build i testy instrumentalne - w CI.

## Poza zakresem

- Zmiana ekranu ustawień („Obliczony cykl i ostatnia dawka") - opis teoretycznej pojemności
  pozostaje poprawny.
- Wyróżnianie w kalendarzu dni po wyczerpaniu ampułki - ostrzeżenie na karcie dnia i
  w powiadomieniu wystarcza.
