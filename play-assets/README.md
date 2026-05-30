# Materiały graficzne do Google Play

Grafiki do listingu w Sklepie Play (wygenerowane spójnie z ikoną aplikacji).

| Plik | Wymiary | Przeznaczenie | Status |
|---|---|---|---|
| `icon-512.png` | 512×512 | Ikona aplikacji (Play zaokrągla rogi automatycznie) | ✅ gotowe |
| `feature-1024x500.png` | 1024×500 | Grafika promocyjna (feature graphic) | ✅ gotowe |
| zrzuty ekranu | min. 320 px, proporcje telefonu | min. 2 (zalecane 4–8) | ⏳ do zrobienia |

## Zrzuty ekranu — do wykonania z telefonu

Zalecane ujęcia (z realnej aplikacji):
1. Ekran główny z dzisiejszą dawką i przyciskiem „Oznacz: podano".
2. Historia‑kalendarz z kolorami i licznikiem.
3. Okno edycji dnia (data, dawka, komentarz, przyciski Podano/Pominięto).
4. Ustawienia (schemat dawkowania + język).

Jak zrobić: na telefonie zrób zrzuty ekranu w aplikacji, albo przez ADB:
```
adb shell screencap -p /sdcard/s.png && adb pull /sdcard/s.png .
```

Wymogi Play: PNG lub JPEG, min. wymiar 320 px, maks. 3840 px, proporcje 16:9 lub 9:16.
