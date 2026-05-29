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
}
