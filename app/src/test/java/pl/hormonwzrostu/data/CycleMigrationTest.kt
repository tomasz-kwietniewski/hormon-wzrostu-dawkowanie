package pl.hormonwzrostu.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CycleMigrationTest {

    private fun sched(start: String) =
        Schedule(childName = "Dziecko", ampouleMg = 10.0, dailyDoseMg = 0.8, startDateIso = start)

    /** Wszystkie daty od [from] do [to] włącznie. */
    private fun range(from: String, to: String): Set<String> {
        val a = LocalDate.parse(from)
        val b = LocalDate.parse(to)
        return generateSequence(a) { d -> d.plusDays(1).takeIf { !it.isAfter(b) } }
            .map { it.toString() }
            .toSet()
    }

    @Test
    fun autoStart_followedByManualAnchor_isDropped() {
        // 12 dni po 0,8 wyczerpuje ampułkę -> stara logika robiła auto-start 13. dnia,
        // ale użytkownik oznaczył nową ampułkę dopiero 14. dnia.
        val intake = range("2026-01-01", "2026-01-14")
        val anchors = setOf("2026-01-14")
        assertTrue(legacyAutoAnchors(sched("2026-01-01"), intake, emptyMap(), anchors).isEmpty())
    }

    @Test
    fun autoStart_withoutManualAnchor_isKept() {
        val intake = range("2026-01-01", "2026-01-14")
        assertEquals(
            setOf("2026-01-13"),
            legacyAutoAnchors(sched("2026-01-01"), intake, emptyMap(), emptySet()),
        )
    }

    @Test
    fun noAutoStart_returnsEmpty() {
        val intake = range("2026-01-01", "2026-01-05")
        assertTrue(legacyAutoAnchors(sched("2026-01-01"), intake, emptyMap(), emptySet()).isEmpty())
    }

    @Test
    fun skippedDaysBetween_doNotBreakTheMatch() {
        // Liczy się najbliższe kolejne PODANIE, nie kolejny dzień kalendarza:
        // auto-start 13. dnia, przerwa, a ręczne oznaczenie trzy dni później.
        val intake = range("2026-01-01", "2026-01-13") + "2026-01-17"
        val anchors = setOf("2026-01-17")
        assertTrue(legacyAutoAnchors(sched("2026-01-01"), intake, emptyMap(), anchors).isEmpty())
    }

    @Test
    fun noStartDate_returnsEmpty() {
        assertTrue(legacyAutoAnchors(Schedule(), range("2026-01-01", "2026-01-20"), emptyMap(), emptySet()).isEmpty())
    }

    // --- Realna historia z eksportu hormon_Jeremiasz_2026-08-05.xlsx ---

    private val realIntake =
        range("2026-05-29", "2026-06-16") + // 06-17, 06-18 — pominięte (szpital)
            range("2026-06-19", "2026-07-12") + // 07-13, 07-14 — pominięte (brak igieł)
            range("2026-07-15", "2026-07-28") + // 07-29 — pominięte (nocowanka)
            range("2026-07-30", "2026-07-31") + // 08-01 — pominięte (wyjazd)
            range("2026-08-02", "2026-08-04")

    /** Dawki inne niż 0,8 mg — odczytane z kolumny „Dawka (mg)" eksportu. */
    private val realDoses = mapOf(
        "2026-06-10" to 1.2,
        "2026-06-20" to 0.5,
        "2026-06-21" to 1.2,
        "2026-07-03" to 0.5,
        "2026-07-17" to 0.8,
        "2026-07-18" to 0.5,
        "2026-07-31" to 0.8,
        "2026-08-02" to 0.8,
        "2026-08-03" to 0.2,
        "2026-08-04" to 1.2,
    )

    /** Granice ampułek, które użytkownik oznaczył ręcznie. */
    private val realAnchors = setOf("2026-06-06", "2026-06-21", "2026-07-04", "2026-08-04")

    @Test
    fun realHistory_keepsOnlyTheAnchorlessBoundary() {
        // 20.06, 03.07 i 03.08 to dociągnięte końcówki — nazajutrz użytkownik oznaczał
        // nową ampułkę, więc te granice odpadają. 19.07 nie ma następnika-kotwicy.
        assertEquals(
            setOf("2026-07-19"),
            legacyAutoAnchors(sched("2026-05-29"), realIntake, realDoses, realAnchors),
        )
    }

    @Test
    fun realHistory_afterMigration_matchesUserExpectation() {
        val migrated = realAnchors + legacyAutoAnchors(sched("2026-05-29"), realIntake, realDoses, realAnchors)
        val tl = buildTimeline(sched("2026-05-29"), realIntake, realDoses, migrated)
            .associateBy { it.date.toString() }

        // Granice ampułek — dzień 1 wyłącznie tam, gdzie zaczyna się nowa ampułka.
        listOf("2026-05-29", "2026-06-06", "2026-06-21", "2026-07-04", "2026-07-19", "2026-08-04")
            .forEach { assertEquals(it, 1, tl[it]!!.dayInCycle) }

        // Dni, które stara logika fałszywie numerowała jako „1".
        assertEquals(13, tl["2026-06-20"]!!.dayInCycle)
        assertEquals(13, tl["2026-07-03"]!!.dayInCycle)
        assertEquals(14, tl["2026-08-03"]!!.dayInCycle)

        // Ostatni pełny cykl w całości — z przeskokami kalendarza na pominięciach.
        assertEquals(10, tl["2026-07-28"]!!.dayInCycle)
        assertEquals(11, tl["2026-07-30"]!!.dayInCycle)
        assertEquals(12, tl["2026-07-31"]!!.dayInCycle)
        assertEquals(13, tl["2026-08-02"]!!.dayInCycle)

        // Sześć ampułek po kolei, bez dziur w numeracji.
        assertEquals(6, tl.values.maxOf { it.cycleNumber })
    }

    @Test
    fun realHistory_ampouleStateWarnsBeforeTheEnd() {
        val migrated = realAnchors + legacyAutoAnchors(sched("2026-05-29"), realIntake, realDoses, realAnchors)
        val tl = buildTimeline(sched("2026-05-29"), realIntake, realDoses, migrated)
            .associateBy { it.date.toString() }

        assertEquals(AmpouleState.LAST_FULL, tl["2026-07-31"]!!.ampouleState)
        assertEquals(AmpouleState.REMNANT, tl["2026-08-02"]!!.ampouleState)
        assertEquals(AmpouleState.EMPTY, tl["2026-08-03"]!!.ampouleState)
        assertEquals(AmpouleState.NORMAL, tl["2026-08-04"]!!.ampouleState)
    }
}
