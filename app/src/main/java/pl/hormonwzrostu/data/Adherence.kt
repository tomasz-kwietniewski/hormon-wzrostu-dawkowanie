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

/** Pojedynczy wiersz zestawienia do eksportu (xlsx). */
data class IntakeRow(
    val date: String,
    val day: Int,
    val doseMg: Double,
    val status: String,
    val comment: String,
)

/**
 * Buduje wiersze zestawienia od dnia startu do dziś włącznie, ze statusami
 * w języku aplikacji (etykiety przekazane w [labels]).
 */
fun buildIntakeRows(
    schedule: Schedule,
    intake: Set<String>,
    comments: Map<String, String>,
    today: LocalDate,
    labels: CsvLabels,
): List<IntakeRow> {
    val rows = mutableListOf<IntakeRow>()
    val start = schedule.startDate() ?: return rows
    var date = start
    while (!date.isAfter(today)) {
        val idx = schedule.dayIndexInCycle(date)
        if (idx != null) {
            val statusLabel = when (dayStatus(schedule, date, today, intake)) {
                DayStatus.GIVEN -> labels.given
                DayStatus.TODAY_PENDING -> labels.pending
                else -> labels.missed
            }
            rows.add(
                IntakeRow(
                    date = date.toString(),
                    day = idx + 1,
                    doseMg = schedule.doseForDay(idx),
                    status = statusLabel,
                    comment = comments[date.toString()] ?: "",
                ),
            )
        }
        date = date.plusDays(1)
    }
    return rows
}
