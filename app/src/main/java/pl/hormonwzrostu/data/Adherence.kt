package pl.hormonwzrostu.data

import java.time.LocalDate

/** Status pojedynczego dnia względem schematu i zarejestrowanych podań. */
enum class DayStatus { GIVEN, MISSED, TODAY_PENDING, UPCOMING, NONE }

/** Wyznacza status dnia: brak/podano/pominięto/dziś/później. */
fun dayStatus(
    schedule: Schedule,
    date: LocalDate,
    today: LocalDate,
    intake: Set<String>,
): DayStatus {
    val start = schedule.startDate() ?: return DayStatus.NONE
    if (date.isBefore(start)) return DayStatus.NONE
    if (intake.contains(date.toString())) return DayStatus.GIVEN
    return when {
        date.isBefore(today) -> DayStatus.MISSED
        date.isEqual(today) -> DayStatus.TODAY_PENDING
        else -> DayStatus.UPCOMING
    }
}

/** Zlokalizowane etykiety nagłówków i statusów do eksportu CSV. */
data class CsvLabels(
    val date: String,
    val day: String,
    val dose: String,
    val status: String,
    val comment: String,
    val given: String,
    val missed: String,
    val pending: String,
)

/** Pojedynczy wiersz zestawienia do eksportu (xlsx). day/doseMg = null dla dni bez podania. */
data class IntakeRow(
    val date: String,
    val day: Int?,
    val doseMg: Double?,
    val status: String,
    val comment: String,
)

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
