# Cykl wg realnych mg + awaryjna edycja dawki

Data: 2026-06-15
Aplikacja: Hormon Wzrostu Dawkowanie (Android, Kotlin/Compose)

## Problem

Dwa zgłoszenia z użytkowania:

1. **Cykl gubi się przy pominięciu dnia.** Obecnie dzień cyklu liczony jest wyłącznie
   z kalendarza: `dayIndexInCycle = (dni od startu) % daysPerCycle`
   ([Schedule.kt:54](../../../app/src/main/java/pl/hormonwzrostu/data/Schedule.kt)).
   Gdy danego dnia nie podam dawki (oznaczę „Pominięto"), kalendarz leci dalej — następnego
   dnia mam już dzień 6 zamiast wciąż dnia 5. Cykl powinien stać, dopóki dawka nie zostanie
   faktycznie podana.

2. **Brak awaryjnej edycji podanej dawki.** Dawka nie jest nigdzie zapisywana — jest liczona
   „w locie" z planu (`doseForDay`,
   [Schedule.kt:69](../../../app/src/main/java/pl/hormonwzrostu/data/Schedule.kt)). Jeśli
   przegapię, że to dzień ostatniej (większej) dawki, i podam np. 0,8 zamiast 1,2 mg, nie mam
   jak skorygować zapisu.

## Decyzje projektowe (zatwierdzone)

- **Model cyklu = licznik podań** (pominięcie nie przesuwa cyklu).
- **Śledzenie realnych mg**: ampułka kończy się, gdy zużyto całą jej zawartość; korekta dawki
  realnie wpływa na to, kiedy ampułka się skończy (resztka → dodatkowy dzień; przedawkowanie →
  krótszy cykl).
- **Domyślnie trzymamy się rozpiski.** Pole mg jest tylko wstępnie wypełnione planowaną dawką;
  brak ręcznej zmiany = brak zapisu override = liczenie wg planu. Edycja mg to rzadkość awaryjna.
- **A:** `daysPerCycle` przestaje być stałą — przy realnych mg jest zmienna. Na karcie „dziś"
  pokazujemy ją jako wartość szacowaną (np. „~12"); realny koniec wynika ze zużycia.
- **B:** Edycja dawki działa też dla dnia wcześniej pominiętego — wpisanie dawki zamienia go
  z „Pominięto" (czerwony) na „Podano" (zielony) przez to samo pole w dialogu dnia.

## Model danych

Dodajemy mapę override'ów faktycznie podanych mg, obok istniejących `intake` i `comments`:

- `doses: Map<String, Double>` — data ISO (`yyyy-MM-dd`) → faktycznie podane mg.
- Brak wpisu dla daty = „podano zgodnie z planem".
- Wpis istnieje tylko dla dat obecnych w `intake` (override ma sens wyłącznie dla dni podanych).

Rozszerzenia warstwy trwałości:

- `ScheduleRepository`: nowy klucz `intake_doses` (JSON), metody `loadDoses()` / `saveDoses()`.
- `Backup`: nowe pole `doses: Map<String, Double> = emptyMap()`. Stare kopie (bez pola) wczytują
  się poprawnie dzięki `ignoreUnknownKeys` + domyślnej wartości. `version` podbijamy do `2`
  (informacyjnie; brak twardej migracji).
- `MainViewModel`: stan `doses`, metoda `setActualDose(date, mg?)` (null/pusto → usunięcie
  override), getter `actualDoseFor(date)`.

## Rdzeń — `CycleTimeline` (jeden przebieg po historii)

Nowy, czysty (bez zależności od Androida) plik `data/CycleTimeline.kt`. Zamiast liczyć dzień
z kalendarza, przechodzimy po podanych dawkach w kolejności dat i symulujemy zużycie ampułki.

Wynik jednego podania:

```kotlin
data class DoseEvent(
    val date: LocalDate,
    val cycleNumber: Int,    // numer ampułki, 1-based
    val dayInCycle: Int,     // pozycja w ampułce, 1-based
    val plannedMg: Double,   // dawka wg planu dla tego slotu przy danym remaining
    val actualMg: Double,    // faktycznie podana (override lub = planned)
    val isLastInCycle: Boolean, // to podanie domknęło ampułkę (remaining <= 0 po odjęciu)
)
```

Algorytm (`buildTimeline(schedule, intake, doses): List<DoseEvent>`):

```
remaining = ampouleMg;  cykl = 1;  dzieńWCyklu = 0
dla każdej daty z intake, rosnąco, od startDate wzwyż:
    jeśli remaining <= EPS:                 // poprzednia dawka domknęła ampułkę
        cykl++;  remaining = ampouleMg;  dzieńWCyklu = 0
    dzieńWCyklu++
    planned  = if (remaining >= 2 * dailyDose) dailyDose else remaining
    actual   = doses[date] ?: planned
    remaining -= actual
    emit DoseEvent(date, cykl, dzieńWCyklu, planned, actual, isLastInCycle = remaining <= EPS)
```

Reguła „ostatniego dnia" odwzorowuje istniejące `computeCycleDays`: dopóki w ampułce zostają
≥ 2 pełne dawki — dawka dzienna; gdy mniej — cała reszta. Liczone z tolerancją `EPS`
(np. 1e-6) i/lub na zaokrąglonych tysięcznych mg, by uniknąć błędów zmiennoprzecinkowych.

Funkcje pochodne (też w `CycleTimeline.kt`):

- `nextDose(schedule, intake, doses, fromDate): NextDose?` — stan dla pierwszej *niepodanej*
  dawki (np. dziś): kontynuuje przebieg o jeden hipotetyczny krok i zwraca `cykl`, `dzieńWCyklu`,
  `plannedMg`, `isLastInCycle`. Używane przez kartę „dziś" i powiadomienie.
- `eventByDate(timeline): Map<LocalDate, DoseEvent>` — szybki dostęp dla dialogu i eksportu.
- `estimatedDaysPerCycle(schedule)` — szacunkowa długość cyklu (= dotychczasowe
  `computeCycleDays`) do etykiety „~M".

Edge case'y obsłużone jawnie:

- resztka mniejsza niż dawka dzienna → osobny, krótszy ostatni dzień;
- przedawkowanie (`actual > remaining`) → `remaining` schodzi ≤ 0, ampułka domknięta, bez
  wartości ujemnych w prezentacji;
- pusta historia → następna dawka to cykl 1, dzień 1;
- override = 0 lub ujemny → ignorowany/traktowany jak brak (walidacja pola wejściowego).

## UI

### Karta „dziś" — `MainScreen.TodayDoseCard`
- Dawka, numer dnia i ostrzeżenie „ostatnia dawka" pobierane z `nextDose(...)`, nie z kalendarza.
- Etykieta „dzień N z ~M" (M = `estimatedDaysPerCycle`, z tyldą jako wartość szacowana).

### Dialog dnia — `Calendar.DayEditDialog`
- Nowe pole liczbowe **„Podana dawka (mg)"**, wstępnie wypełnione: override jeśli istnieje, inaczej
  planowana dawka dla tego dnia (z timeline lub projekcji).
- „Podano": jeśli wartość pola różni się od planowanej → zapis do `doses`; jeśli równa planowanej →
  brak override (czyszczenie ewentualnego starego). Zawsze ustawia `intake` = podano.
- „Pominięto": usuwa z `intake` oraz czyści override w `doses` dla tej daty.
- Numer dnia cyklu i status z `eventByDate`. Dla dnia pominiętego pole dawki ukryte; dla dnia
  przyszłego — projekcja informacyjna bez twardego numeru.

## Kalendarz, eksport, powiadomienia

- **Kalendarz** ([Calendar.kt](../../../app/src/main/java/pl/hormonwzrostu/ui/Calendar.kt)):
  kolory i statusy bez zmian (zielony/czerwony/dziś/później; status nadal z `dayStatus`).
  Numer dnia cyklu w dialogu z `eventByDate`.
- **Eksport xlsx** (`buildIntakeRows`,
  [Adherence.kt:50](../../../app/src/main/java/pl/hormonwzrostu/data/Adherence.kt)): wiersze
  „podano" pokazują **faktyczną** dawkę (`actualMg`) i prawdziwy `dayInCycle` z timeline; dni
  pominięte pozostają jako luki ze statusem „Pominięto" (kolumna dnia/dawki pusta).
- **Powiadomienie** (`ReminderReceiver`,
  [ReminderReceiver.kt:19](../../../app/src/main/java/pl/hormonwzrostu/notify/ReminderReceiver.kt)):
  dawka, numer dnia i „ostatnia dawka?" z `nextDose(...)`. Wczytuje też `doses` z repozytorium.

## Migracja danych

Brak twardej migracji. Po aktualizacji historia (`intake`) zostaje przeliczona nowym przebiegiem:
- tam gdzie nie było pominięć — wyniki pokrywają się z dotychczasowymi;
- tam gdzie były przerwy — cykl zostaje „naprawiony" (zachowanie pożądane).
`doses` startuje puste → wszystkie historyczne dawki = planowane (zgodnie z dotychczasową logiką).

## Testy

Lekki test JVM (bez Androida) na `CycleTimeline.buildTimeline` / `nextDose`, sploty:

- równy podział (np. 0,5 mg z 10 mg → 20 równych dni, brak resztki);
- standardowa resztka ostatniego dnia (0,8 mg → 11×0,8 + 1,2);
- pominięcie w środku — cykl nie przesuwa się, dzień powtarza się aż do podania;
- korekta w dół (0,8 zamiast 1,2) → dodatkowy dzień z resztką 0,4;
- korekta w górę / przedawkowanie → krótszy cykl, brak wartości ujemnych;
- przejście między ampułkami (cykl 1 → 2) z resztką;
- pusta historia → następna dawka = cykl 1, dzień 1.

## Pliki

Nowe:
- `app/src/main/java/pl/hormonwzrostu/data/CycleTimeline.kt`
- test JVM w `app/src/test/java/...` (lub minimalny runner zgodny z konfiguracją projektu)

Modyfikowane:
- `data/Backup.kt` — pole `doses`
- `data/ScheduleRepository.kt` — load/save `doses`, w backupie
- `MainViewModel.kt` — stan + `setActualDose` / `actualDoseFor`
- `ui/MainScreen.kt` — `TodayDoseCard` na `nextDose`, przekazanie `doses`
- `ui/Calendar.kt` — pole mg w `DayEditDialog`, numer dnia z `eventByDate`
- `data/Adherence.kt` — `buildIntakeRows` na timeline (faktyczna dawka, dzień z przebiegu)
- `notify/ReminderReceiver.kt` — `nextDose` zamiast `dayIndexInCycle`
- `res/values*/strings.xml` — etykieta pola „Podana dawka (mg)", ewentualne „~M"

## Poza zakresem (YAGNI)

- Historia zmian dawki / audyt.
- Edycja pojemności ampułki w trakcie trwającego cyklu (zmiana schematu działa jak dotąd).
- Ostrzeżenia o przekroczeniu dawki dziennej.
