package pl.hormonwzrostu.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import pl.hormonwzrostu.R
import pl.hormonwzrostu.data.ScheduleRepository
import pl.hormonwzrostu.data.formatMg
import pl.hormonwzrostu.util.ampouleHint
import java.time.LocalDate

/** Odbiera dzienny alarm: pokazuje powiadomienie o dawce i planuje kolejny dzień. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repo = ScheduleRepository(context)
        // Powiadomienie może wyprzedzić pierwsze otwarcie aplikacji po aktualizacji.
        repo.migrateAmpouleAnchorsIfNeeded()
        val schedule = repo.load()

        if (schedule.enabled && schedule.isValid()) {
            val today = LocalDate.now()
            val next = pl.hormonwzrostu.data.nextDose(
                schedule, repo.loadIntake(), repo.loadDoses(), today, repo.loadAmpouleStarts(),
            )
            if (next != null) {
                val title = context.getString(
                    R.string.notif_title,
                    schedule.childName,
                    formatMg(next.plannedMg),
                )
                val hint = ampouleHint(context, next.ampouleState, next.remainingBeforeMg)
                val medLine = schedule.medName + (hint?.let { "\n" + it } ?: "")
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
