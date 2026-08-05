package pl.hormonwzrostu.data

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
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

    /** Zbiór dat (ISO yyyy-MM-dd), w które oznaczono lek jako podany. */
    fun loadIntake(): Set<String> {
        val raw = prefs.getString(KEY_INTAKE, null) ?: return emptySet()
        return runCatching { json.decodeFromString<Set<String>>(raw) }.getOrDefault(emptySet())
    }

    fun saveIntake(dates: Set<String>) {
        prefs.edit().putString(KEY_INTAKE, json.encodeToString(dates)).apply()
    }

    /** Mapa data (ISO) -> komentarz do danego dnia. */
    fun loadComments(): Map<String, String> {
        val raw = prefs.getString(KEY_COMMENTS, null) ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, String>>(raw) }.getOrDefault(emptyMap())
    }

    fun saveComments(comments: Map<String, String>) {
        prefs.edit().putString(KEY_COMMENTS, json.encodeToString(comments)).apply()
    }

    /** Mapa data (ISO) -> faktycznie podana dawka (mg). Brak wpisu = dawka wg planu. */
    fun loadDoses(): Map<String, Double> {
        val raw = prefs.getString(KEY_DOSES, null) ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, Double>>(raw) }.getOrDefault(emptyMap())
    }

    fun saveDoses(doses: Map<String, Double>) {
        prefs.edit().putString(KEY_DOSES, json.encodeToString(doses)).apply()
    }

    /** Zbiór dat (ISO) jawnie oznaczonych jako „pominięto". */
    fun loadSkipped(): Set<String> {
        val raw = prefs.getString(KEY_SKIPPED, null) ?: return emptySet()
        return runCatching { json.decodeFromString<Set<String>>(raw) }.getOrDefault(emptySet())
    }

    fun saveSkipped(dates: Set<String>) {
        prefs.edit().putString(KEY_SKIPPED, json.encodeToString(dates)).apply()
    }

    /** Zbiór dat (ISO) otwarcia nowej ampułki — ręczne re-kotwice cyklu. */
    fun loadAmpouleStarts(): Set<String> {
        val raw = prefs.getString(KEY_AMPOULE_STARTS, null) ?: return emptySet()
        return runCatching { json.decodeFromString<Set<String>>(raw) }.getOrDefault(emptySet())
    }

    fun saveAmpouleStarts(dates: Set<String>) {
        prefs.edit().putString(KEY_AMPOULE_STARTS, json.encodeToString(dates)).apply()
    }

    /** Mapa data (ISO) -> token miejsca wkłucia (np. „L-udo"). */
    fun loadSites(): Map<String, String> {
        val raw = prefs.getString(KEY_SITES, null) ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, String>>(raw) }.getOrDefault(emptyMap())
    }

    fun saveSites(sites: Map<String, String>) {
        prefs.edit().putString(KEY_SITES, json.encodeToString(sites)).apply()
    }

    /**
     * Utrwala granice ampułek, które stara logika wyznaczała sama (przy wyczerpaniu
     * zapasu), tak by nowa reguła „nową ampułkę otwiera wyłącznie ręczne oznaczenie"
     * nie przenumerowała historii wstecz. Wykonuje się raz — patrz [legacyAutoAnchors].
     */
    fun migrateAmpouleAnchorsIfNeeded() {
        if (prefs.getBoolean(KEY_ANCHOR_MIGRATION, false)) return
        val existing = loadAmpouleStarts()
        val added = legacyAutoAnchors(load(), loadIntake(), loadDoses(), existing)
        if (added.isNotEmpty()) saveAmpouleStarts(existing + added)
        prefs.edit().putBoolean(KEY_ANCHOR_MIGRATION, true).apply()
    }

    /** Wybrany język UI: "" = systemowy, "pl", "en". */
    fun loadLang(): String = prefs.getString(KEY_LANG, "") ?: ""

    fun saveLang(tag: String) {
        prefs.edit().putString(KEY_LANG, tag).apply()
    }

    /** Pełna kopia wszystkich danych jako JSON. */
    fun exportBackup(): String =
        json.encodeToString(
            Backup(
                version = ANCHOR_MODEL_VERSION,
                schedule = load(),
                intake = loadIntake(),
                comments = loadComments(),
                lang = loadLang(),
                doses = loadDoses(),
                skipped = loadSkipped(),
                ampouleStarts = loadAmpouleStarts(),
                sites = loadSites(),
            ),
        )

    /** Wczytuje kopię z JSON; zwraca true przy powodzeniu. */
    fun importBackup(text: String): Boolean = runCatching {
        val backup = json.decodeFromString<Backup>(text)
        save(backup.schedule)
        saveIntake(backup.intake)
        saveComments(backup.comments)
        saveLang(backup.lang)
        saveDoses(backup.doses)
        saveSkipped(backup.skipped)
        saveAmpouleStarts(backup.ampouleStarts)
        saveSites(backup.sites)
        // Kopie sprzed wersji 5 pochodzą z modelu z auto-przeskokiem cyklu — ich granice
        // ampułek trzeba utrwalić tak samo jak przy aktualizacji aplikacji.
        if (backup.version < ANCHOR_MODEL_VERSION) {
            prefs.edit().putBoolean(KEY_ANCHOR_MIGRATION, false).apply()
            migrateAmpouleAnchorsIfNeeded()
        }
    }.isSuccess

    companion object {
        private const val PREFS = "hormon_prefs"
        private const val KEY_SCHEDULE = "schedule"
        private const val KEY_INTAKE = "intake_dates"
        private const val KEY_COMMENTS = "intake_comments"
        private const val KEY_LANG = "ui_lang"
        private const val KEY_DOSES = "intake_doses"
        private const val KEY_SKIPPED = "intake_skipped"
        private const val KEY_AMPOULE_STARTS = "ampoule_starts"
        private const val KEY_ANCHOR_MIGRATION = "ampoule_anchor_migration_v2"

        /** Pierwsza wersja kopii zapisana już w modelu bez auto-przeskoku cyklu. */
        private const val ANCHOR_MODEL_VERSION = 5
        private const val KEY_SITES = "intake_sites"
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
