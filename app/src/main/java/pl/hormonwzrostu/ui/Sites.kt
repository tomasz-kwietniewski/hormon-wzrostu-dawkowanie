package pl.hormonwzrostu.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pl.hormonwzrostu.R
import pl.hormonwzrostu.data.InjectionSites

/** Identyfikator zasobu etykiety dla tokenu miejsca (np. „L-udo"). */
private fun siteLabelRes(token: String): Int = when (token) {
    "L-udo" -> R.string.site_l_udo
    "P-udo" -> R.string.site_p_udo
    "L-posladek" -> R.string.site_l_posladek
    "P-posladek" -> R.string.site_p_posladek
    "L-ramie" -> R.string.site_l_ramie
    "P-ramie" -> R.string.site_p_ramie
    "L-brzuch" -> R.string.site_l_brzuch
    else -> R.string.site_p_brzuch
}

/** Zlokalizowana etykieta miejsca wkłucia. */
@Composable
fun siteLabel(token: String): String = stringResource(siteLabelRes(token))

/** Mapa token -> etykieta dla wszystkich miejsc (do eksportu). */
@Composable
fun siteLabelsMap(): Map<String, String> =
    InjectionSites.ROTATION.associateWith { stringResource(siteLabelRes(it)) }
