# Hormon Wzrostu Dawkowanie

Aplikacja na Androida, która **codziennie o ustalonej porze przypomina o dawce** hormonu
wzrostu (lub innego leku z ampułki dzielonej na cykl), pozwala **odznaczać podane dawki**,
prowadzi **historię w formie kalendarza**, eksportuje dane do **Excela** i robi **kopie zapasowe**.
Powstała na własne potrzeby rodzica dziecka w programie lekowym i jest udostępniana innym rodzicom.

> [!IMPORTANT]
> Aplikacja **tylko przypomina** o dawce wpisanej przez rodzica. **Nie zastępuje** ulotki leku ani
> zaleceń lekarza prowadzącego i nie udziela porad medycznych. Dawkowanie zawsze ustala lekarz.

🇬🇧 *English summary at the bottom.*

## Funkcje

- ⏰ **Codzienne powiadomienie** o wybranej godzinie (alarm typu „budzik", odporny na tryb Doze).
- 🧮 **Auto-liczenie cyklu** — podajesz tylko pojemność ampułki i dawkę dzienną, a aplikacja sama
  wylicza **długość cyklu** oraz „resztę" na ostatni dzień (i powtarza cykl). Ostatnia dawka mieści
  się zawsze w przedziale ⟨dawka, 2 × dawka) — gdy w ampułce zostaje mniej niż dwie pełne dawki.
- ✅ **Śledzenie podań** — przycisk „Podano" na dziś oraz edycja dowolnego dnia w kalendarzu
  (podano / nie podano), także wstecz.
- 📝 **Komentarz do dnia** — opcjonalne pole (np. powód pominięcia, okoliczności podania).
- 📅 **Historia (kalendarz) na ekranie głównym** — miesiąc z kolorami: podano (zielony),
  pominięto (czerwony), dziś (bursztyn), później (szary), plus licznik podano/pominięto;
  tapnięcie dnia otwiera okno edycji (status + komentarz).
- 📊 **Eksport do Excela (.xlsx)** — nazwy kolumn i statusy w języku aplikacji, kolumna komentarza,
  poprawne polskie znaki.
- 💾 **Kopia zapasowa** — ręczny eksport/import całej historii (JSON), np. na Google Drive;
  plus automatyczny backup Androida na konto Google.
- 🌍 **Języki PL/EN** — automatycznie wg języka telefonu, z możliwością ręcznego przełączenia.
  Sama nazwa aplikacji (etykieta skrótu na pulpicie) jest zawsze po polsku — to nazwa marki.
- 🔒 **Prywatność** — dane wyłącznie lokalnie na urządzeniu, brak konta, brak sieci.

### Przykładowe schematy (suma zawsze = pojemność ampułki, np. 10 mg)

| Dawka dzienna | Dni standardowe | Ostatni dzień | Dni / ampułkę |
|---|---|---|---|
| 0,6 mg | 15 | 1,0 mg | 16 |
| 0,7 mg | 13 | 0,9 mg | 14 |
| 0,8 mg | 11 | 1,2 mg | 12 |

W aplikacji wpisujesz tylko pojemność (10 mg) i dawkę dzienną — długość cyklu i ostatnią dawkę policzy sama.

## Wymagania

- Android 8.0 (API 26) lub nowszy.
- Kotlin + Jetpack Compose (Material 3), `AlarmManager` (dokładny alarm budzika).

## Pobranie gotowego APK

Każdy build w **GitHub Actions** (zakładka *Actions* → *Build APK*) zawiera artefakt
`HormonWzrostuDawkowanie-debug` z plikiem `app-debug.apk`.

## Instalacja

### Telefon bez ograniczeń — zwykły sideload
Skopiuj `app-debug.apk` na telefon i otwórz; zezwól na instalację z tego źródła.

### Telefon z Google Advanced Protection — przez ADB (kabel USB)
Advanced Protection blokuje zwykłą instalację APK, ale instalacja przez ADB działa bez wyłączania ochrony:

1. *Ustawienia → Informacje → numer kompilacji* (7×) → włącz **Opcje programisty**.
2. W Opcjach programisty włącz **Debugowanie USB**.
3. Na komputerze zainstaluj **Android Platform Tools** (zawiera `adb`).
4. Podłącz telefon, potwierdź zaufanie i uruchom:
   ```
   adb install -r app-debug.apk
   ```

### Po pierwszym uruchomieniu
1. Zezwól na **powiadomienia** (Android 13+ zapyta od razu).
2. Wejdź w **Ustawienia** i uzupełnij schemat oraz dzień startu cyklu.
3. Na ekranie głównym możesz włączyć **„Bez ograniczeń baterii"** dla maksymalnej niezawodności.

## Niezawodność powiadomień

Przypomnienia używają `setAlarmClock` (priorytet jak budzik, zwolniony z Doze). Na telefonach
agresywnie usypiających aplikacje (OnePlus, Samsung, Xiaomi, Huawei) warto dodatkowo ustawić
baterię aplikacji na **„Bez ograniczeń"** — aplikacja proponuje to jednym tapnięciem.

## Kopie zapasowe i przenosiny na nowy telefon

- **Ręcznie (zalecane):** Ustawienia → *Kopia zapasowa* → **Eksportuj kopię zapasową** (zapisz plik
  np. na Google Drive). Na nowym telefonie: **Importuj kopię zapasową** i wskaż plik.
- **Automatycznie:** Android kopiuje dane aplikacji na konto Google użytkownika (Auto Backup);
  przywracane przy instalacji na nowym urządzeniu (najpewniej ze Sklepu Play).

## Budowanie ze źródeł

Android Gradle Plugin 8.7, Kotlin 2.0, Gradle 8.9, JDK 17, compileSdk 35, minSdk 26.

- **Android Studio:** otwórz katalog projektu — IDE pobierze SDK i wygeneruje wrapper Gradle.
- **Wiersz poleceń** (z Gradle 8.9 i Android SDK):
  ```
  gradle assembleDebug          # APK do testów (debug)
  gradle bundleRelease          # AAB do Sklepu Play (wymaga klucza, patrz niżej)
  ```

### Podpisywanie (release)
Build release/AAB jest podpisywany kluczem dostarczanym przez zmienne środowiskowe
(`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). W CI pochodzą one z sekretów
repozytorium; lokalnie bez nich build debug używa domyślnego klucza debug.

## Publikacja w Google Play

Plan i lista kontrolna: [docs/PLAY_STORE.md](docs/PLAY_STORE.md). Polityka prywatności:
[PRIVACY.md](PRIVACY.md).

## Licencja

MIT — zobacz [LICENSE](LICENSE).

---

## English summary

**Growth Hormone Dosing** is an Android app that sends a **daily reminder** about a child's growth
hormone dose (or any medicine split from one ampoule over a cycle), lets you **mark doses as given**,
keeps a **calendar history** with optional per-day **comments**, exports to **Excel (.xlsx)**, and
supports **manual backup/restore** plus Android Auto Backup. UI in **English/Polish** (follows the
phone language, switchable in Settings). **Data stays on the device** — no account, no network.

> This tool only reminds about a dose entered by a parent. It does not replace the medicine leaflet
> or a doctor's instructions and is not medical advice.

Build: `gradle assembleDebug` (APK) or `gradle bundleRelease` (Play AAB). License: MIT.
