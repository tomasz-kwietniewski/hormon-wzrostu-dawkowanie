package pl.hormonwzrostu.data

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Schemat dawkowania jednego leku.
 *
 * Każdego dnia podajemy pełną [dailyDoseMg] — także wtedy, gdy w ampułce zostaje mniej.
 * [daysPerCycle] to wyłącznie **szacunek**, na ile dni starcza pojemność [ampouleMg]
 * (patrz [computeCycleDays]); służy za mianownik w „dzień 12/~12" i za opis schematu
 * w ustawieniach. Nie zamyka cyklu: nową ampułkę otwiera dopiero ręczne oznaczenie
 * użytkownika (re-kotwica w `CycleTimeline.walk`), bo dozownik bywa niedokładny
 * i realnie z ampułki idzie więcej dawek, niż wynika z pojemności.
 *
 * Szacunki (pojemność 10 mg):
 *  - 0,6 mg → ~16 dni
 *  - 0,7 mg → ~14 dni
 *  - 0,8 mg → ~12 dni (w praktyce 13, czasem 14)
 *  - 0,5 mg → ~20 dni (dzieli się równo)
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
    /** Szacowana długość cyklu — na ile dni starcza pojemność przy dawce dziennej. */
    val daysPerCycle: Int get() = computeCycleDays(ampouleMg, dailyDoseMg)

    /** Liczba dni ze standardową dawką (wszystkie poza ostatnim z pojemności). */
    val regularDays: Int get() = (daysPerCycle - 1).coerceAtLeast(0)

    /**
     * Reszta pojemności po [regularDays] pełnych dawkach — opis schematu w ustawieniach.
     * Aplikacja NIE proponuje jej jako dawki; to zawsze [dailyDoseMg].
     */
    val lastDayDoseMg: Double get() = ampouleMg - regularDays * dailyDoseMg

    fun startDate(): LocalDate? =
        startDateIso.takeIf { it.isNotBlank() }?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        }

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
