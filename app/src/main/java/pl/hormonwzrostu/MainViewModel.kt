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

    /** Zapisuje schemat i przeplanowuje codzienne przypomnienie. */
    fun update(newSchedule: Schedule) {
        schedule = newSchedule
        repository.save(newSchedule)
        ReminderScheduler.reschedule(getApplication(), newSchedule)
    }

    fun isGiven(date: LocalDate): Boolean = intake.contains(date.toString())

    /** Oznacza/odznacza podanie leku danego dnia. */
    fun setGiven(date: LocalDate, given: Boolean) {
        val iso = date.toString()
        val updated = if (given) intake + iso else intake - iso
        intake = updated
        repository.saveIntake(updated)
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

    /** Ponowne wczytanie stanu z repozytorium (np. po imporcie kopii). */
    fun reload() {
        schedule = repository.load()
        intake = repository.loadIntake()
        comments = repository.loadComments()
    }
}
