package pl.hormonwzrostu.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/**
 * Zwraca kontekst z wymuszonym językiem UI.
 * Pusty tag = język systemowy. Ustawia też domyślny Locale procesu
 * (potrzebny m.in. do formatowania liczb i dat).
 */
fun wrapLocale(context: Context, tag: String): Context {
    val locale = if (tag.isEmpty()) {
        Resources.getSystem().configuration.locales[0]
    } else {
        Locale.forLanguageTag(tag)
    }
    Locale.setDefault(locale)
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    return context.createConfigurationContext(config)
}
