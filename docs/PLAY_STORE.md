# Publikacja w Google Play — przygotowanie

Praktyczna lista kontrolna i gotowe teksty do wystawienia aplikacji **Hormon Wzrostu Dawkowanie**
w Sklepie Play.

> **STATUS (2026-06-15): OPUBLIKOWANA PUBLICZNIE na produkcji** — 177 krajów, wersja 1.15,
> wydawana z **konta organizacji** (Z Sensem Tomasz Kwietniewski).
> Link: https://play.google.com/store/apps/details?id=pl.hormonwzrostu
> Najważniejszy wniosek z procesu (konto organizacji dla aplikacji zdrowotnej) — patrz **sekcja 11**.

## 1. Wymagania wstępne

- [ ] Konto **Google Play Developer** (jednorazowa opłata 25 USD).
- [ ] Dostęp do **Play Console** (https://play.google.com/console).
- [ ] Klucz podpisujący (mamy: `release.keystore`, alias `hormon`) — będzie **kluczem przesyłania**
      (upload key). Google włącza **Play App Signing** i zarządza kluczem dystrybucyjnym.
      ⚠️ Trzymaj kopię `release.keystore` w bezpiecznym miejscu — bez niego nie wyślesz aktualizacji.

## 2. Artefakt do wgrania

Sklep Play wymaga **Android App Bundle (.aab)**, podpisanego.

- CI: workflow **`.github/workflows/release.yml`** (uruchom ręcznie *Run workflow* lub utwórz tag `vX.Y`)
  buduje `app-release.aab` i wystawia jako artefakt.
- Lokalnie:
  ```
  gradle bundleRelease
  # -> app/build/outputs/bundle/release/app-release.aab
  ```

## 3. Dane aplikacji (Store listing)

**Nazwa aplikacji:** Hormon Wzrostu Dawkowanie

**Krótki opis (PL, ≤80 zn.):**
> Codzienne przypomnienia o dawce hormonu wzrostu + historia podań i eksport.

**Pełny opis (PL):**
> Prosta aplikacja, która codziennie o ustalonej porze przypomina o dawce hormonu wzrostu (lub
> innego leku z ampułki dzielonej na cykl). Sama liczy ostatnią dawkę z ampułki, pozwala odznaczać
> podane dawki, prowadzi historię w formie kalendarza z komentarzami, eksportuje dane do Excela
> i robi kopie zapasowe (eksport/import).
>
> • Codzienne powiadomienie o wybranej godzinie
> • Auto-liczenie ostatniej dawki z ampułki
> • Oznaczanie „podano/pominięto" — także wstecz, z komentarzem
> • Historia-kalendarz z licznikiem podań
> • Eksport do Excela (.xlsx) i kopie zapasowe (JSON)
> • Język polski i angielski
> • Dane wyłącznie lokalnie — bez konta i bez internetu
>
> Uwaga: aplikacja tylko przypomina o dawce wpisanej przez rodzica. Nie zastępuje ulotki leku ani
> zaleceń lekarza i nie udziela porad medycznych. Dawkowanie zawsze ustala lekarz.

**Krótki opis (EN, ≤80):**
> Daily growth-hormone dose reminders with intake history and Excel export.

**Pełny opis (EN):** jak wyżej, wersja angielska (UI aplikacji ma już pełne tłumaczenie).

## 4. Grafiki (gotowe)

Wszystkie materiały w katalogu [`play-assets/`](../play-assets/):

- [x] **Ikona** 512×512 PNG — `play-assets/icon-512.png`.
- [x] **Grafika promocyjna (feature graphic)** 1024×500 PNG — `play-assets/feature-1024x500.png`.
- [x] **Zrzuty ekranu telefonu** (4 szt., 1080×1920, 9:16) — `play-assets/screenshots/`:
      ekran główny (kalendarz + dawka), okno dnia (podano/pominięto + komentarz),
      ustawienia (auto‑liczenie dawki), język + kopie zapasowe.
      Wierne makiety z UI; generowane przez `play-assets/screenshots/gen.py` (render Edge headless).

## 5. Klasyfikacja i odbiorcy

- [ ] **Kwestionariusz oceny treści (IARC)** — brak przemocy/treści dla dorosłych → kategoria dla wszystkich.
- [ ] **Grupa docelowa i treść** — aplikacja jest **narzędziem dla rodzica/opiekuna**, nie jest
      kierowana do dzieci jako użytkowników. Wybierz grupę wiekową dorosłych/opiekunów.
- [ ] **Aplikacje zdrowotne / medyczne** — przypomnienie o lekach: zadeklaruj zgodnie z formularzem
      „Health" i zaznacz, że to narzędzie przypominające, nie urządzenie medyczne i nie udziela porad.

## 6. Bezpieczeństwo danych (Data safety)

- **Czy aplikacja zbiera lub udostępnia dane użytkownika?** → **Nie** (dane tylko lokalnie; eksport
  inicjuje wyłącznie użytkownik i sam wybiera odbiorcę).
- **Szyfrowanie w trakcie przesyłania** → nie dotyczy (brak przesyłania).
- **Możliwość usunięcia danych** → tak (odinstalowanie aplikacji usuwa dane; brak konta).
- Polityka prywatności (URL): wskaż surowy plik [PRIVACY.md] z repozytorium lub stronę GitHub Pages,
  np. `https://github.com/tomasz-kwietniewski/hormon-wzrostu-dawkowanie/blob/main/PRIVACY.md`.

## 7. Uprawnienia do zadeklarowania

- **USE_EXACT_ALARM** — Play ogranicza to uprawnienie do aplikacji typu budzik/kalendarz/przypomnienia.
  W deklaracji uzasadnij: „aplikacja przypomina o dawce leku o dokładnej, ustalonej przez użytkownika
  godzinie — funkcja przypomnienia wymaga dokładnego alarmu". To kwalifikujący się przypadek użycia.
- **POST_NOTIFICATIONS**, **RECEIVE_BOOT_COMPLETED**, **REQUEST_IGNORE_BATTERY_OPTIMIZATIONS** — standardowe,
  uzasadnione niezawodnością przypomnień.

## 8. Dostęp do aplikacji (App access)

- Brak logowania — zaznacz „cała funkcjonalność dostępna bez specjalnego dostępu/konta".

## 9. Wydanie

- [ ] Ścieżka **Testy wewnętrzne** (Internal testing) — dodaj e-maile testerów (np. żona, inni rodzice);
      instalacja idzie ze Sklepu Play, więc działa też przy Google Advanced Protection.
- [ ] Po testach: **Produkcja** (lub Testy zamknięte/otwarte).
- [x] `versionCode`/`versionName` rosną z każdym wydaniem (w `app/build.gradle.kts`; obecnie **1.15 / 16**).

## 10. Uwagi techniczne

- `targetSdk`/`compileSdk` = 36 (Android 16; spełnia wymóg Google Play od 31 sierpnia 2026).
- `minSdk` = 26 (Android 8.0+).
- Minifikacja (R8) włączona od v1.23 (`isMinifyEnabled = true`): mniejszy rozmiar + `mapping.txt`
  w AAB (czytelne raporty crashy w Play). Model danych `pl.hormonwzrostu.data.**` chroniony
  regułami keep w `proguard-rules.pro` (bezpieczna serializacja backupów).
- Symbole debugowania kodu natywnego w AAB (`ndk { debugSymbolLevel = "FULL" }`) — czytelne
  raporty crashy/ANR natywnych.

## 11. ⚠️ Konto organizacji — kluczowe dla aplikacji zdrowotnych (wnioski z procesu)

Aplikacje **zdrowotne/medyczne** (kategoria Medical + deklaracja Health „Medication and treatment
management") mogą być publikowane **wyłącznie z konta organizacji** — konto **osobiste** jest
odrzucane („Violation of Play Console Requirements", polityka od 31.08.2024). Tak było tutaj:

- Z konta **osobistego**: testy wewnętrzne działały, ale produkcja/tor zamknięty → **odrzucenie**.
- Rozwiązanie: **konwersja konta na organizację** (Developer account → zmiana typu konta). Wymaga:
  - numeru **D‑U‑N‑S** (darmowy z Dun & Bradstreet, bywa do 30 dni — tu nadany od ręki: 436779442),
  - zweryfikowanej **strony firmowej** (np. `tomaszkwietniewski.pl`),
  - **dokumentu rejestrowego**: dla jednoosobowej działalności → **zaświadczenie REGON**
    (z [wyszukiwarkaregon.stat.gov.pl](https://wyszukiwarkaregon.stat.gov.pl/)); **KRS** jest dla spółek,
  - utworzenia **nowego profilu płatności** typu organizacja (D‑U‑N‑S/typ konta nie da się zmienić
    na istniejącym profilu). Konta NIE zakłada się od nowa, $25 nie płaci ponownie.
- Po konwersji **dane kontaktowe organizacji (nazwa, adres, telefon, e‑mail) są PUBLICZNE** na
  profilu dewelopera (wymóg UE) — przy koncie organizacji nie da się tego ukryć.
- ✅ Konta organizacji są **zwolnione z wymogu 12 testerów × 14 dni** (ten dotyczy tylko kont
  osobistych) → produkcja dostępna od razu, bez zamkniętego testu.
- ⏳ **Synchronizacja statusu organizacji w systemach egzekwujących trwa do 72 h** (potwierdził
  support Play). Wysłanie ponownie PRZED upływem 72 h → kolejne automatyczne odrzucenia (to nie błąd!).
  Po 72 h i wysłaniu z **podbitym versionCode** → recenzja przeszła, aplikacja opublikowana publicznie.
