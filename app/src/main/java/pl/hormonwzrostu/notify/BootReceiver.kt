package pl.hormonwzrostu.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import pl.hormonwzrostu.data.ScheduleRepository

/** Po restarcie telefonu odtwarza zaplanowany alarm (alarmy nie przeżywają restartu). */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val schedule = ScheduleRepository(context).load()
            ReminderScheduler.reschedule(context, schedule)
        }
    }
}
