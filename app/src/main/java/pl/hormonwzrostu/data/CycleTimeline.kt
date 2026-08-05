package pl.hormonwzrostu.data

import java.time.LocalDate

/**
 * Stan ampułki *przed* daną dawką — czysta informacja dla UI i powiadomienia.
 * Nie steruje numeracją dni: nową ampułkę otwiera wyłącznie ręczne oznaczenie.
 */
enum class AmpouleState {
    /** Zapas na co najmniej dwie pełne dawki — zwykły dzień. */
    NORMAL,

    /** Ostatnia pełna dawka; resztę można dociągnąć i zamknąć ampułkę. */
    LAST_FULL,

    /** Została już tylko końcówka — mniej niż pełna dawka dzienna. */
    REMNANT,

    /** Teoretyczna zawartość wyczerpana; ampułka powinna być pusta. */
    EMPTY,
}

/** Jedno faktyczne podanie dawki, z pozycją w cyklu wyliczoną z całej historii. */
data class DoseEvent(
    val date: LocalDate,
    val cycleNumber: Int,    // numer ampułki, 1-based
    val dayInCycle: Int,     // pozycja w ampułce, 1-based
    val plannedMg: Double,   // dawka proponowana = zawsze dawka dzienna ze schematu
    val actualMg: Double,    // faktycznie podana (override lub = planned)
    val remainingBeforeMg: Double, // teoretyczna zawartość ampułki przed tym podaniem
    val ampouleState: AmpouleState,
)

/** Stan następnej (jeszcze niepodanej) dawki — np. dzisiejszej. */
data class NextDose(
    val cycleNumber: Int,
    val dayInCycle: Int,
    val plannedMg: Double,
    val remainingBeforeMg: Double,
    val ampouleState: AmpouleState,
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

/**
 * Czy [date] jest ręczną granicą nowej ampułki (re-kotwicą). Dzień startu schematu jest
 * niejawną pierwszą kotwicą — nie traktujemy go jako re-kotwicy, by nie zerować dnia 1.
 */
private fun isAmpouleAnchor(date: LocalDate, p: Prep, ampouleStarts: Set<String>): Boolean =
    date != p.start && ampouleStarts.contains(date.toString())

/** Stan ampułki przy zapasie [remainingTh] i dawce dziennej [dailyTh] (w tysięcznych mg). */
private fun stateOf(remainingTh: Long, dailyTh: Long): AmpouleState = when {
    remainingTh <= 0L -> AmpouleState.EMPTY
    remainingTh < dailyTh -> AmpouleState.REMNANT
    remainingTh < 2 * dailyTh -> AmpouleState.LAST_FULL
    else -> AmpouleState.NORMAL
}

/**
 * Przechodzi po datach symulując zużycie ampułki; opcjonalnie zbiera zdarzenia.
 *
 * Zapas może zejść poniżej zera i tam zostaje — dozownik bywa niedokładny, więc z ampułki
 * realnie idzie więcej niż wynika z pojemności. Zeruje go wyłącznie ręczna re-kotwica.
 */
private fun walk(
    p: Prep,
    dates: List<LocalDate>,
    doses: Map<String, Double>,
    ampouleStarts: Set<String>,
    collect: ((DoseEvent) -> Unit)?,
): WalkState {
    var remaining = p.ampouleTh
    var cycle = 1
    var dayInCycle = 0
    for (date in dates) {
        if (isAmpouleAnchor(date, p, ampouleStarts)) {
            cycle++; remaining = p.ampouleTh; dayInCycle = 0
        }
        dayInCycle++
        val before = remaining
        val actualTh = doses[date.toString()]?.let { toTh(it) }?.takeIf { it > 0L } ?: p.dailyTh
        remaining -= actualTh
        collect?.invoke(
            DoseEvent(
                date = date,
                cycleNumber = cycle,
                dayInCycle = dayInCycle,
                plannedMg = toMg(p.dailyTh),
                actualMg = toMg(actualTh),
                remainingBeforeMg = toMg(before),
                ampouleState = stateOf(before, p.dailyTh),
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
    ampouleStarts: Set<String> = emptySet(),
): List<DoseEvent> {
    val p = prep(schedule) ?: return emptyList()
    val out = ArrayList<DoseEvent>()
    walk(p, sortedDates(intake, p.start), doses, ampouleStarts) { out.add(it) }
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
    ampouleStarts: Set<String> = emptySet(),
): NextDose? {
    val p = prep(schedule) ?: return null
    if (onDate.isBefore(p.start)) return null
    val priorDates = sortedDates(intake, p.start).filter { it.isBefore(onDate) }
    val s = walk(p, priorDates, doses, ampouleStarts, null)

    var remaining = s.remainingTh
    var cycle = s.cycle
    var dayInCycle = s.dayInCycle
    // Re-kotwica na sam [onDate] też otwiera nową ampułkę przy projekcji.
    if (isAmpouleAnchor(onDate, p, ampouleStarts)) {
        cycle++; remaining = p.ampouleTh; dayInCycle = 0
    }
    dayInCycle++
    return NextDose(
        cycleNumber = cycle,
        dayInCycle = dayInCycle,
        plannedMg = toMg(p.dailyTh),
        remainingBeforeMg = toMg(remaining),
        ampouleState = stateOf(remaining, p.dailyTh),
    )
}
