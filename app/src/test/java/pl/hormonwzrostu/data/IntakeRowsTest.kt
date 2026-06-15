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
