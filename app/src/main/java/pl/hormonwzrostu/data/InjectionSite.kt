package pl.hormonwzrostu.data

import java.time.LocalDate

/**
 * Miejsca wkłucia jako para (strona, region). Token przechowywany w danych to ASCII
 * „strona-region", np. „L-udo", „P-brzuch" — niezależny od języka; etykiety lokalizujemy w UI.
 *
 * Rotacja: stała kolejność 8 miejsc tak dobrana, by sąsiednie różniły się i stroną, i regionem,
 * a każda strona trafiła w każdy region. Podpowiedź na dany dzień = następne miejsce po
 * ostatnio użytym wcześniej; użytkownik może ją nadpisać.
 */
object InjectionSites {

    val SIDES = listOf("L", "P")
    val REGIONS = listOf("udo", "posladek", "ramie", "brzuch")

    /** Kolejność rotacji (8 miejsc). Sąsiednie różnią się stroną i regionem. */
    val ROTATION: List<String> = listOf(
        "L-udo", "P-ramie", "L-posladek", "P-brzuch",
        "L-ramie", "P-udo", "L-brzuch", "P-posladek",
    )

    fun isValid(token: String?): Boolean = token != null && ROTATION.contains(token)

    /** Następne miejsce w rotacji po [last]; gdy brak/nieznane — pierwsze z listy. */
    fun nextAfter(last: String?): String {
        val i = ROTATION.indexOf(last)
        return if (i < 0) ROTATION.first() else ROTATION[(i + 1) % ROTATION.size]
    }

    /** Najświeższe poprawne miejsce ściśle przed [onDate] (do wyznaczenia podpowiedzi). */
    private fun lastBefore(sites: Map<String, String>, onDate: LocalDate): String? =
        sites.entries
            .mapNotNull { (d, s) -> runCatching { LocalDate.parse(d) }.getOrNull()?.let { it to s } }
            .filter { it.first.isBefore(onDate) && isValid(it.second) }
            .maxByOrNull { it.first }
            ?.second

    /** Podpowiadane miejsce na [onDate]: następne w rotacji po ostatnim użytym wcześniej. */
    fun suggestedFor(sites: Map<String, String>, onDate: LocalDate): String =
        nextAfter(lastBefore(sites, onDate))
}
