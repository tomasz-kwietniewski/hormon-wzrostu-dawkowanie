package pl.hormonwzrostu

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import pl.hormonwzrostu.data.Schedule
import pl.hormonwzrostu.data.ScheduleRepository
import pl.hormonwzrostu.notify.ReminderScheduler

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ScheduleRepository(app)

    var schedule by mutableStateOf(repository.load())
        private set

    /** Zapisuje schemat i przeplanowuje codzienne przypomnienie. */
    fun update(newSchedule: Schedule) {
        schedule = newSchedule
        repository.save(newSchedule)
        ReminderScheduler.reschedule(getApplication(), newSchedule)
    }
}
