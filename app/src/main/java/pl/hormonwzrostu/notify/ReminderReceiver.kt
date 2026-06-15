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
            val repo = ScheduleRepository(context)
            val next = pl.hormonwzrostu.data.nextDose(schedule, repo.loadIntake(), repo.loadDoses(), today)
            if (next != null) {
                val title = context.getString(
                    R.string.notif_title,
                    schedule.childName,
                    formatMg(next.plannedMg),
                )
                val medLine = schedule.medName +
                    if (next.isLastInCycle) context.getString(R.string.notif_last_suffix) else ""
                val text = context.getString(
                    R.string.notif_text,
                    next.dayInCycle,
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
