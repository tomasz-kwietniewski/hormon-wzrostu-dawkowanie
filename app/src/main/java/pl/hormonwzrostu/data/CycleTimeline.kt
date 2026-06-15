package pl.hormonwzrostu.data

import java.time.LocalDate

/** Jedno faktyczne podanie dawki, z pozycją w cyklu wyliczoną z całej historii. */
data class DoseEvent(
    val date: LocalDate,
    val cycleNumber: Int,    // numer ampułki, 1-based
    val dayInCycle: Int,     // pozycja w ampułce, 1-based
    val plannedMg: Double,   // dawka wg planu dla tego slotu przy danym stanie ampułki
    val actualMg: Double,    // faktycznie podana (override lub = planned)
    val isLastInCycle: Boolean, // to podanie domknęło ampułkę
)

/** Stan następnej (jeszcze niepodanej) dawki — np. dzisiejszej. */
data class NextDose(
    val cycleNumber: Int,
    val dayInCycle: Int,
    val plannedMg: Double,
    val isLastInCycle: Boolean,
)

// Liczymy w tysięcznych mg na liczbach całkowitych, by uniknąć błędów zmiennoprzecinkowych.
private const val MG_SCALE = 1000.0

private fun toTh(mg: Double): Long = Math.round(mg * MG_SCALE)
private fun toMg(th: Long): Double = th / MG_SCALE

/** Wspólne dane wejściowe; null gdy schemat nie pozwala na przebieg. */
private data class Prep(val start: LocalDate, val ampouleTh: Long, val dailyTh: Long)

private fun prep(schedule: Schedule): Prep? {
    val start = schedule.startDate() ?: return null
    val ampouleTh = toTh(schedule.ampouleMg)
    val dailyTh = toTh(schedule.dailyDoseMg)
    if (ampouleTh <= 0L || dailyTh <= 0L || ampouleTh < dailyTh) return null
    return Prep(start, ampouleTh, dailyTh)
}

private fun sortedDates(intake: Set<String>, start: LocalDate): List<LocalDate> =
    intake.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        .filter { !it.isBefore(start) }
        .sorted()

private data class WalkState(val remainingTh: Long, val cycle: Int, val dayInCycle: Int)

/** Przechodzi po datach symulując zużycie ampułki; opcjonalnie zbiera zdarzenia. */
private fun walk(
    p: Prep,
    dates: List<LocalDate>,
    doses: Map<String, Double>,
    collect: ((DoseEvent) -> Unit)?,
): WalkState {
    var remaining = p.ampouleTh
    var cycle = 1
    var dayInCycle = 0
    for (date in dates) {
        if (remaining <= 0L) {
            cycle++; remaining = p.ampouleTh; dayInCycle = 0
        }
        dayInCycle++
        val plannedTh = if (remaining >= 2 * p.dailyTh) p.dailyTh else remaining
        val actualTh = doses[date.toString()]?.let { toTh(it) }?.takeIf { it > 0L } ?: plannedTh
        remaining -= actualTh
        collect?.invoke(
            DoseEvent(
                date = date,
                cycleNumber = cycle,
                dayInCycle = dayInCycle,
                plannedMg = toMg(plannedTh),
                actualMg = toMg(actualTh),
                isLastInCycle = remaining <= 0L,
            ),
        )
    }
    return WalkState(remaining, cycle, dayInCycle)
}

/** Pełna oś czasu podanych dawek z pozycją w cyklu i realnym zużyciem. */
fun buildTimeline(
    schedule: Schedule,
    intake: Set<String>,
    doses: Map<String, Double>,
): List<DoseEvent> {
    val p = prep(schedule) ?: return emptyList()
    val out = ArrayList<DoseEvent>()
    walk(p, sortedDates(intake, p.start), doses) { out.add(it) }
    return out
}

/**
 * Stan pierwszej jeszcze niepodanej dawki na dzień [onDate] (np. dziś).
 * Liczy stan po wszystkich podaniach ściśle wcześniejszych niż [onDate], po czym
 * projektuje jeden krok. Zwraca null, gdy brak daty startu lub [onDate] przed startem.
 */
fun nextDose(
    schedule: Schedule,
    intake: Set<String>,
    doses: Map<String, Double>,
    onDate: LocalDate,
): NextDose? {
    val p = prep(schedule) ?: return null
    if (onDate.isBefore(p.start)) return null
    val priorDates = sortedDates(intake, p.start).filter { it.isBefore(onDate) }
    val s = walk(p, priorDates, doses, null)

    var remaining = s.remainingTh
    var cycle = s.cycle
    var dayInCycle = s.dayInCycle
    if (remaining <= 0L) {
        cycle++; remaining = p.ampouleTh; dayInCycle = 0
    }
    dayInCycle++
    val plannedTh = if (remaining >= 2 * p.dailyTh) p.dailyTh else remaining
    return NextDose(
        cycleNumber = cycle,
        dayInCycle = dayInCycle,
        plannedMg = toMg(plannedTh),
        isLastInCycle = (remaining - plannedTh) <= 0L,
    )
}
