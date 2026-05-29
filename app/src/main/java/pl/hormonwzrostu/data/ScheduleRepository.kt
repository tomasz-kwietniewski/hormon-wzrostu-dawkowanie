package pl.hormonwzrostu.data

import android.content.Context
import kotlinx.serialization.json.Json

/** Trwały zapis schematu w SharedPreferences (jako JSON). */
class ScheduleRepository(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): Schedule {
        val raw = prefs.getString(KEY_SCHEDULE, null) ?: return Schedule()
        return runCatching { json.decodeFromString<Schedule>(raw) }.getOrDefault(Schedule())
    }

    fun save(schedule: Schedule) {
        prefs.edit().putString(KEY_SCHEDULE, json.encodeToString(schedule)).apply()
    }

    companion object {
        private const val PREFS = "hormon_prefs"
        private const val KEY_SCHEDULE = "schedule"
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
