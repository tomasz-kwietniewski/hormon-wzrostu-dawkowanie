package pl.hormonwzrostu

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import pl.hormonwzrostu.data.Schedule
import pl.hormonwzrostu.data.ScheduleRepository
import pl.hormonwzrostu.notify.ReminderScheduler
import java.time.LocalDate

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ScheduleRepository(app)

    var schedule by mutableStateOf(repository.load())
        private set

    /** Zbiór dat (ISO) oznaczonych jako „podano". */
    var intake by mutableStateOf(repository.loadIntake())
        private set

    /** Mapa data (ISO) -> komentarz. */
    var comments by mutableStateOf(repository.loadComments())
        private set

    /** Mapa data (ISO) -> faktycznie podana dawka (mg). */
    var doses by mutableStateOf(repository.loadDoses())
        private set

    /** Zbiór dat (ISO) jawnie oznaczonych jako „pominięto". */
    var skipped by mutableStateOf(repository.loadSkipped())
        private set

    /** Zbiór dat (ISO) otwarcia nowej ampułki — ręczne re-kotwice cyklu. */
    var ampouleStarts by mutableStateOf(repository.loadAmpouleStarts())
        private set

    /** Mapa data (ISO) -> token miejsca wkłucia (np. „L-udo"). */
    var sites by mutableStateOf(repository.loadSites())
        private set

    /** Zapisuje schemat i przeplanowuje codzienne przypomnienie. */
    fun update(newSchedule: Schedule) {
        schedule = newSchedule
        repository.save(newSchedule)
        ReminderScheduler.reschedule(getApplication(), newSchedule)
    }

    fun isGiven(date: LocalDate): Boolean = intake.contains(date.toString())

    /** Oznacza/odznacza podanie leku danego dnia. Podanie wyklucza jawne pominięcie. */
    fun setGiven(date: LocalDate, given: Boolean) {
        val iso = date.toString()
        val updated = if (given) intake + iso else intake - iso
        intake = updated
        repository.saveIntake(updated)
        if (given && skipped.contains(iso)) {
            val s = skipped - iso
            skipped = s
            repository.saveSkipped(s)
        }
    }

    /** Jawnie oznacza/odznacza pominięcie dnia. Pominięcie wyklucza podanie. */
    fun setSkipped(date: LocalDate, skip: Boolean) {
        val iso = date.toString()
        val s = if (skip) skipped + iso else skipped - iso
        skipped = s
        repository.saveSkipped(s)
        if (skip && intake.contains(iso)) {
            val i = intake - iso
            intake = i
            repository.saveIntake(i)
        }
    }

    /** Ustawia/zdejmuje ręczną re-kotwicę „nowa ampułka od tego dnia". */
    fun setAmpouleStart(date: LocalDate, anchor: Boolean) {
        val iso = date.toString()
        val a = if (anchor) ampouleStarts + iso else ampouleStarts - iso
        ampouleStarts = a
        repository.saveAmpouleStarts(a)
    }

    /** Ustawia miejsce wkłucia dnia (null/pusty = usuwa). */
    fun setSite(date: LocalDate, token: String?) {
        val iso = date.toString()
        val updated = if (token.isNullOrBlank()) sites - iso else sites + (iso to token)
        sites = updated
        repository.saveSites(updated)
    }

    /** Zapisuje komentarz do dnia (pusty = usuwa). */
    fun setComment(date: LocalDate, text: String) {
        val iso = date.toString()
        val trimmed = text.trim()
        val updated = if (trimmed.isEmpty()) comments - iso else comments + (iso to trimmed)
        comments = updated
        repository.saveComments(updated)
    }

    fun commentFor(date: LocalDate): String = comments[date.toString()] ?: ""

    /** Zapisuje faktycznie podaną dawkę dnia; null/≤0 = usuwa override (dawka wg planu). */
    fun setActualDose(date: LocalDate, mg: Double?) {
        val iso = date.toString()
        val updated = if (mg == null || mg <= 0.0) doses - iso else doses + (iso to mg)
        doses = updated
        repository.saveDoses(updated)
    }

    fun actualDoseFor(date: LocalDate): Double? = doses[date.toString()]

    /** Ponowne wczytanie stanu z repozytorium (np. po imporcie kopii). */
    fun reload() {
        schedule = repository.load()
        intake = repository.loadIntake()
        comments = repository.loadComments()
        doses = repository.loadDoses()
        skipped = repository.loadSkipped()
        ampouleStarts = repository.loadAmpouleStarts()
        sites = repository.loadSites()
    }
}
