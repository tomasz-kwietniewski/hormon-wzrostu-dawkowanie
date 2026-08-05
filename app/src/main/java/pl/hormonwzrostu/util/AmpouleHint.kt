package pl.hormonwzrostu.util

import android.content.Context
import pl.hormonwzrostu.R
import pl.hormonwzrostu.data.AmpouleState
import pl.hormonwzrostu.data.formatMg

/**
 * Podpowiedź o stanie ampułki — ten sam tekst na karcie dnia, w dialogu edycji
 * i w powiadomieniu. Null, gdy zapas jest zwykły i nie ma czego komunikować.
 *
 * Podpowiedź nigdy nie zmienia proponowanej dawki: mówi tylko, ile w ampułce zostało.
 */
fun ampouleHint(context: Context, state: AmpouleState, remainingMg: Double): String? =
    when (state) {
        AmpouleState.NORMAL -> null
        AmpouleState.LAST_FULL -> context.getString(R.string.ampoule_last_full, formatMg(remainingMg))
        AmpouleState.REMNANT -> context.getString(R.string.ampoule_remnant, formatMg(remainingMg))
        AmpouleState.EMPTY -> context.getString(R.string.ampoule_empty)
    }
