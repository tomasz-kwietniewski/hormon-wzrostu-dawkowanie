# Cykl wg realnych mg + awaryjna edycja dawki — Plan implementacji

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cykl ma się nie gubić przy pominięciu dnia (liczony wg liczby faktycznych podań) i ma śledzić realne zużycie mg z ampułki; dodatkowo awaryjna edycja faktycznie podanej dawki dla danego dnia.

**Architecture:** Zamiast liczyć dzień cyklu z kalendarza (`(dni od startu) % długość`), wprowadzamy jeden czysty przebieg po podanych dawkach w kolejności dat (`CycleTimeline`), który symuluje zużycie ampułki w tysięcznych mg. Dawka faktyczna jest opcjonalnym override'em w nowej mapie `doses` (brak wpisu = dawka wg planu). UI/eksport/powiadomienie czytają z tego przebiegu zamiast z kalendarza.

**Tech Stack:** Kotlin, Jetpack Compose, kotlinx.serialization, SharedPreferences. Testy: JUnit4 (lokalne testy JVM w `app/src/test`), uruchamiane przez Gradle (CI: gradle 8.9).

**Środowisko uruchomieniowe:** Repo nie ma skryptu `gradlew` (CI używa `gradle` 8.9 przez `gradle/actions/setup-gradle`). Lokalnie używaj zainstalowanego `gradle` albo Gradle z Android Studio. Instalacja debug APK przez `adb` (jak w poprzednich sesjach). Testy są też podpięte do CI (Task 9), więc realne wykonanie jest gwarantowane przy push.

---

## Struktura plików

**Nowe:**
- `app/src/main/java/pl/hormonwzrostu/data/CycleTimeline.kt` — rdzeń: `DoseEvent`, `NextDose`, `buildTimeline`, `nextDose`.
- `app/src/test/java/pl/hormonwzrostu/data/CycleTimelineTest.kt` — testy rdzenia.
- `app/src/test/java/pl/hormonwzrostu/data/BackupSerializationTest.kt` — round-trip kopii + zgodność wstecz.
- `app/src/test/java/pl/hormonwzrostu/data/IntakeRowsTest.kt` — testy `buildIntakeRows`.

**Modyfikowane:**
- `gradle/libs.versions.toml` — JUnit w katalogu wersji.
- `app/build.gradle.kts` — `testImplementation(junit)`, podbicie wersji.
- `app/src/main/java/pl/hormonwzrostu/data/Backup.kt` — pole `doses`.
- `app/src/main/java/pl/hormonwzrostu/data/ScheduleRepository.kt` — load/save `doses`, w backupie.
- `app/src/main/java/pl/hormonwzrostu/MainViewModel.kt` — stan `doses` + `setActualDose` / `actualDoseFor`.
- `app/src/main/java/pl/hormonwzrostu/data/Adherence.kt` — `IntakeRow` (nullable day/dose), `buildIntakeRows` na timeline.
- `app/src/main/java/pl/hormonwzrostu/util/XlsxExport.kt` — puste komórki dla nulli.
- `app/src/main/java/pl/hormonwzrostu/ui/MainScreen.kt` — `TodayDoseCard` na `nextDose`, przekazanie `doses`/`onSetActualDose`.
- `app/src/main/java/pl/hormonwzrostu/ui/Calendar.kt` — pole mg w `DayEditDialog`, numer dnia z timeline.
- `app/src/main/java/pl/hormonwzrostu/MainActivity.kt` — wiring `doses` + `onSetActualDose`.
- `app/src/main/java/pl/hormonwzrostu/notify/ReminderReceiver.kt` — `nextDose` zamiast `dayIndexInCycle`.
- `app/src/main/res/values/strings.xml` + `values-pl/strings.xml` — nowe etykiety.
- `.github/workflows/build.yml` — krok testów przed budową.

---

## Task 1: Infrastruktura testów (JUnit) + smoke test

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Test: `app/src/test/java/pl/hormonwzrostu/SmokeTest.kt`

- [ ] **Step 1: Dodaj JUnit do katalogu wersji**

W `gradle/libs.versions.toml` w sekcji `[versions]` dopisz po linii `serialization = "1.7.3"`:

```toml
junit = "4.13.2"
```

W sekcji `[libraries]` dopisz po linii `kotlinx-serialization-json = ...`:

```toml
junit = { group = "junit", name = "junit", version.ref = "junit" }
```

- [ ] **Step 2: Dodaj zależność testową**

W `app/build.gradle.kts`, w bloku `dependencies { ... }`, dopisz na końcu (po `debugImplementation(libs.androidx.ui.tooling)`):

```kotlin
    testImplementation(libs.junit)
```

- [ ] **Step 3: Napisz smoke test (ma potwierdzić, że runner działa)**

Utwórz `app/src/test/java/pl/hormonwzrostu/SmokeTest.kt`:

```kotlin
package pl.hormonwzrostu

import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test
    fun runnerWorks() {
        assertEquals(4, 2 + 2)
    }
}
```

- [ ] **Step 4: Uruchom testy — mają przejść**

Run: `gradle :app:testDebugUnitTest --no-daemon`
Expected: BUILD SUCCESSFUL, `SmokeTest > runnerWorks PASSED`.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/test/java/pl/hormonwzrostu/SmokeTest.kt
git commit -m "test: dodaj lokalne testy JVM (JUnit) + smoke test"
```

---

## Task 2: Rdzeń `CycleTimeline` (TDD)

**Files:**
- Create: `app/src/main/java/pl/hormonwzrostu/data/CycleTimeline.kt`
- Test: `app/src/test/java/pl/hormonwzrostu/data/CycleTimelineTest.kt`

- [ ] **Step 1: Napisz testy (mają się nie kompilować/nie przechodzić)**

Utwórz `app/src/test/java/pl/hormonwzrostu/data/CycleTimelineTest.kt`:

```kotlin
package pl.hormonwzrostu.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CycleTimelineTest {

    private fun sched(ampoule: Double = 10.0, daily: Double = 0.8, start: String = "2026-01-01") =
        Schedule(childName = "Dziecko", ampouleMg = ampoule, dailyDoseMg = daily, startDateIso = start)

    /** Zbiór n kolejnych dat od [start] (przebieg i tak nie patrzy na odstępy między datami). */
    private fun dates(start: String, n: Int): Set<String> {
        val s = LocalDate.parse(start)
        return (0 until n).map { s.plusDays(it.toLong()).toString() }.toSet()
    }

    @Test
    fun equalDivision_noRemainder_rollsOverAfter20() {
        val tl = buildTimeline(sched(10.0, 0.5), dates("2026-01-01", 21), emptyMap())
        assertEquals(21, tl.size)
        assertTrue(tl.take(20).all { it.cycleNumber == 1 })
        assertEquals(20, tl[19].dayInCycle)
        assertTrue(tl[19].isLastInCycle)
        assertEquals(2, tl[20].cycleNumber)
        assertEquals(1, tl[20].dayInCycle)
        tl.forEach { assertEquals(0.5, it.plannedMg, 1e-9) }
    }

    @Test
    fun standardRemainder_lastDayIsRest() {
        val tl = buildTimeline(sched(10.0, 0.8), dates("2026-01-01", 12), emptyMap())
        assertEquals(12, tl.size)
        (0..10).forEach { assertEquals(0.8, tl[it].plannedMg, 1e-9) }
        assertEquals(1.2, tl[11].plannedMg, 1e-9)
        assertEquals(12, tl[11].dayInCycle)
        assertTrue(tl[11].isLastInCycle)
    }

    @Test
    fun gapsDoNotAdvanceCycle() {
        val intake = setOf("2026-01-01", "2026-01-02", "2026-01-05", "2026-01-09")
        val tl = buildTimeline(sched(10.0, 0.8), intake, emptyMap())
        assertEquals(listOf(1, 2, 3, 4), tl.map { it.dayInCycle })
    }

    @Test
    fun nextDose_skippedDayStaysSameUntilGiven() {
        val intake = setOf("2026-01-01", "2026-01-02", "2026-01-05", "2026-01-09")
        val nd1 = nextDose(sched(10.0, 0.8), intake, emptyMap(), LocalDate.parse("2026-01-10"))!!
        assertEquals(5, nd1.dayInCycle)
        // Kolejny dzień bez podania — wciąż dzień 5 (rdzeń poprawki użytkownika).
        val nd2 = nextDose(sched(10.0, 0.8), intake, emptyMap(), LocalDate.parse("2026-01-11"))!!
        assertEquals(5, nd2.dayInCycle)
    }

    @Test
    fun correctionDown_addsLeftoverDay_sameAmpoule() {
        val doses = mapOf("2026-01-12" to 0.8) // dzień 12: podano 0,8 zamiast planowanych 1,2
        val tl12 = buildTimeline(sched(10.0, 0.8), dates("2026-01-01", 12), doses)
        assertEquals(0.8, tl12[11].actualMg, 1e-9)
        assertFalse(tl12[11].isLastInCycle) // zostaje 0,4 mg

        val tl13 = buildTimeline(sched(10.0, 0.8), dates("2026-01-01", 13), doses)
        assertEquals(13, tl13.size)
        assertEquals(1, tl13[12].cycleNumber) // wciąż ta sama ampułka
        assertEquals(13, tl13[12].dayInCycle)
        assertEquals(0.4, tl13[12].plannedMg, 1e-9) // resztka
        assertTrue(tl13[12].isLastInCycle)
    }

    @Test
    fun overdose_closesAmpoule_noNegative_nextIsNewCycle() {
        val doses = mapOf("2026-01-12" to 2.0) // więcej niż resztka 1,2
        val tl = buildTimeline(sched(10.0, 0.8), dates("2026-01-01", 13), doses)
        assertTrue(tl[11].isLastInCycle)
        assertEquals(2, tl[12].cycleNumber)
        assertEquals(1, tl[12].dayInCycle)
        assertTrue(tl.all { it.plannedMg >= 0.0 && it.actualMg >= 0.0 })
    }

    @Test
    fun emptyHistory_nextIsCycle1Day1() {
        val nd = nextDose(sched(10.0, 0.8), emptySet(), emptyMap(), LocalDate.parse("2026-01-01"))!!
        assertEquals(1, nd.cycleNumber)
        assertEquals(1, nd.dayInCycle)
        assertEquals(0.8, nd.plannedMg, 1e-9)
        assertFalse(nd.isLastInCycle)
        assertTrue(buildTimeline(sched(), emptySet(), emptyMap()).isEmpty())
    }

    @Test
    fun nextDose_beforeStart_isNull() {
        assertNull(nextDose(sched(start = "2026-01-10"), emptySet(), emptyMap(), LocalDate.parse("2026-01-05")))
    }

    @Test
    fun nextDose_noStartDate_isNull() {
        assertNull(nextDose(Schedule(), emptySet(), emptyMap(), LocalDate.parse("2026-01-01")))
    }
}
```

- [ ] **Step 2: Uruchom testy — mają się NIE skompilować**

Run: `gradle :app:testDebugUnitTest --no-daemon`
Expected: kompilacja FAIL — `unresolved reference: buildTimeline`, `nextDose`, `DoseEvent`, `NextDose`.

- [ ] **Step 3: Zaimplementuj rdzeń**

Utwórz `app/src/main/java/pl/hormonwzrostu/data/CycleTimeline.kt`:

```kotlin
package pl.hormonwzrostu.data

import java.time.LocalDate

/** Jedno faktyczne podanie dawki, z pozycją w cyklu wyliczoną z całej historii. */
data class DoseEvent(
    val date: LocalDate,
    val cycleNumber: Int,    // numer ampułki, 1-based
    val dayInCycle: Int,     // pozycja w ampułce, 1-based
    val plannedMg: Double,   // dawka wg planu dla tego slotu przy danym stanie ampułki
    val actualMg: Double,    // faktycznie podana (override lub = planned)
    val isLastInCycle: Boolean, // to podanie domknęło ampułkę
)

/** Stan następnej (jeszcze niepodanej) dawki — np. dzisiejszej. */
data class NextDose(
    val cycleNumber: Int,
    val dayInCycle: Int,
    val plannedMg: Double,
    val isLastInCycle: Boolean,
)

// Liczymy w tysięcznych mg na liczbach całkowitych, by uniknąć błędów zmiennoprzecinkowych.
private const val MG_SCALE = 1000.0

private fun toTh(mg: Double): Long = Math.round(mg * MG_SCALE)
private fun toMg(th: Long): Double = th / MG_SCALE

/** Wspólne dane wejściowe; null gdy schemat nie pozwala na przebieg. */
private data class Prep(val start: LocalDate, val ampouleTh: Long, val dailyTh: Long)

private fun prep(schedule: Schedule): Prep? {
    val start = schedule.startDate() ?: return null
    val ampouleTh = toTh(schedule.ampouleMg)
    val dailyTh = toTh(schedule.dailyDoseMg)
    if (ampouleTh <= 0L || dailyTh <= 0L || ampouleTh < dailyTh) return null
    return Prep(start, ampouleTh, dailyTh)
}

private fun sortedDates(intake: Set<String>, start: LocalDate): List<LocalDate> =
    intake.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        .filter { !it.isBefore(start) }
        .sorted()

private data class WalkState(val remainingTh: Long, val cycle: Int, val dayInCycle: Int)

/** Przechodzi po datach symulując zużycie ampułki; opcjonalnie zbiera zdarzenia. */
private fun walk(
    p: Prep,
    dates: List<LocalDate>,
    doses: Map<String, Double>,
    collect: ((DoseEvent) -> Unit)?,
): WalkState {
    var remaining = p.ampouleTh
    var cycle = 1
    var dayInCycle = 0
    for (date in dates) {
        if (remaining <= 0L) {
            cycle++; remaining = p.ampouleTh; dayInCycle = 0
        }
        dayInCycle++
        val plannedTh = if (remaining >= 2 * p.dailyTh) p.dailyTh else remaining
        val actualTh = doses[date.toString()]?.let { toTh(it) }?.takeIf { it > 0L } ?: plannedTh
        remaining -= actualTh
        collect?.invoke(
            DoseEvent(
                date = date,
                cycleNumber = cycle,
                dayInCycle = dayInCycle,
                plannedMg = toMg(plannedTh),
                actualMg = toMg(actualTh),
                isLastInCycle = remaining <= 0L,
            ),
        )
    }
    return WalkState(remaining, cycle, dayInCycle)
}

/** Pełna oś czasu podanych dawek z pozycją w cyklu i realnym zużyciem. */
fun buildTimeline(
    schedule: Schedule,
    intake: Set<String>,
    doses: Map<String, Double>,
): List<DoseEvent> {
    val p = prep(schedule) ?: return emptyList()
    val out = ArrayList<DoseEvent>()
    walk(p, sortedDates(intake, p.start), doses) { out.add(it) }
    return out
}

/**
 * Stan pierwszej jeszcze niepodanej dawki na dzień [onDate] (np. dziś).
 * Liczy stan po wszystkich podaniach ściśle wcześniejszych niż [onDate], po czym
 * projektuje jeden krok. Zwraca null, gdy brak daty startu lub [onDate] przed startem.
 */
fun nextDose(
    schedule: Schedule,
    intake: Set<String>,
    doses: Map<String, Double>,
    onDate: LocalDate,
): NextDose? {
    val p = prep(schedule) ?: return null
    if (onDate.isBefore(p.start)) return null
    val priorDates = sortedDates(intake, p.start).filter { it.isBefore(onDate) }
    val s = walk(p, priorDates, doses, null)

    var remaining = s.remainingTh
    var cycle = s.cycle
    var dayInCycle = s.dayInCycle
    if (remaining <= 0L) {
        cycle++; remaining = p.ampouleTh; dayInCycle = 0
    }
    dayInCycle++
    val plannedTh = if (remaining >= 2 * p.dailyTh) p.dailyTh else remaining
    return NextDose(
        cycleNumber = cycle,
        dayInCycle = dayInCycle,
        plannedMg = toMg(plannedTh),
        isLastInCycle = (remaining - plannedTh) <= 0L,
    )
}
```

- [ ] **Step 4: Uruchom testy — mają przejść**

Run: `gradle :app:testDebugUnitTest --no-daemon`
Expected: BUILD SUCCESSFUL, wszystkie testy `CycleTimelineTest` PASSED.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/pl/hormonwzrostu/data/CycleTimeline.kt app/src/test/java/pl/hormonwzrostu/data/CycleTimelineTest.kt
git commit -m "feat: CycleTimeline — cykl wg podań + realne mg (z testami)"
```

---

## Task 3: Trwałość — Backup, Repository, ViewModel

**Files:**
- Modify: `app/src/main/java/pl/hormonwzrostu/data/Backup.kt`
- Modify: `app/src/main/java/pl/hormonwzrostu/data/ScheduleRepository.kt`
- Modify: `app/src/main/java/pl/hormonwzrostu/MainViewModel.kt`
- Test: `app/src/test/java/pl/hormonwzrostu/data/BackupSerializationTest.kt`

- [ ] **Step 1: Napisz testy serializacji kopii**

Utwórz `app/src/test/java/pl/hormonwzrostu/data/BackupSerializationTest.kt`:

```kotlin
package pl.hormonwzrostu.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun roundTrip_withDoses() {
        val backup = Backup(version = 2, doses = mapOf("2026-01-12" to 0.8))
        val text = json.encodeToString(backup)
        val back = json.decodeFromString<Backup>(text)
        assertEquals(mapOf("2026-01-12" to 0.8), back.doses)
    }

    @Test
    fun oldJson_withoutDoses_decodesToEmpty() {
        val old = """{"version":1,"schedule":{},"intake":[],"comments":{},"lang":""}"""
        val back = json.decodeFromString<Backup>(old)
        assertTrue(back.doses.isEmpty())
    }
}
```

- [ ] **Step 2: Uruchom testy — mają się NIE skompilować**

Run: `gradle :app:testDebugUnitTest --no-daemon --tests "pl.hormonwzrostu.data.BackupSerializationTest"`
Expected: kompilacja FAIL — `no value passed for parameter 'doses'` / `unresolved reference: doses`.

- [ ] **Step 3: Dodaj pole `doses` do `Backup`**

W `app/src/main/java/pl/hormonwzrostu/data/Backup.kt` zamień całą klasę na:

```kotlin
package pl.hormonwzrostu.data

import kotlinx.serialization.Serializable

/** Pełna kopia danych aplikacji (do ręcznego eksportu/importu). */
@Serializable
data class Backup(
    val version: Int = 2,
    val schedule: Schedule = Schedule(),
    val intake: Set<String> = emptySet(),
    val comments: Map<String, String> = emptyMap(),
    val lang: String = "",
    /** Data ISO -> faktycznie podana dawka (mg). Brak wpisu = dawka wg planu. */
    val doses: Map<String, Double> = emptyMap(),
)
```

- [ ] **Step 4: Uruchom testy — mają przejść**

Run: `gradle :app:testDebugUnitTest --no-daemon --tests "pl.hormonwzrostu.data.BackupSerializationTest"`
Expected: PASSED (oba testy).

- [ ] **Step 5: Rozszerz `ScheduleRepository` o `doses`**

W `app/src/main/java/pl/hormonwzrostu/data/ScheduleRepository.kt`:

a) Po metodach `loadComments`/`saveComments` (po linii 41) dodaj:

```kotlin
    /** Mapa data (ISO) -> faktycznie podana dawka (mg). Brak wpisu = dawka wg planu. */
    fun loadDoses(): Map<String, Double> {
        val raw = prefs.getString(KEY_DOSES, null) ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, Double>>(raw) }.getOrDefault(emptyMap())
    }

    fun saveDoses(doses: Map<String, Double>) {
        prefs.edit().putString(KEY_DOSES, json.encodeToString(doses)).apply()
    }
```

b) Zamień `exportBackup` (linie 51-52) na:

```kotlin
    /** Pełna kopia wszystkich danych jako JSON. */
    fun exportBackup(): String =
        json.encodeToString(Backup(2, load(), loadIntake(), loadComments(), loadLang(), loadDoses()))
```

c) W `importBackup` (po `saveLang(backup.lang)`, linia 60) dodaj linię:

```kotlin
        saveDoses(backup.doses)
```

d) W `companion object` po `private const val KEY_LANG = "ui_lang"` dodaj:

```kotlin
        private const val KEY_DOSES = "intake_doses"
```

- [ ] **Step 6: Rozszerz `MainViewModel` o stan `doses`**

W `app/src/main/java/pl/hormonwzrostu/MainViewModel.kt`:

a) Po deklaracji `comments` (po linii 25) dodaj:

```kotlin
    /** Mapa data (ISO) -> faktycznie podana dawka (mg). */
    var doses by mutableStateOf(repository.loadDoses())
        private set
```

b) Po metodzie `commentFor` (po linii 54) dodaj:

```kotlin
    /** Zapisuje faktycznie podaną dawkę dnia; null/≤0 = usuwa override (dawka wg planu). */
    fun setActualDose(date: LocalDate, mg: Double?) {
        val iso = date.toString()
        val updated = if (mg == null || mg <= 0.0) doses - iso else doses + (iso to mg)
        doses = updated
        repository.saveDoses(updated)
    }

    fun actualDoseFor(date: LocalDate): Double? = doses[date.toString()]
```

c) W metodzie `reload` (linie 57-61) dodaj na końcu:

```kotlin
        doses = repository.loadDoses()
```

- [ ] **Step 7: Uruchom wszystkie testy + skompiluj główny moduł**

Run: `gradle :app:testDebugUnitTest --no-daemon`
Expected: BUILD SUCCESSFUL, wszystkie testy PASSED (kompilacja `main` też przechodzi).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/pl/hormonwzrostu/data/Backup.kt app/src/main/java/pl/hormonwzrostu/data/ScheduleRepository.kt app/src/main/java/pl/hormonwzrostu/MainViewModel.kt app/src/test/java/pl/hormonwzrostu/data/BackupSerializationTest.kt
git commit -m "feat: trwałość faktycznych dawek (doses) w repo, VM i kopii"
```

---

## Task 4: Eksport na bazie timeline (TDD)

**Files:**
- Modify: `app/src/main/java/pl/hormonwzrostu/data/Adherence.kt`
- Modify: `app/src/main/java/pl/hormonwzrostu/util/XlsxExport.kt`
- Modify: `app/src/main/java/pl/hormonwzrostu/ui/MainScreen.kt` (tylko wywołanie `buildIntakeRows`)
- Test: `app/src/test/java/pl/hormonwzrostu/data/IntakeRowsTest.kt`

- [ ] **Step 1: Napisz testy `buildIntakeRows`**

Utwórz `app/src/test/java/pl/hormonwzrostu/data/IntakeRowsTest.kt`:

```kotlin
package pl.hormonwzrostu.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class IntakeRowsTest {

    private val labels = CsvLabels(
        date = "Data", day = "Dzień", dose = "Dawka", status = "Status", comment = "Komentarz",
        given = "Podano", missed = "Pominięto", pending = "Oczekuje",
    )

    private fun sched() =
        Schedule(childName = "Dziecko", ampouleMg = 10.0, dailyDoseMg = 0.8, startDateIso = "2026-01-01")

    @Test
    fun givenMissedAndCorrectedDose() {
        val intake = setOf("2026-01-01", "2026-01-03") // 2026-01-02 pominięty
        val doses = mapOf("2026-01-03" to 0.5)         // faktyczna korekta
        val rows = buildIntakeRows(sched(), intake, doses, emptyMap(), LocalDate.parse("2026-01-03"), labels)

        assertEquals(3, rows.size)
        assertEquals(IntakeRow("2026-01-01", 1, 0.8, "Podano", ""), rows[0])
        assertEquals(IntakeRow("2026-01-02", null, null, "Pominięto", ""), rows[1])
        // Dzień cyklu = 2 (pominięcie nie liczone), dawka = faktyczna 0,5.
        assertEquals(IntakeRow("2026-01-03", 2, 0.5, "Podano", ""), rows[2])
    }

    @Test
    fun todayPending_blankDayAndDose() {
        val rows = buildIntakeRows(sched(), emptySet(), emptyMap(), emptyMap(), LocalDate.parse("2026-01-01"), labels)
        assertEquals(1, rows.size)
        assertEquals(IntakeRow("2026-01-01", null, null, "Oczekuje", ""), rows[0])
    }
}
```

- [ ] **Step 2: Uruchom testy — mają się NIE skompilować**

Run: `gradle :app:testDebugUnitTest --no-daemon --tests "pl.hormonwzrostu.data.IntakeRowsTest"`
Expected: FAIL — sygnatura `buildIntakeRows` nie pasuje (brak parametru `doses`); `IntakeRow` nie przyjmuje `null` dla `day`/`doseMg`.

- [ ] **Step 3: Zmień `IntakeRow` i `buildIntakeRows`**

W `app/src/main/java/pl/hormonwzrostu/data/Adherence.kt`:

a) Zamień `data class IntakeRow` (linie 38-44) na:

```kotlin
/** Pojedynczy wiersz zestawienia do eksportu (xlsx). day/doseMg = null dla dni bez podania. */
data class IntakeRow(
    val date: String,
    val day: Int?,
    val doseMg: Double?,
    val status: String,
    val comment: String,
)
```

b) Zamień całą funkcję `buildIntakeRows` (linie 50-81) na:

```kotlin
/**
 * Buduje wiersze zestawienia od dnia startu do dziś włącznie. Dzień cyklu i dawka pochodzą
 * z faktycznego przebiegu podań ([buildTimeline]); dni pominięte/oczekujące mają puste
 * dzień i dawkę. Etykiety statusów wg [labels].
 */
fun buildIntakeRows(
    schedule: Schedule,
    intake: Set<String>,
    doses: Map<String, Double>,
    comments: Map<String, String>,
    today: LocalDate,
    labels: CsvLabels,
): List<IntakeRow> {
    val rows = mutableListOf<IntakeRow>()
    val start = schedule.startDate() ?: return rows
    val byDate = buildTimeline(schedule, intake, doses).associateBy { it.date }
    var date = start
    while (!date.isAfter(today)) {
        val iso = date.toString()
        val comment = comments[iso] ?: ""
        when (dayStatus(schedule, date, today, intake)) {
            DayStatus.GIVEN -> {
                val ev = byDate[date]
                rows.add(IntakeRow(iso, ev?.dayInCycle, ev?.actualMg, labels.given, comment))
            }
            DayStatus.TODAY_PENDING -> rows.add(IntakeRow(iso, null, null, labels.pending, comment))
            else -> rows.add(IntakeRow(iso, null, null, labels.missed, comment))
        }
        date = date.plusDays(1)
    }
    return rows
}
```

- [ ] **Step 4: Obsłuż puste komórki w `XlsxExport`**

W `app/src/main/java/pl/hormonwzrostu/util/XlsxExport.kt` zamień dwie linie w pętli danych (linie 37-38):

```kotlin
        sheet.append(numCell("B$rn", row.day.toString()))
        sheet.append(numCell("C$rn", doseXml(row.doseMg)))
```

na:

```kotlin
        sheet.append(if (row.day != null) numCell("B$rn", row.day.toString()) else strCell("B$rn", "", 0))
        sheet.append(if (row.doseMg != null) numCell("C$rn", doseXml(row.doseMg)) else strCell("C$rn", "", 0))
```

- [ ] **Step 5: Zaktualizuj wywołanie w `MainScreen.ExportButton`**

W `app/src/main/java/pl/hormonwzrostu/ui/MainScreen.kt`, w `ExportButton` zamień wywołanie (linia 270):

```kotlin
            val rows = buildIntakeRows(schedule, intake, comments, today, labels)
```

na:

```kotlin
            val rows = buildIntakeRows(schedule, intake, doses, comments, today, labels)
```

Dodaj parametr `doses` do sygnatury `ExportButton` (linia 249-254):

```kotlin
@Composable
private fun ExportButton(
    schedule: Schedule,
    intake: Set<String>,
    doses: Map<String, Double>,
    comments: Map<String, String>,
    today: LocalDate,
) {
```

I w miejscu wywołania `ExportButton(...)` w `MainScreen` (linia 117) dodaj `doses`:

```kotlin
                ExportButton(schedule, intake, doses, comments, today)
```

> Uwaga: `doses` trafi do `MainScreen` w Task 6. Jeśli ten task jest wykonywany przed Task 6, kompilacja `MainScreen` chwilowo nie przejdzie — testy JVM (`testDebugUnitTest`) i tak działają, bo nie zależą od `MainScreen`. Pełna kompilacja modułu nastąpi po Task 6.

- [ ] **Step 6: Uruchom testy — mają przejść**

Run: `gradle :app:testDebugUnitTest --no-daemon`
Expected: BUILD SUCCESSFUL, `IntakeRowsTest` PASSED, pozostałe testy nadal PASSED.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/pl/hormonwzrostu/data/Adherence.kt app/src/main/java/pl/hormonwzrostu/util/XlsxExport.kt app/src/main/java/pl/hormonwzrostu/ui/MainScreen.kt app/src/test/java/pl/hormonwzrostu/data/IntakeRowsTest.kt
git commit -m "feat: eksport wg faktycznego przebiegu (dzień z podań, dawka faktyczna)"
```

---

## Task 5: Nowe stringi UI

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-pl/strings.xml`

- [ ] **Step 1: Dodaj stringi angielskie**

W `app/src/main/res/values/strings.xml`, po linii `<string name="day_of_cycle">%1$s • day %2$d/%3$d</string>` (linia 17) dodaj:

```xml
    <string name="day_of_cycle_est">%1$s • day %2$d/~%3$d</string>
```

oraz po linii `<string name="field_comment">Comment (optional)</string>` (linia 77) dodaj:

```xml
    <string name="field_actual_dose">Given dose (mg)</string>
```

- [ ] **Step 2: Dodaj stringi polskie**

W `app/src/main/res/values-pl/strings.xml`, po linii `<string name="day_of_cycle">%1$s • dzień %2$d/%3$d</string>` (linia 15) dodaj:

```xml
    <string name="day_of_cycle_est">%1$s • dzień %2$d/~%3$d</string>
```

oraz po linii `<string name="field_comment">Komentarz (opcjonalnie)</string>` (linia 75) dodaj:

```xml
    <string name="field_actual_dose">Podana dawka (mg)</string>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-pl/strings.xml
git commit -m "i18n: etykiety pola dawki i szacowanej długości cyklu"
```

---

## Task 6: Ekran główny — karta „dziś" na `nextDose` + przekazanie `doses`

**Files:**
- Modify: `app/src/main/java/pl/hormonwzrostu/ui/MainScreen.kt`
- Modify: `app/src/main/java/pl/hormonwzrostu/MainActivity.kt`

- [ ] **Step 1: Rozszerz sygnaturę `MainScreen`**

W `app/src/main/java/pl/hormonwzrostu/ui/MainScreen.kt` zamień sygnaturę `MainScreen` (linie 56-64) na:

```kotlin
@Composable
fun MainScreen(
    schedule: Schedule,
    intake: Set<String>,
    comments: Map<String, String>,
    doses: Map<String, Double>,
    onSetGiven: (LocalDate, Boolean) -> Unit,
    onSetComment: (LocalDate, String) -> Unit,
    onSetActualDose: (LocalDate, Double?) -> Unit,
    onOpenSettings: () -> Unit,
) {
```

- [ ] **Step 2: Przekaż `doses` do `TodayDoseCard` i `ExportButton`**

W body `MainScreen`, zamień wywołania (linie 109-117) tak, by `TodayDoseCard` i `ExportButton` dostały `doses`:

```kotlin
                TodayDoseCard(
                    schedule = schedule,
                    intake = intake,
                    doses = doses,
                    today = today,
                    onMark = { selected = today },
                    onUndo = { onSetGiven(today, false) },
                )
                CalendarCard(schedule, intake, today, onPickDay = { selected = it })
                ExportButton(schedule, intake, doses, comments, today)
```

- [ ] **Step 3: Przepisz `TodayDoseCard` na `nextDose`**

Zamień całą funkcję `TodayDoseCard` (linie 183-246) na:

```kotlin
@Composable
private fun TodayDoseCard(
    schedule: Schedule,
    intake: Set<String>,
    doses: Map<String, Double>,
    today: LocalDate,
    onMark: () -> Unit,
    onUndo: () -> Unit,
) {
    val next = nextDose(schedule, intake, doses, today)
    val given = intake.contains(today.toString())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.today_dose_title), style = MaterialTheme.typography.titleMedium)

            if (next == null) {
                Text(stringResource(R.string.cycle_not_started), style = MaterialTheme.typography.bodyMedium)
            } else {
                // Jeśli dziś już podano, pokaż faktyczną dawkę; w przeciwnym razie planowaną.
                val shownDose = if (given) (doses[today.toString()] ?: next.plannedMg) else next.plannedMg

                Text(
                    stringResource(R.string.mg_value, formatMg(shownDose)),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(
                        R.string.day_of_cycle_est,
                        schedule.childName,
                        next.dayInCycle,
                        schedule.daysPerCycle,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (next.isLastInCycle) {
                    Text(
                        stringResource(R.string.last_dose_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.padding(2.dp))
                if (given) {
                    Text(
                        stringResource(R.string.given_today_done),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    TextButton(onClick = onUndo) {
                        Text(stringResource(R.string.btn_unmark_given))
                    }
                } else {
                    Button(onClick = onMark, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.btn_mark_given))
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 4: Dodaj import `nextDose`**

W `app/src/main/java/pl/hormonwzrostu/ui/MainScreen.kt`, w sekcji importów obok `import pl.hormonwzrostu.data.dayStatus` (linia 45) dodaj:

```kotlin
import pl.hormonwzrostu.data.nextDose
```

> `DayEditDialog` w bloku `selected?.let { ... }` (linie 134-152) dostanie nowe parametry w Task 7 — na razie zostaw jak jest; pełna kompilacja UI domyka się po Task 7. Krok kompilacji modułu wykonujemy w Task 7.

- [ ] **Step 5: Zaktualizuj wywołanie `MainScreen` w `MainActivity`**

W `app/src/main/java/pl/hormonwzrostu/MainActivity.kt` zamień blok `MainScreen(...)` (linie 56-63) na:

```kotlin
                    Screen.MAIN -> MainScreen(
                        schedule = vm.schedule,
                        intake = vm.intake,
                        comments = vm.comments,
                        doses = vm.doses,
                        onSetGiven = { date, given -> vm.setGiven(date, given) },
                        onSetComment = { date, text -> vm.setComment(date, text) },
                        onSetActualDose = { date, mg -> vm.setActualDose(date, mg) },
                        onOpenSettings = { screen = Screen.SETTINGS },
                    )
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/pl/hormonwzrostu/ui/MainScreen.kt app/src/main/java/pl/hormonwzrostu/MainActivity.kt
git commit -m "feat: karta dziś liczona z nextDose; przekazanie doses przez UI"
```

---

## Task 7: Dialog dnia — pole „Podana dawka (mg)" + numeracja z timeline

**Files:**
- Modify: `app/src/main/java/pl/hormonwzrostu/ui/MainScreen.kt` (blok `selected?.let`)
- Modify: `app/src/main/java/pl/hormonwzrostu/ui/Calendar.kt` (`DayEditDialog`)

- [ ] **Step 1: Przepisz blok `selected?.let` w `MainScreen`**

W `app/src/main/java/pl/hormonwzrostu/ui/MainScreen.kt` zamień blok (linie 134-152) na:

```kotlin
    selected?.let { date ->
        val status = dayStatus(schedule, date, today, intake)
        val timeline = buildTimeline(schedule, intake, doses)
        val event = timeline.firstOrNull { it.date == date }
        // Planowana dawka dla tego dnia: z przebiegu (gdy podano) lub projekcja na ten dzień.
        val plannedMg = event?.plannedMg
            ?: nextDose(schedule, intake, doses, date)?.plannedMg
            ?: schedule.dailyDoseMg
        DayEditDialog(
            date = date,
            schedule = schedule,
            status = status,
            dayInCycle = event?.dayInCycle,
            plannedMg = plannedMg,
            actualMg = doses[date.toString()],
            initialComment = comments[date.toString()] ?: "",
            onConfirm = { given, comment, doseMg ->
                onSetComment(date, comment)
                onSetActualDose(date, if (given) doseMg else null)
                onSetGiven(date, given)
                selected = null
            },
            onSaveComment = { comment ->
                onSetComment(date, comment)
                selected = null
            },
            onDismiss = { selected = null },
        )
    }
```

- [ ] **Step 2: Dodaj import `buildTimeline`**

W `app/src/main/java/pl/hormonwzrostu/ui/MainScreen.kt`, obok importu `nextDose` dodaj:

```kotlin
import pl.hormonwzrostu.data.buildTimeline
```

- [ ] **Step 3: Przepisz `DayEditDialog` w `Calendar.kt`**

W `app/src/main/java/pl/hormonwzrostu/ui/Calendar.kt` zamień całą funkcję `DayEditDialog` (linie 213-290) na:

```kotlin
/**
 * Okno edycji jednego dnia: data, dzień cyklu/dawka, pole faktycznie podanej dawki, komentarz
 * oraz akcje Podano (zielony) / Pominięto (czerwony) / Zapisz (sam komentarz).
 * Pole dawki widoczne, gdy dzień można podać (dziś, wstecz). „X" zamyka bez zmian.
 */
@Composable
fun DayEditDialog(
    date: LocalDate,
    schedule: Schedule,
    status: DayStatus,
    dayInCycle: Int?,
    plannedMg: Double,
    actualMg: Double?,
    initialComment: String,
    onConfirm: (given: Boolean, comment: String, doseMg: Double?) -> Unit,
    onSaveComment: (comment: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var comment by remember(date) { mutableStateOf(initialComment) }
    var doseText by remember(date) { mutableStateOf(formatMg(actualMg ?: plannedMg)) }
    // Pole dawki ma sens dla dni, które realnie można podać (nie dla przyszłych).
    val canDose = status != DayStatus.UPCOMING && status != DayStatus.NONE

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Box(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(end = 36.dp),
                    )
                    if (dayInCycle != null) {
                        Text(
                            stringResource(
                                R.string.edit_day_info,
                                dayInCycle,
                                schedule.daysPerCycle,
                                formatMg(actualMg ?: plannedMg),
                            ),
                        )
                    }
                    Text(stringResource(R.string.edit_current, statusWord(status)))

                    if (canDose) {
                        OutlinedTextField(
                            value = doseText,
                            onValueChange = { doseText = it },
                            label = { Text(stringResource(R.string.field_actual_dose)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text(stringResource(R.string.field_comment)) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { onConfirm(true, comment, parseDose(doseText, plannedMg)) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GivenColor, contentColor = Color.White),
                        ) { Text(stringResource(R.string.legend_given)) }
                        Button(
                            onClick = { onConfirm(false, comment, null) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MissedColor, contentColor = Color.White),
                        ) { Text(stringResource(R.string.legend_missed)) }
                    }
                    TextButton(
                        onClick = { onSaveComment(comment) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.btn_save)) }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.btn_cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Parsuje wpisaną dawkę (akceptuje przecinek i kropkę). Zwraca null, gdy puste, niepoprawne,
 * ≤ 0 lub równe dawce planowanej — null oznacza „bez override, trzymaj się rozpiski".
 */
private fun parseDose(text: String, plannedMg: Double): Double? {
    val value = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (value <= 0.0) return null
    val rounded = Math.round(value * 1000.0) / 1000.0
    val plannedRounded = Math.round(plannedMg * 1000.0) / 1000.0
    return if (rounded == plannedRounded) null else rounded
}
```

- [ ] **Step 4: Dodaj importy w `Calendar.kt`**

W `app/src/main/java/pl/hormonwzrostu/ui/Calendar.kt`, w sekcji importów dodaj (po istniejących importach `androidx.compose.foundation...`):

```kotlin
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
```

- [ ] **Step 5: Skompiluj cały moduł (debug)**

Run: `gradle :app:assembleDebug --no-daemon`
Expected: BUILD SUCCESSFUL (UI domknięte — sygnatury `MainScreen` ↔ `MainActivity` ↔ `DayEditDialog` zgodne).

- [ ] **Step 6: Uruchom testy JVM**

Run: `gradle :app:testDebugUnitTest --no-daemon`
Expected: BUILD SUCCESSFUL, wszystkie testy PASSED.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/pl/hormonwzrostu/ui/MainScreen.kt app/src/main/java/pl/hormonwzrostu/ui/Calendar.kt
git commit -m "feat: dialog dnia z polem faktycznej dawki + numeracja z timeline"
```

---

## Task 8: Powiadomienie na `nextDose`

**Files:**
- Modify: `app/src/main/java/pl/hormonwzrostu/notify/ReminderReceiver.kt`

- [ ] **Step 1: Przepisz `onReceive` na `nextDose`**

W `app/src/main/java/pl/hormonwzrostu/notify/ReminderReceiver.kt` zamień blok `if (schedule.enabled && schedule.isValid()) { ... }` (linie 17-40) na:

```kotlin
        if (schedule.enabled && schedule.isValid()) {
            val today = LocalDate.now()
            val repo = ScheduleRepository(context)
            val next = pl.hormonwzrostu.data.nextDose(schedule, repo.loadIntake(), repo.loadDoses(), today)
            if (next != null) {
                val title = context.getString(
                    R.string.notif_title,
                    schedule.childName,
                    formatMg(next.plannedMg),
                )
                val medLine = schedule.medName +
                    if (next.isLastInCycle) context.getString(R.string.notif_last_suffix) else ""
                val text = context.getString(
                    R.string.notif_text,
                    next.dayInCycle,
                    schedule.daysPerCycle,
                    medLine,
                )
                showDoseNotification(context, title, text)
            }
        }
```

> `schedule.daysPerCycle` w tekście to wartość szacowana długości cyklu — akceptowalne w powiadomieniu (krótki komunikat). `repo` powstaje raz; uwaga: `val schedule = ScheduleRepository(context).load()` z linii 15 zostaje bez zmian.

- [ ] **Step 2: Skompiluj moduł**

Run: `gradle :app:assembleDebug --no-daemon`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/pl/hormonwzrostu/notify/ReminderReceiver.kt
git commit -m "feat: powiadomienie liczone z nextDose (cykl wg podań)"
```

---

## Task 9: Podbicie wersji + bramka testów w CI

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `.github/workflows/build.yml`

- [ ] **Step 1: Podbij wersję**

W `app/build.gradle.kts` zamień (linie 16-17):

```kotlin
        versionCode = 16
        versionName = "1.15"
```

na:

```kotlin
        versionCode = 17
        versionName = "1.16"
```

- [ ] **Step 2: Dodaj krok testów w CI przed budową**

W `.github/workflows/build.yml`, przed krokiem `- name: Build debug APK` (linia 38) wstaw:

```yaml
      - name: Run unit tests
        run: gradle testDebugUnitTest --no-daemon --stacktrace

```

- [ ] **Step 3: Lokalna weryfikacja pełna**

Run: `gradle :app:testDebugUnitTest :app:assembleDebug --no-daemon`
Expected: BUILD SUCCESSFUL; testy PASSED; APK w `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts .github/workflows/build.yml
git commit -m "build: wersja 1.16 (17) + bramka testów JVM w CI"
```

---

## Task 10: Budowa, instalacja na telefonie i weryfikacja end-to-end

**Files:** brak zmian kodu — weryfikacja realnego działania (wymóg użytkownika: nie deklaruj „gotowe" bez uruchomienia).

- [ ] **Step 1: Zbuduj debug APK**

Run: `gradle :app:assembleDebug --no-daemon`
Expected: BUILD SUCCESSFUL, plik `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 2: Zainstaluj na telefonie przez ADB**

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
Expected: `Success`. (Telefon ma Advanced Protection — instalacja przez ADB, nie sideload.)

- [ ] **Step 3: Weryfikacja — cykl nie gubi się przy pominięciu**

Scenariusz ręczny w aplikacji (lub opisany do potwierdzenia przez użytkownika):
1. Ustaw schemat (np. ampułka 10 mg, dawka 0,8 mg, data startu kilka dni wstecz).
2. Oznacz kilka dni „Podano", jeden dzień zostaw/oznacz „Pominięto".
3. Sprawdź, że karta „dziś" pokazuje numer dnia = liczba podań + 1 (pominięty dzień NIE przesuwa numeru) i że dzień się nie zwiększa, dopóki nie oznaczysz „Podano".

Expected: numer dnia cyklu rośnie tylko po faktycznym podaniu.

- [ ] **Step 4: Weryfikacja — awaryjna edycja dawki**

1. Otwórz dzień w kalendarzu, w polu „Podana dawka (mg)" wpisz inną wartość (np. 0,8 zamiast 1,2), kliknij „Podano".
2. Eksportuj do Excela i sprawdź, że w wierszu widnieje faktyczna dawka 0,8.
3. Sprawdź, że pozostała część ampułki przesuwa koniec cyklu (dochodzi dodatkowy dzień z resztką).

Expected: faktyczna dawka zapisana; zużycie ampułki uwzględnia korektę.

- [ ] **Step 5: Raport**

Zaraportuj wynik uczciwie: które kroki potwierdzone realnym uruchomieniem, ewentualne odstępstwa (zgodnie z preferencją użytkownika — dowód z outputu/zachowania, bez deklaracji sukcesu bez weryfikacji).

---

## Self-review (wykonane)

- **Pokrycie specyfikacji:** model danych `doses` (Task 3) ✓; rdzeń `CycleTimeline` z realnymi mg i regułą ostatniego dnia (Task 2) ✓; `nextDose` dla karty „dziś" i powiadomienia (Task 2, 6, 8) ✓; UI karta dziś + dialog z polem mg, edycja też dla pominiętych dni B (Task 6, 7) ✓; szacowana długość cyklu „~M" — decyzja A (Task 5, 6) ✓; eksport wg faktycznego przebiegu (Task 4) ✓; migracja bez konwersji (brak twardego kroku — `doses` startuje puste, historia przeliczana) ✓; testy splotów (Task 2) ✓.
- **Brak placeholderów:** każdy krok zawiera pełny kod/komendę i oczekiwany wynik.
- **Spójność typów:** `buildTimeline(schedule, intake, doses)`, `nextDose(schedule, intake, doses, onDate)`, `DoseEvent`, `NextDose`, `IntakeRow(date, day:Int?, doseMg:Double?, status, comment)`, `buildIntakeRows(schedule, intake, doses, comments, today, labels)`, `DayEditDialog(..., dayInCycle, plannedMg, actualMg, ..., onConfirm:(Boolean,String,Double?)->Unit)`, `MainScreen(..., doses, ..., onSetActualDose, ...)`, `setActualDose(date, mg:Double?)` — używane spójnie między taskami.
- **Kolejność kompilacji:** zmiany UI rozłożone tak, że testy JVM przechodzą po każdym tasku; pełna kompilacja modułu domyka się w Task 7 (assembleDebug), co jest jawnie zaznaczone.
