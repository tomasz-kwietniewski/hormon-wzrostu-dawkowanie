package pl.hormonwzrostu.data

import org.junit.Assert.assertEquals
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
    fun plannedDoseIsAlwaysDaily_regardlessOfAmpouleState() {
        // 10 mg / 0,8 — nawet gdy w ampułce zostaje 1,2 mg albo 0,4 mg, proponujemy 0,8.
        val tl = buildTimeline(sched(10.0, 0.8), dates("2026-01-01", 14), emptyMap())
        assertEquals(14, tl.size)
        tl.forEach { assertEquals(0.8, it.plannedMg, 1e-9) }
        tl.forEach { assertEquals(0.8, it.actualMg, 1e-9) }
    }

    @Test
    fun ampouleState_thresholds() {
        val tl = buildTimeline(sched(10.0, 0.8), dates("2026-01-01", 14), emptyMap())
        // Zapas przed dawką: dzień 11 -> 2,0; dzień 12 -> 1,2; dzień 13 -> 0,4; dzień 14 -> -0,4.
        assertEquals(AmpouleState.NORMAL, tl[10].ampouleState)
        assertEquals(2.0, tl[10].remainingBeforeMg, 1e-9)
        assertEquals(AmpouleState.LAST_FULL, tl[11].ampouleState)
        assertEquals(1.2, tl[11].remainingBeforeMg, 1e-9)
        assertEquals(AmpouleState.REMNANT, tl[12].ampouleState)
        assertEquals(0.4, tl[12].remainingBeforeMg, 1e-9)
        assertEquals(AmpouleState.EMPTY, tl[13].ampouleState)
        assertEquals(-0.4, tl[13].remainingBeforeMg, 1e-9)
    }

    @Test
    fun exhaustedAmpoule_doesNotStartNewCycle() {
        // Rdzeń poprawki: bez ręcznego oznaczenia licznik idzie dalej, choć ampułka „pusta".
        val tl = buildTimeline(sched(10.0, 0.8), dates("2026-01-01", 20), emptyMap())
        assertTrue(tl.all { it.cycleNumber == 1 })
        assertEquals(20, tl.last().dayInCycle)
        assertEquals(AmpouleState.EMPTY, tl.last().ampouleState)
    }

    @Test
    fun equalDivision_noRemainder_stillNoAutoRollover() {
        // 10 mg / 0,5 dzieli się równo na 20 dni — 21. dzień to wciąż ta sama ampułka.
        val tl = buildTimeline(sched(10.0, 0.5), dates("2026-01-01", 21), emptyMap())
        assertEquals(21, tl.size)
        assertTrue(tl.all { it.cycleNumber == 1 })
        assertEquals(21, tl[20].dayInCycle)
        assertEquals(AmpouleState.EMPTY, tl[20].ampouleState)
        tl.forEach { assertEquals(0.5, it.plannedMg, 1e-9) }
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
        // Kolejny dzień bez podania — wciąż dzień 5 (rdzeń wcześniejszej poprawki użytkownika).
        val nd2 = nextDose(sched(10.0, 0.8), intake, emptyMap(), LocalDate.parse("2026-01-11"))!!
        assertEquals(5, nd2.dayInCycle)
    }

    @Test
    fun biggerDose_consumesFaster_butKeepsSameAmpoule() {
        // Dociągnięcie całej resztki (1,2 mg) w dniu 12 domyka zapas, ale nie numeruje od nowa.
        val doses = mapOf("2026-01-12" to 1.2)
        val tl = buildTimeline(sched(10.0, 0.8), dates("2026-01-01", 13), doses)
        assertEquals(1.2, tl[11].actualMg, 1e-9)
        assertEquals(0.8, tl[11].plannedMg, 1e-9)
        assertEquals(0.0, tl[12].remainingBeforeMg, 1e-9)
        assertEquals(AmpouleState.EMPTY, tl[12].ampouleState)
        assertEquals(1, tl[12].cycleNumber)
        assertEquals(13, tl[12].dayInCycle)
    }

    @Test
    fun overdose_doesNotCloseAmpouleByItself() {
        val doses = mapOf("2026-01-12" to 2.0) // więcej, niż w ampułce zostało
        val tl = buildTimeline(sched(10.0, 0.8), dates("2026-01-01", 13), doses)
        assertEquals(1, tl[12].cycleNumber)
        assertEquals(13, tl[12].dayInCycle)
        assertEquals(AmpouleState.EMPTY, tl[12].ampouleState)
        assertTrue(tl.all { it.plannedMg > 0.0 && it.actualMg > 0.0 })
    }

    /**
     * Realny przebieg z eksportu 2026-08-05: z ampułki idzie więcej dawek, niż wynika
     * z pojemności, a nowy cykl otwiera dopiero ręczne oznaczenie (2026-08-04).
     */
    @Test
    fun realCourse_noAutoRollover_dayKeepsCounting() {
        val intake = setOf(
            "2026-07-19", "2026-07-20", "2026-07-21", "2026-07-22", "2026-07-23",
            "2026-07-24", "2026-07-25", "2026-07-26", "2026-07-27", "2026-07-28",
            // 2026-07-29 — pominięte (nocowanka)
            "2026-07-30", "2026-07-31",
            // 2026-08-01 — pominięte (wyjazd)
            "2026-08-02", "2026-08-03", "2026-08-04",
        )
        val doses = mapOf("2026-08-03" to 0.2, "2026-08-04" to 1.2)
        val tl = buildTimeline(
            sched(10.0, 0.8, "2026-07-19"), intake, doses, setOf("2026-08-04"),
        ).associateBy { it.date.toString() }

        assertEquals(12, tl["2026-07-31"]!!.dayInCycle)
        assertEquals(AmpouleState.LAST_FULL, tl["2026-07-31"]!!.ampouleState)
        assertEquals(13, tl["2026-08-02"]!!.dayInCycle)
        assertEquals(AmpouleState.REMNANT, tl["2026-08-02"]!!.ampouleState)
        // Końcówka ampułki — wciąż ta sama ampułka, dzień 14.
        assertEquals(14, tl["2026-08-03"]!!.dayInCycle)
        assertEquals(1, tl["2026-08-03"]!!.cycleNumber)
        assertEquals(AmpouleState.EMPTY, tl["2026-08-03"]!!.ampouleState)
        // Dopiero ręczne oznaczenie otwiera nową ampułkę — pełny zapas i dzień 1.
        assertEquals(1, tl["2026-08-04"]!!.dayInCycle)
        assertEquals(2, tl["2026-08-04"]!!.cycleNumber)
        assertEquals(10.0, tl["2026-08-04"]!!.remainingBeforeMg, 1e-9)
        assertEquals(AmpouleState.NORMAL, tl["2026-08-04"]!!.ampouleState)
    }

    @Test
    fun emptyHistory_nextIsCycle1Day1() {
        val nd = nextDose(sched(10.0, 0.8), emptySet(), emptyMap(), LocalDate.parse("2026-01-01"))!!
        assertEquals(1, nd.cycleNumber)
        assertEquals(1, nd.dayInCycle)
        assertEquals(0.8, nd.plannedMg, 1e-9)
        assertEquals(AmpouleState.NORMAL, nd.ampouleState)
        assertTrue(buildTimeline(sched(), emptySet(), emptyMap()).isEmpty())
    }

    @Test
    fun nextDose_lastFullSlot_signalsLeftover() {
        // Po 11 podaniach po 0,8 zostaje 1,2 mg: proponujemy 0,8, ale sygnalizujemy resztkę.
        val nd = nextDose(
            sched(10.0, 0.8), dates("2026-01-01", 11), emptyMap(), LocalDate.parse("2026-01-12"),
        )!!
        assertEquals(12, nd.dayInCycle)
        assertEquals(0.8, nd.plannedMg, 1e-9)
        assertEquals(1.2, nd.remainingBeforeMg, 1e-9)
        assertEquals(AmpouleState.LAST_FULL, nd.ampouleState)
    }

    @Test
    fun nextDose_afterExhaustion_isEmptyStateNotNewCycle() {
        val nd = nextDose(
            sched(10.0, 0.8), dates("2026-01-01", 13), emptyMap(), LocalDate.parse("2026-01-14"),
        )!!
        assertEquals(1, nd.cycleNumber)
        assertEquals(14, nd.dayInCycle)
        assertEquals(AmpouleState.EMPTY, nd.ampouleState)
    }

    @Test
    fun ampouleAnchor_resetsCycleAndRenumbersDay() {
        // 7 dni po 0,8 (ampułka ma jeszcze sporo), ale 5. dzień ręcznie = nowa ampułka.
        val anchor = setOf("2026-01-05")
        val tl = buildTimeline(sched(10.0, 0.8), dates("2026-01-01", 7), emptyMap(), anchor)
        assertEquals(listOf(1, 2, 3, 4), tl.take(4).map { it.dayInCycle })
        assertTrue(tl.take(4).all { it.cycleNumber == 1 })
        // 2026-01-05: re-kotwica -> dzień 1 nowej (drugiej) ampułki, pełny zapas.
        assertEquals(2, tl[4].cycleNumber)
        assertEquals(1, tl[4].dayInCycle)
        assertEquals(10.0, tl[4].remainingBeforeMg, 1e-9)
        assertEquals(2, tl[5].dayInCycle)
        assertEquals(2, tl[6].cycleNumber)
    }

    @Test
    fun ampouleAnchor_onStartDate_isIgnored() {
        // Kotwica na samym dniu startu nie tworzy „drugiej" ampułki — dzień 1 zostaje dniem 1.
        val anchor = setOf("2026-01-01")
        val tl = buildTimeline(sched(10.0, 0.8), dates("2026-01-01", 3), emptyMap(), anchor)
        assertEquals(1, tl[0].cycleNumber)
        assertEquals(1, tl[0].dayInCycle)
    }

    @Test
    fun nextDose_withAnchorOnDate_isNewAmpouleDay1() {
        // Po 7 podaniach projekcja na 2026-01-08 z re-kotwicą = dzień 1, pełna dawka 0,8.
        val nd = nextDose(
            sched(10.0, 0.8),
            dates("2026-01-01", 7),
            emptyMap(),
            LocalDate.parse("2026-01-08"),
            setOf("2026-01-08"),
        )!!
        assertEquals(2, nd.cycleNumber)
        assertEquals(1, nd.dayInCycle)
        assertEquals(0.8, nd.plannedMg, 1e-9)
        assertEquals(AmpouleState.NORMAL, nd.ampouleState)
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
