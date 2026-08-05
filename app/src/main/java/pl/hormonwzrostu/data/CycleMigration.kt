package pl.hormonwzrostu.data

import java.time.LocalDate

/**
 * Jednorazowa migracja danych sprzed reguły „nową ampułkę otwiera wyłącznie ręczne
 * oznaczenie".
 *
 * Stary model zaczynał nowy cykl także wtedy, gdy symulowana zawartość ampułki spadła do
 * zera. Część granic ampułek w zapisanej historii pochodzi więc z takiego auto-przeskoku,
 * a nie z kliknięcia użytkownika — bez utrwalenia ich jako re-kotwic nowa logika
 * przenumerowałaby całą historię wstecz.
 *
 * Zwraca daty do dopisania do zbioru re-kotwic. Pomija auto-przeskok, po którym najbliższe
 * kolejne podanie jest już ręczną re-kotwicą: taki dzień to w rzeczywistości dociągnięta
 * końcówka poprzedniej ampułki (użytkownik oznaczał nową dopiero nazajutrz), więc ma
 * zostać ostatnim dniem starego cyklu, a nie pierwszym dniem nowego.
 */
fun legacyAutoAnchors(
    schedule: Schedule,
    intake: Set<String>,
    doses: Map<String, Double>,
    ampouleStarts: Set<String>,
): Set<String> {
    val start = schedule.startDate() ?: return emptySet()
    val ampoule = Math.round(schedule.ampouleMg * 1000.0)
    val daily = Math.round(schedule.dailyDoseMg * 1000.0)
    if (ampoule <= 0L || daily <= 0L || ampoule < daily) return emptySet()

    val dates = intake.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        .filter { !it.isBefore(start) }
        .sorted()

    // Odtworzenie starego przebiegu: dawka wg planu to reszta ampułki, gdy nie starcza już
    // na dwie pełne, a wyczerpanie zapasu samo otwierało kolejny cykl.
    val autoStarts = ArrayList<Int>()
    var remaining = ampoule
    for ((i, date) in dates.withIndex()) {
        val manual = date != start && ampouleStarts.contains(date.toString())
        if (remaining <= 0L || manual) {
            if (!manual) autoStarts.add(i)
            remaining = ampoule
        }
        val planned = if (remaining >= 2 * daily) daily else remaining
        val actual = doses[date.toString()]
            ?.let { Math.round(it * 1000.0) }
            ?.takeIf { it > 0L }
            ?: planned
        remaining -= actual
    }

    return autoStarts
        .filter { i ->
            // Ostatnie podanie w historii nie ma następnika — zachowujemy je zachowawczo,
            // żeby aktualizacja nie przesunęła granicy, którą użytkownik już widział.
            val next = dates.getOrNull(i + 1) ?: return@filter true
            !ampouleStarts.contains(next.toString())
        }
        .map { dates[it].toString() }
        .toSet()
}
