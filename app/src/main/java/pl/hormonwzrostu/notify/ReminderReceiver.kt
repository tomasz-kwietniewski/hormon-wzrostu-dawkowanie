package pl.hormonwzrostu.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import pl.hormonwzrostu.R
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

                val title = context.getString(
                    R.string.notif_title,
                    schedule.childName,
                    formatMg(dose),
                )
                val medLine = schedule.medName +
                    if (isLast) context.getString(R.string.notif_last_suffix) else ""
                val text = context.getString(
                    R.string.notif_text,
                    dayNumber,
                    schedule.daysPerCycle,
                    medLine,
                )
                showDoseNotification(context, title, text)
            }
        }

        // Zaplanuj kolejne przypomnienie (na następny dzień), niezależnie od powyższego.
        ReminderScheduler.reschedule(context, schedule)
    }
}
