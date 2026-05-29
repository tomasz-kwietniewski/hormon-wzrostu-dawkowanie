package pl.hormonwzrostu.data

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Schemat dawkowania jednego leku.
 *
 * Model „auto-liczenia ostatniego dnia": z jednej ampułki o pojemności [ampouleMg]
 * podajemy [dailyDoseMg] przez (daysPerCycle - 1) dni, a ostatniego dnia cyklu
 * podajemy resztę, jaka została w ampułce. Po [daysPerCycle] dniach cykl startuje
 * od nowa (kolejna ampułka).
 *
 * Przykłady ze skierowań (suma zawsze = 10 mg):
 *  - 0,6 mg × 15 dni + 1,0 mg  (16 dni)
 *  - 0,7 mg × 13 dni + 0,9 mg  (14 dni)
 *  - 0,8 mg × 11 dni + 1,2 mg  (12 dni)
 */
@Serializable
data class Schedule(
    val childName: String = "",
    val medName: String = "Omnitrope 10 mg (somatropina)",
    val ampouleMg: Double = 10.0,
    val dailyDoseMg: Double = 0.8,
    val daysPerCycle: Int = 12,
    /** Data dnia 1. bieżącego cyklu w formacie ISO (yyyy-MM-dd). Pusta = nie ustawiono. */
    val startDateIso: String = "",
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val enabled: Boolean = true,
) {
    /** Liczba dni ze standardową dawką (wszystkie poza ostatnim). */
    val regularDays: Int get() = (daysPerCycle - 1).coerceAtLeast(0)

    /** Dawka ostatniego dnia = reszta z ampułki. */
    val lastDayDoseMg: Double get() = ampouleMg - regularDays * dailyDoseMg

    fun startDate(): LocalDate? =
        startDateIso.takeIf { it.isNotBlank() }?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        }

    /**
     * Indeks dnia w cyklu (0-based) dla podanej daty.
     * Zwraca null, gdy brak daty startu lub data jest przed startem.
     */
    fun dayIndexInCycle(date: LocalDate): Int? {
        val start = startDate() ?: return null
        val diff = ChronoUnit.DAYS.between(start, date)
        if (diff < 0 || daysPerCycle < 1) return null
        return (diff % daysPerCycle).toInt()
    }

    /** Numer cyklu (=numer ampułki) liczony od 1 dla podanej daty. */
    fun cycleNumber(date: LocalDate): Int? {
        val start = startDate() ?: return null
        val diff = ChronoUnit.DAYS.between(start, date)
        if (diff < 0 || daysPerCycle < 1) return null
        return (diff / daysPerCycle).toInt() + 1
    }

    fun doseForDay(dayIndexInCycle: Int): Double =
        if (dayIndexInCycle >= regularDays) lastDayDoseMg else dailyDoseMg

    fun isLastDayOfCycle(dayIndexInCycle: Int): Boolean = dayIndexInCycle >= regularDays

    /** Czy schemat jest na tyle kompletny, by planować powiadomienia. */
    fun isValid(): Boolean =
        childName.isNotBlank() &&
            daysPerCycle >= 1 &&
            dailyDoseMg > 0 &&
            ampouleMg > 0 &&
            lastDayDoseMg > 0 &&
            startDate() != null &&
            reminderHour in 0..23 &&
            reminderMinute in 0..59
}

/** Formatuje miligramy po polsku, bez zbędnych zer (np. 0.8 -> "0,8"; 1.0 -> "1"). */
fun formatMg(value: Double): String {
    val bd = BigDecimal(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros()
    return bd.toPlainString().replace('.', ',')
}
