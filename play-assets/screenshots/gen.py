# -*- coding: utf-8 -*-
"""Generator wiernych makiet ekranów aplikacji -> HTML 1080x1920 (pod Sklep Play).
Odtwarza UI z kodu Compose: kolory motywu, teksty z values-pl, układ Material 3.
"""
import datetime, calendar, os, locale

OUT = os.path.dirname(os.path.abspath(__file__))
BUILD = os.path.join(OUT, "build")
os.makedirs(BUILD, exist_ok=True)

# --- Kolory z Theme.kt / Calendar.kt ---
PRIMARY = "#2E6B5E"
PRIMARY_DARK = "#234f45"
PRIMARY_CONTAINER = "#B8E9DA"   # M3 primaryContainer dla seed #2E6B5E (jasny teal)
ON_PRIMARY_CONTAINER = "#00201a"
SURFACE = "#FBFDFA"
SURFACE_VARIANT = "#DBE5E0"
ON_SURFACE = "#191C1B"
ON_SURFACE_VAR = "#3F4946"
OUTLINE = "#6F7976"
CARD = "#FFFFFF"
GIVEN = "#2E7D32"
MISSED = "#C62828"
TODAY = "#F9A825"
UPCOMING = "#4A4A4A"
UPCOMING_TXT = "#D6D6D6"

FONT = "'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif"

# --- Dane przykładowe (spójne: ampułka 6 mg, 0,7 mg/dzień, 7 dni -> ost. 1,8 mg dzień 7) ---
CHILD = "Antek"  # neutralne imię demonstracyjne (publiczny listing Play)
MED = "Hormon wzrostu"
TODAY_DATE = datetime.date(2026, 6, 12)
MISSED_DAYS = {5}

def cal_grid():
    """Buduje siatkę czerwca 2026 z poniedziałkiem jako pierwszym dniem + statusy."""
    y, m = TODAY_DATE.year, TODAY_DATE.month
    first = datetime.date(y, m, 1)
    lead = (first.weekday())  # poniedziałek=0
    ndays = calendar.monthrange(y, m)[1]
    cells = [None] * lead + list(range(1, ndays + 1))
    while len(cells) % 7 != 0:
        cells.append(None)
    given = missed = 0
    html_rows = []
    for wk in range(0, len(cells), 7):
        tds = []
        for d in cells[wk:wk + 7]:
            if d is None:
                tds.append('<div class="cell empty"></div>')
                continue
            if d == TODAY_DATE.day:
                bg, fg, bold = TODAY, "#1A1A1A", "700"
            elif d < TODAY_DATE.day and d in MISSED_DAYS:
                bg, fg, bold = MISSED, "#fff", "400"; missed += 1
            elif d < TODAY_DATE.day:
                bg, fg, bold = GIVEN, "#fff", "400"; given += 1
            else:
                bg, fg, bold = UPCOMING, UPCOMING_TXT, "400"
            tds.append(
                f'<div class="cell" style="background:{bg};color:{fg};font-weight:{bold}">{d}</div>')
        html_rows.append('<div class="week">' + "".join(tds) + "</div>")
    return "\n".join(html_rows), given, missed

CAL_HTML, GIVEN_N, MISSED_N = cal_grid()

# --- Komponenty UI (px dobrane pod ekran telefonu 720px szer.) ---

def status_bar():
    return f'''<div class="statusbar">
      <span class="clock">8:00</span>
      <span class="sysicons">
        <svg width="20" height="14" viewBox="0 0 20 14"><path d="M1 9 h3 v4 h-3z M6 6 h3 v7 h-3z M11 3 h3 v10 h-3z M16 0 h3 v13 h-3z" fill="#191C1B"/></svg>
        <svg width="18" height="14" viewBox="0 0 18 14"><path d="M9 13 L0 4 A12 12 0 0 1 18 4 Z" fill="#191C1B"/></svg>
        <svg width="26" height="14" viewBox="0 0 26 14"><rect x="0.5" y="2" width="22" height="10" rx="2.5" fill="none" stroke="#191C1B"/><rect x="2" y="3.5" width="15" height="7" rx="1" fill="#191C1B"/><rect x="23" y="5" width="2.5" height="4" rx="1" fill="#191C1B"/></svg>
      </span>
    </div>'''

def topbar():
    return f'''<div class="topbar">
      <div class="logo">
        <svg width="26" height="26" viewBox="0 0 24 24">
          <path d="M12,2 C12,2 5.5,9.5 5.5,14.5 C5.5,18.09 8.41,21 12,21 C15.59,21 18.5,18.09 18.5,14.5 C18.5,9.5 12,2 12,2 Z" fill="#fff"/>
          <path d="M11,11 h2 v3 h3 v2 h-3 v3 h-2 v-3 h-3 v-2 h3 z" fill="{PRIMARY}"/>
        </svg>
      </div>
      <div class="title">Hormon Wzrostu Dawkowanie</div>
      <div class="gear">
        <svg width="26" height="26" viewBox="0 0 24 24" fill="{ON_SURFACE_VAR}"><path d="M19.14 12.94a7.49 7.49 0 0 0 0-1.88l2.03-1.58a.5.5 0 0 0 .12-.64l-1.92-3.32a.5.5 0 0 0-.61-.22l-2.39.96a7.3 7.3 0 0 0-1.62-.94l-.36-2.54a.5.5 0 0 0-.5-.42h-3.84a.5.5 0 0 0-.5.42l-.36 2.54c-.59.24-1.13.56-1.62.94l-2.39-.96a.5.5 0 0 0-.61.22L2.27 8.84a.5.5 0 0 0 .12.64l2.03 1.58a7.49 7.49 0 0 0 0 1.88l-2.03 1.58a.5.5 0 0 0-.12.64l1.92 3.32c.14.24.43.34.69.22l2.39-.96c.49.38 1.03.7 1.62.94l.36 2.54c.04.24.25.42.5.42h3.84c.25 0 .46-.18.5-.42l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.26.12.55.02.69-.22l1.92-3.32a.5.5 0 0 0-.12-.64l-2.03-1.58zM12 15.5A3.5 3.5 0 1 1 12 8.5a3.5 3.5 0 0 1 0 7z"/></svg>
      </div>
    </div>'''

def today_card():
    return f'''<div class="card today">
      <div class="t-title">Dzisiejsza dawka</div>
      <div class="t-dose">0,7 mg</div>
      <div class="t-day">{CHILD} • dzień 3/7</div>
      <button class="btn-fill">Oznacz: podano</button>
    </div>'''

def calendar_card(compact=False):
    legend = f'''<div class="legend">
      <span><i style="background:{GIVEN}"></i>Podano</span>
      <span><i style="background:{MISSED}"></i>Pominięto</span>
      <span><i style="background:{TODAY}"></i>Dziś</span>
      <span><i style="background:{UPCOMING}"></i>Później</span>
    </div>'''
    return f'''<div class="card cal">
      <div class="cal-head">
        <span class="chev">‹</span>
        <span class="cal-month">czerwiec 2026</span>
        <span class="chev">›</span>
      </div>
      <div class="week dow">
        <div class="cell h">pon</div><div class="cell h">wt</div><div class="cell h">śr</div>
        <div class="cell h">czw</div><div class="cell h">pt</div><div class="cell h">sob</div><div class="cell h">niedz</div>
      </div>
      {CAL_HTML}
      <div class="counts">Podano: {GIVEN_N} • Pominięto: {MISSED_N}</div>
      {legend}
    </div>'''

def summary_card():
    return f'''<div class="card">
      <div class="s-med">{MED}</div>
      <div class="s-row"><div class="s-l">Przypomnienie codziennie o</div><div class="s-v">08:00</div></div>
      <div class="s-row"><div class="s-l">Schemat</div><div class="s-v">0,7 mg × 6 dni + 1,8 mg</div></div>
      <div class="s-row"><div class="s-l">Ampułka</div><div class="s-v">6 mg na 7 dni</div></div>
      <div class="s-row"><div class="s-l">Powiadomienia</div><div class="s-v">włączone</div></div>
    </div>'''

def field(label, value, hint=False):
    return f'''<div class="tf {'tf-hint' if hint else ''}">
      <span class="tf-lab">{label}</span>
      <span class="tf-val">{value}</span>
    </div>'''

def settings_top():
    return f'''<div class="topbar settings">
      <div class="cancel">Anuluj</div>
      <div class="title-s">Ustawienia dawkowania</div>
    </div>'''

def day_dialog():
    return f'''<div class="scrim">
      <div class="dialog">
        <div class="dlg-x">✕</div>
        <div class="dlg-title">9 czerwca 2026</div>
        <div class="dlg-info">Dzień 7/7 • 1,8 mg</div>
        <div class="dlg-cur">Teraz: Podano</div>
        <div class="otf">
          <div class="otf-lab">Komentarz (opcjonalnie)</div>
          <div class="otf-val">Ostatnia z ampułki — jutro nowa.</div>
        </div>
        <div class="dlg-btns">
          <button class="btn-given">Podano</button>
          <button class="btn-missed">Pominięto</button>
        </div>
        <button class="btn-text">Zapisz</button>
      </div>
    </div>'''

# --- Powłoka marketingowa 1080x1920 ---
def shell(caption, sub, inner, screen_extra="", scrim=False):
    return f'''<!doctype html><html lang="pl"><head><meta charset="utf-8">
<style>
* {{ margin:0; padding:0; box-sizing:border-box; }}
html,body {{ width:1080px; height:1920px; font-family:{FONT}; }}
.bg {{ width:1080px; height:1920px;
  background:linear-gradient(160deg,{PRIMARY} 0%,{PRIMARY_DARK} 100%);
  display:flex; flex-direction:column; align-items:center; }}
.caption {{ color:#fff; text-align:center; padding:70px 70px 0; }}
.caption h1 {{ font-size:58px; font-weight:800; line-height:1.12; letter-spacing:-0.5px; }}
.caption p {{ font-size:30px; font-weight:400; opacity:.85; margin-top:18px; }}
.phone {{ position:relative; margin-top:54px; width:760px; height:1500px;
  background:#0c0f0e; border-radius:62px; padding:20px;
  box-shadow:0 30px 80px rgba(0,0,0,.35); }}
.screen {{ position:relative; width:720px; height:1460px; background:{SURFACE};
  border-radius:44px; overflow:hidden; }}
.statusbar {{ height:44px; display:flex; align-items:center; justify-content:space-between;
  padding:0 28px; color:{ON_SURFACE}; font-size:22px; font-weight:600; }}
.sysicons {{ display:flex; gap:8px; align-items:center; }}
.topbar {{ height:88px; display:flex; align-items:center; gap:16px; padding:0 18px;
  background:{SURFACE}; }}
.logo {{ width:50px; height:50px; border-radius:14px; background:{PRIMARY};
  display:flex; align-items:center; justify-content:center; flex:0 0 auto; }}
.title {{ font-size:30px; font-weight:600; color:{ON_SURFACE}; white-space:nowrap;
  overflow:hidden; text-overflow:ellipsis; }}
.gear {{ margin-left:auto; }}
.topbar.settings {{ gap:20px; }}
.cancel {{ color:{PRIMARY}; font-size:26px; font-weight:600; }}
.title-s {{ font-size:30px; font-weight:600; color:{ON_SURFACE}; }}
.body {{ padding:24px; display:flex; flex-direction:column; gap:24px; }}
.card {{ background:{CARD}; border-radius:26px; padding:30px; box-shadow:0 1px 3px rgba(0,0,0,.10); }}
.today {{ background:{PRIMARY_CONTAINER}; text-align:center; display:flex;
  flex-direction:column; align-items:center; gap:12px; padding:38px 30px; }}
.t-title {{ font-size:28px; font-weight:500; color:{ON_PRIMARY_CONTAINER}; }}
.t-dose {{ font-size:84px; font-weight:800; color:{ON_PRIMARY_CONTAINER}; line-height:1; }}
.t-day {{ font-size:28px; color:{ON_PRIMARY_CONTAINER}; }}
.btn-fill {{ margin-top:14px; width:100%; border:none; background:{PRIMARY}; color:#fff;
  font-size:28px; font-weight:600; padding:22px; border-radius:100px; font-family:{FONT}; }}
.cal-head {{ display:flex; align-items:center; justify-content:space-between; margin-bottom:18px; }}
.cal-month {{ font-size:34px; font-weight:600; color:{ON_SURFACE}; text-transform:none; }}
.chev {{ font-size:46px; font-weight:700; color:{PRIMARY}; padding:0 14px; }}
.week {{ display:flex; gap:8px; margin-bottom:8px; }}
.dow {{ margin-bottom:10px; }}
.cell {{ flex:1; aspect-ratio:1/1; border-radius:13px; display:flex; align-items:center;
  justify-content:center; font-size:26px; color:{ON_SURFACE}; }}
.cell.h {{ aspect-ratio:auto; height:34px; font-size:22px; color:{ON_SURFACE_VAR};
  background:none; font-weight:500; }}
.cell.empty {{ background:none; }}
.counts {{ font-size:30px; font-weight:600; color:{ON_SURFACE}; margin-top:18px; }}
.legend {{ display:flex; gap:22px; margin-top:16px; }}
.legend span {{ display:flex; align-items:center; gap:8px; font-size:22px; color:{ON_SURFACE_VAR}; }}
.legend i {{ width:18px; height:18px; border-radius:50%; display:inline-block; }}
.s-med {{ font-size:30px; font-weight:600; color:{ON_SURFACE}; margin-bottom:14px; }}
.s-row {{ margin-bottom:14px; }}
.s-l {{ font-size:22px; color:{ON_SURFACE_VAR}; }}
.s-v {{ font-size:28px; color:{ON_SURFACE}; }}
.tf {{ border:2px solid {OUTLINE}; border-radius:14px; padding:16px 20px; position:relative; }}
.tf-lab {{ position:absolute; top:-14px; left:16px; background:{SURFACE}; padding:0 8px;
  font-size:20px; color:{PRIMARY}; }}
.tf-val {{ font-size:28px; color:{ON_SURFACE}; }}
.calc {{ background:{CARD}; border-radius:20px; padding:24px; box-shadow:0 1px 3px rgba(0,0,0,.10); }}
.calc-t {{ font-size:23px; font-weight:600; color:{ON_SURFACE}; margin-bottom:6px; }}
.calc-v {{ font-size:28px; color:{ON_SURFACE}; }}
.obtn {{ border:2px solid {OUTLINE}; border-radius:100px; padding:20px; text-align:center;
  font-size:26px; color:{PRIMARY}; font-weight:600; }}
.switchrow {{ display:flex; align-items:center; justify-content:space-between; padding:6px 4px; }}
.switchrow span {{ font-size:28px; color:{ON_SURFACE}; }}
.switch {{ width:74px; height:42px; border-radius:100px; background:{PRIMARY}; position:relative; }}
.switch::after {{ content:''; position:absolute; right:5px; top:5px; width:32px; height:32px;
  border-radius:50%; background:#fff; }}
.chips {{ display:flex; gap:14px; }}
.chip {{ border:2px solid {OUTLINE}; border-radius:12px; padding:14px 22px; font-size:24px; color:{ON_SURFACE}; }}
.chip.sel {{ background:{PRIMARY_CONTAINER}; border-color:{PRIMARY_CONTAINER}; color:{ON_PRIMARY_CONTAINER}; }}
.lab {{ font-size:23px; font-weight:600; color:{ON_SURFACE}; }}
.hinttxt {{ font-size:21px; color:{ON_SURFACE_VAR}; }}
/* dialog */
.scrim {{ position:absolute; inset:0; background:rgba(0,0,0,.32);
  display:flex; align-items:center; justify-content:center; padding:30px; }}
.dialog {{ width:100%; background:{CARD}; border-radius:34px; padding:36px; position:relative;
  display:flex; flex-direction:column; gap:18px; box-shadow:0 20px 60px rgba(0,0,0,.4); }}
.dlg-x {{ position:absolute; top:28px; right:32px; font-size:30px; color:{ON_SURFACE_VAR}; }}
.dlg-title {{ font-size:36px; font-weight:600; color:{ON_SURFACE}; padding-right:50px; }}
.dlg-info {{ font-size:27px; color:{ON_SURFACE}; }}
.dlg-cur {{ font-size:27px; color:{ON_SURFACE}; }}
.otf {{ border:2px solid {OUTLINE}; border-radius:14px; padding:20px; position:relative; min-height:110px; }}
.otf-lab {{ position:absolute; top:-14px; left:16px; background:{CARD}; padding:0 8px;
  font-size:20px; color:{PRIMARY}; }}
.otf-val {{ font-size:26px; color:{ON_SURFACE}; }}
.dlg-btns {{ display:flex; gap:18px; }}
.btn-given {{ flex:1; border:none; background:{GIVEN}; color:#fff; font-size:28px; font-weight:600;
  padding:22px; border-radius:100px; font-family:{FONT}; }}
.btn-missed {{ flex:1; border:none; background:{MISSED}; color:#fff; font-size:28px; font-weight:600;
  padding:22px; border-radius:100px; font-family:{FONT}; }}
.btn-text {{ background:none; border:none; color:{PRIMARY}; font-size:28px; font-weight:600;
  padding:14px; font-family:{FONT}; }}
</style></head><body>
<div class="bg">
  <div class="caption"><h1>{caption}</h1><p>{sub}</p></div>
  <div class="phone"><div class="screen">
    {status_bar()}
    {inner}
  </div></div>
</div>
</body></html>'''

# --- Złożenie 4 ekranów ---
screens = {
  "01-dashboard": shell(
      "Kalendarz i dawka<br>na ekranie głównym",
      "Dzisiejsza dawka, historia podań i szybkie „Oznacz: podano”",
      topbar() + f'<div class="body">{today_card()}{calendar_card()}</div>'),

  "02-dzien": shell(
      "Oznacz podanie<br>i dodaj komentarz",
      "Podano / pominięto — także wstecz, z notatką do dnia",
      topbar() + f'<div class="body">{calendar_card()}</div>' + day_dialog()),

  "03-ustawienia": shell(
      "Auto-liczenie<br>ostatniej dawki",
      "Wpisz ampułkę, dawkę i dni — resztę policzy aplikacja",
      settings_top() + f'''<div class="body">
        {field("Imię dziecka", CHILD)}
        {field("Pojemność ampułki (mg)", "6")}
        {field("Dawka dzienna (mg)", "0,7")}
        {field("Liczba dni cyklu (na 1 ampułkę)", "7")}
        <div class="calc"><div class="calc-t">Wyliczona ostatnia dawka</div>
          <div class="calc-v">0,7 mg × 6 dni + 1,8 mg (dzień 7)</div></div>
        <div class="obtn">Godzina przypomnienia: 08:00</div>
      </div>'''),

  "04-jezyk-kopie": shell(
      "Polski / English,<br>kopie i eksport",
      "Dane tylko lokalnie • eksport do Excela • kopia zapasowa",
      settings_top() + f'''<div class="body">
        <div class="switchrow"><span>Powiadomienia włączone</span><div class="switch"></div></div>
        <div style="display:flex;flex-direction:column;gap:14px">
          <div class="lab">Język</div>
          <div class="chips"><div class="chip">Domyślny systemu</div><div class="chip sel">Polski</div><div class="chip">English</div></div>
        </div>
        <div style="display:flex;flex-direction:column;gap:12px">
          <div class="lab">Kopia zapasowa</div>
          <div class="hinttxt">Zapisz plik kopii (np. na Google Drive). Na nowym telefonie zaimportuj go tutaj, aby przywrócić wszystko.</div>
          <div class="obtn">Eksportuj kopię zapasową</div>
          <div class="obtn">Importuj kopię zapasową</div>
        </div>
        <button class="btn-fill">Zapisz</button>
      </div>'''),
}

for name, html in screens.items():
    p = os.path.join(BUILD, name + ".html")
    with open(p, "w", encoding="utf-8") as f:
        f.write(html)
    print("wrote", p)
print("OK")
