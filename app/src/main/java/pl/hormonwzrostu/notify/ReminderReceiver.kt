package pl.hormonwzrostu.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import pl.hormonwzrostu.data.ScheduleRepository
import pl.hormonwzrostu.data.formatMg
import java.time.LocalDate

/** Odbiera dzienny alarm: pokazuje powiadomienie o dawce i planuje kolejny dzień. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val schedule = ScheduleRepository(context).load()

        if (schedule.enabled && schedule.isValid()) {
            val today = LocalDate.now()
            val dayIndex = schedule.dayIndexInCycle(today)
            if (dayIndex != null) {
                val dose = schedule.doseForDay(dayIndex)
                val dayNumber = dayIndex + 1
                val isLast = schedule.isLastDayOfCycle(dayIndex)

                val title = "${schedule.childName} — dziś ${formatMg(dose)} mg"
                val text = buildString {
                    append("Dzień $dayNumber/${schedule.daysPerCycle} cyklu. ")
                    append(schedule.medName)
                    if (isLast) {
                        append("\n⚠ Ostatnia dawka z ampułki — jutro otwórz nową ampułkę.")
                    }
                }
                showDoseNotification(context, title, text)
            }
        }

        // Zaplanuj kolejne przypomnienie (na następny dzień), niezależnie od powyższego.
        ReminderScheduler.reschedule(context, schedule)
    }
}
