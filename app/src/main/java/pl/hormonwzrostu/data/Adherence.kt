package pl.hormonwzrostu.data

import java.math.BigDecimal
import java.math.RoundingMode
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

/** Dawka w formacie maszynowym do CSV (zawsze kropka dziesiętna). */
private fun doseCsv(value: Double): String =
    BigDecimal(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

/**
 * Buduje zestawienie CSV od dnia startu do dziś włącznie:
 * date,day_of_cycle,dose_mg,status (status: given/missed/pending).
 */
fun buildIntakeCsv(
    schedule: Schedule,
    intake: Set<String>,
    today: LocalDate,
): String {
    val sb = StringBuilder("date,day_of_cycle,dose_mg,status\n")
    val start = schedule.startDate() ?: return sb.toString()
    var date = start
    while (!date.isAfter(today)) {
        val idx = schedule.dayIndexInCycle(date)
        if (idx != null) {
            val status = when (dayStatus(schedule, date, today, intake)) {
                DayStatus.GIVEN -> "given"
                DayStatus.TODAY_PENDING -> "pending"
                else -> "missed"
            }
            sb.append(date).append(',')
                .append(idx + 1).append(',')
                .append(doseCsv(schedule.doseForDay(idx))).append(',')
                .append(status).append('\n')
        }
        date = date.plusDays(1)
    }
    return sb.toString()
}
