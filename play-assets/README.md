# Materiały graficzne do Google Play

Grafiki do listingu w Sklepie Play (wygenerowane spójnie z ikoną aplikacji).

| Plik | Wymiary | Przeznaczenie | Status |
|---|---|---|---|
| `icon-512.png` | 512×512 | Ikona aplikacji (Play zaokrągla rogi automatycznie) | ✅ gotowe |
| `feature-1024x500.png` | 1024×500 | Grafika promocyjna (feature graphic) | ✅ gotowe |
| `screenshots/01-ekran-glowny.png` | 1080×1920 | Zrzut: ekran główny (dawka + podpowiedź miejsca + kalendarz) | ✅ gotowe |
| `screenshots/02-okno-dnia.png` | 1080×1920 | Zrzut: okno dnia (status, miejsce wkłucia, dawka — zapis „Zapisz”) | ✅ gotowe |
| `screenshots/03-eksport-podsumowanie.png` | 1080×1920 | Zrzut: eksport do Excela + podsumowanie schematu | ✅ gotowe |
| `screenshots/04-ustawienia.png` | 1080×1920 | Zrzut: ustawienia (lek, ampułka, auto‑liczenie dawki) | ✅ gotowe |
| `screenshots/05-jezyk-kopie.png` | 1080×1920 | Zrzut: język + kopie zapasowe | ✅ gotowe |

## Zrzuty ekranu

Gotowe 5 zrzutów (1080×1920, 9:16) w katalogu [`screenshots/`](screenshots/) — z podpisem
i w oprawie telefonu, w spójnej kolorystyce marki. Odtworzone wiernie z UI aplikacji
(te same kolory motywu, teksty z `values-pl`, układ Material 3).

Generator: [`screenshots/gen.py`](screenshots/gen.py) tworzy pliki HTML w `screenshots/build/`
(nazwa pliku = nazwa zrzutu), które renderujemy do PNG przez Chrome headless (Edge potrafi
„dołączyć” do działającej instancji i NIC nie zapisać mimo exit 0 — używaj Chrome albo Edge
z osobnym `--user-data-dir`). W `file://` podawaj ścieżkę w stylu Windows (`C:/...`), nie MSYS (`/c/...`):
```
py screenshots/gen.py
chrome --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 \
  --window-size=1080,1920 --user-data-dir="$(mktemp -d)" \
  --screenshot=01-ekran-glowny.png "file:///C:/.../build/01-ekran-glowny.html"
```

Wymogi Play (spełnione): PNG/JPEG, min. 320 px, maks. 3840 px, proporcje 16:9 lub 9:16.

> Opcjonalnie później możesz podmienić je na „żywe" zrzuty z telefonu
> (`adb shell screencap -p /sdcard/s.png && adb pull /sdcard/s.png .`).
