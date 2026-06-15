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
    fun nextDose_lastSlot_isLastInCycleTrue() {
        // 10 mg / 0,8 → 11 dni po 0,8; dzień 12 = reszta 1,2 (ostatni w ampułce).
        val nd = nextDose(sched(10.0, 0.8), dates("2026-01-01", 11), emptyMap(), LocalDate.parse("2026-01-12"))!!
        assertEquals(12, nd.dayInCycle)
        assertEquals(1.2, nd.plannedMg, 1e-9)
        assertTrue(nd.isLastInCycle)
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
