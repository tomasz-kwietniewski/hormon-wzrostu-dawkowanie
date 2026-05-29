# Hormon Wzrostu Dawkowanie

Prosta aplikacja na Androida, która **codziennie o ustalonej godzinie** przypomina o dawce
hormonu wzrostu (lub innego leku z ampułki dzielonej na cykl). Powstała na własne potrzeby
rodzica dziecka w programie lekowym i jest udostępniana innym rodzicom.

> [!IMPORTANT]
> Aplikacja **tylko przypomina** o dawce wpisanej przez lekarza. **Nie zastępuje** ulotki leku
> ani zaleceń lekarza prowadzącego i nie udziela porad medycznych. Dawkowanie zawsze ustala lekarz.

## Co potrafi

- Jedno powiadomienie dziennie o wybranej godzinie (domyślnie 19:00).
- Schemat „dawka dzienna × N dni + ostatni dzień = reszta z ampułki" — **ostatnia dawka liczy się
  automatycznie**.
- Pokazuje numer dnia w cyklu (np. „dzień 5/12") i ostrzega, gdy jutro trzeba otworzyć nową ampułkę.
- Wszystko edytowalne w ustawieniach: imię dziecka, nazwa leku, pojemność ampułki, dawka dzienna,
  liczba dni cyklu, **dzień startu cyklu**, godzina przypomnienia.
- Przypomnienia przeżywają restart telefonu.
- Dane trzymane lokalnie na urządzeniu (brak konta, brak chmury, brak internetu).

### Przykładowe schematy (suma zawsze = 10 mg z ampułki Omnitrope)

| Dawka dzienna | Dni standardowe | Ostatni dzień | Dni / ampułkę |
|---|---|---|---|
| 0,6 mg | 15 | 1,0 mg | 16 |
| 0,7 mg | 13 | 0,9 mg | 14 |
| 0,8 mg | 11 | 1,2 mg | 12 |

W aplikacji wpisujesz tylko: pojemność (10 mg), dawkę dzienną i liczbę dni — resztę policzy sama.

## Wymagania techniczne

- Android 8.0 (API 26) lub nowszy.
- Kotlin + Jetpack Compose (Material 3), `AlarmManager` do dokładnych alarmów.

## Pobranie gotowego APK

Każdy build w **GitHub Actions** (zakładka *Actions* → ostatni przebieg *Build APK*) zawiera artefakt
`HormonWzrostuDawkowanie-debug` z plikiem `app-debug.apk`.

## Instalacja

### Telefon bez ograniczeń — zwykły sideload
Skopiuj `app-debug.apk` na telefon i otwórz; zezwól na instalację z tego źródła.

### Telefon z Google Advanced Protection — przez ADB (kabel USB)
Advanced Protection blokuje zwykłą instalację APK, ale **instalacja przez ADB działa** i nie wymaga
wyłączania ochrony:

1. Na telefonie: *Ustawienia → Informacje → numer kompilacji* (7×) → włącz **Opcje programisty**.
2. W Opcjach programisty włącz **Debugowanie USB**.
3. Na komputerze zainstaluj **Android Platform Tools** (zawiera `adb`).
4. Podłącz telefon kablem, potwierdź zaufanie do komputera, i uruchom:
   ```
   adb install app-debug.apk
   ```

### Dla wielu rodziców — Google Play (testy wewnętrzne)
Docelowo aplikację można opublikować na ścieżce *Internal testing* w Google Play (wymaga konta
Google Play Developer, jednorazowo 25 USD). Instalacja idzie wtedy ze Sklepu Play, więc działa też
na telefonach z Advanced Protection, bez ADB.

## Budowanie ze źródeł

Projekt korzysta z Android Gradle Plugin 8.7, Kotlin 2.0, Gradle 8.9.

- **Android Studio:** otwórz katalog projektu — IDE pobierze SDK i wygeneruje wrapper Gradle.
- **Wiersz poleceń** (z zainstalowanym Gradle 8.9 i Android SDK):
  ```
  gradle assembleDebug
  ```
  APK pojawi się w `app/build/outputs/apk/debug/app-debug.apk`.

## Po pierwszym uruchomieniu

1. Zezwól na powiadomienia (Android 13+ zapyta od razu).
2. Wejdź w **Ustawienia** i uzupełnij schemat oraz dzień startu cyklu.
3. Jeśli system zapyta o **alarmy dokładne**, zezwól — dzięki temu powiadomienie przyjdzie punktualnie.

## Plany na przyszłość

- Wiele leków/suplementów jednocześnie (D3, K2, omega-3) z osobnymi porami.
- Tryb ręcznej listy faz (dla nietypowych schematów).
- Publikacja na Google Play.

## Licencja

MIT — zobacz [LICENSE](LICENSE).
