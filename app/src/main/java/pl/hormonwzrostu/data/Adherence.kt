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

/** Cytowanie pola CSV (RFC 4180): otacza cudzysłowami i podwaja wewnętrzne cudzysłowy. */
private fun csvQuote(s: String): String = "\"" + s.replace("\"", "\"\"") + "\""

/**
 * Buduje zestawienie CSV od dnia startu do dziś włącznie, w języku aplikacji.
 * Format przyjazny Excelowi: separator ';', hint 'sep=;', liczby z lokalnym
 * separatorem dziesiętnym, pola tekstowe w cudzysłowach.
 */
fun buildIntakeCsv(
    schedule: Schedule,
    intake: Set<String>,
    comments: Map<String, String>,
    today: LocalDate,
    labels: CsvLabels,
): String {
    val sb = StringBuilder("sep=;\n")
    sb.append(csvQuote(labels.date)).append(';')
        .append(csvQuote(labels.day)).append(';')
        .append(csvQuote(labels.dose)).append(';')
        .append(csvQuote(labels.status)).append(';')
        .append(csvQuote(labels.comment)).append('\n')

    val start = schedule.startDate() ?: return sb.toString()
    var date = start
    while (!date.isAfter(today)) {
        val idx = schedule.dayIndexInCycle(date)
        if (idx != null) {
            val statusLabel = when (dayStatus(schedule, date, today, intake)) {
                DayStatus.GIVEN -> labels.given
                DayStatus.TODAY_PENDING -> labels.pending
                else -> labels.missed
            }
            val comment = comments[date.toString()] ?: ""
            sb.append(date).append(';')
                .append(idx + 1).append(';')
                .append(formatMg(schedule.doseForDay(idx))).append(';')
                .append(csvQuote(statusLabel)).append(';')
                .append(csvQuote(comment)).append('\n')
        }
        date = date.plusDays(1)
    }
    return sb.toString()
}
