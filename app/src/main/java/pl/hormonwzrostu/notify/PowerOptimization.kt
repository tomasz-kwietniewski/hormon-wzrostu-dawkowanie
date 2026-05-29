package pl.hormonwzrostu.notify

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/** Czy aplikacja jest już zwolniona z optymalizacji baterii. */
fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(PowerManager::class.java)
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

/**
 * Pokazuje systemowe okno z prośbą o zwolnienie aplikacji z optymalizacji baterii
 * (jedno tapnięcie zamiast szukania w Ustawieniach). Gdyby okno było niedostępne,
 * otwiera ogólny ekran ustawień baterii aplikacji.
 */
@SuppressLint("BatteryLife")
fun requestIgnoreBatteryOptimizations(context: Context) {
    val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    runCatching { context.startActivity(direct) }
        .recoverCatching { context.startActivity(fallback) }
}
