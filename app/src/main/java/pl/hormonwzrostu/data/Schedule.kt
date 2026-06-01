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
 * podajemy pełną [dailyDoseMg] każdego dnia, a ostatniego dnia cyklu podajemy resztę
 * z ampułki. Długość cyklu [daysPerCycle] NIE jest wpisywana ręcznie — wynika
 * wyłącznie z pojemności i dawki dziennej (patrz [computeCycleDays]). Po [daysPerCycle]
 * dniach cykl startuje od nowa (kolejna ampułka).
 *
 * Przykłady (pojemność 10 mg) — liczone automatycznie:
 *  - 0,6 mg → 16 dni (15 × 0,6 + 1,0)
 *  - 0,7 mg → 14 dni (13 × 0,7 + 0,9)
 *  - 0,8 mg → 12 dni (11 × 0,8 + 1,2)
 *  - 0,5 mg → 20 dni (20 × 0,5; dzieli się równo)
 */
@Serializable
data class Schedule(
    val childName: String = "",
    val medName: String = "Omnitrope 10 mg (somatropina)",
    val ampouleMg: Double = 10.0,
    val dailyDoseMg: Double = 0.8,
    /** Data dnia 1. bieżącego cyklu w formacie ISO (yyyy-MM-dd). Pusta = nie ustawiono. */
    val startDateIso: String = "",
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val enabled: Boolean = true,
) {
    /** Długość cyklu (liczba dni z jednej ampułki) — wyliczana z pojemności i dawki dziennej. */
    val daysPerCycle: Int get() = computeCycleDays(ampouleMg, dailyDoseMg)

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

/**
 * Wyznacza długość cyklu (liczbę dni z jednej ampułki) automatycznie z pojemności
 * i dawki dziennej. Zasada: każdego dnia podajemy pełną [dailyMg]; dopóki w ampułce
 * zostają co najmniej dwie pełne dawki, dokładamy kolejny pełny dzień. Gdy zostaje
 * mniej niż dwie, a co najmniej jedna — to dzień ostatni i podajemy całą resztę.
 * Dzięki temu ostatnia dawka mieści się zawsze w przedziale ⟨dawka, 2 × dawka).
 *
 * Liczone w zaokrąglonych tysięcznych mg, by uniknąć błędów zmiennoprzecinkowych.
 * Zwraca 0 dla danych niepoprawnych (dawka ≤ 0 lub pojemność < dawki dziennej).
 */
fun computeCycleDays(ampouleMg: Double, dailyMg: Double): Int {
    if (dailyMg <= 0.0 || ampouleMg <= 0.0) return 0
    val ampoule = Math.round(ampouleMg * 1000.0)
    val daily = Math.round(dailyMg * 1000.0)
    if (daily <= 0L || ampoule < daily) return 0
    return (ampoule / daily).toInt()
}

/**
 * Formatuje miligramy bez zbędnych zer, z separatorem dziesiętnym zgodnym z bieżącym
 * językiem (np. PL: "0,8"; EN: "0.8"; 1.0 -> "1").
 */
fun formatMg(value: Double): String {
    val bd = BigDecimal(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros()
    val sep = java.text.DecimalFormatSymbols.getInstance(java.util.Locale.getDefault()).decimalSeparator
    return bd.toPlainString().replace('.', sep)
}
