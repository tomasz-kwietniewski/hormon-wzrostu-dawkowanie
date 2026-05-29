package pl.hormonwzrostu.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import pl.hormonwzrostu.data.Schedule
import java.time.ZonedDateTime

/**
 * Planuje pojedynczy, dokładny alarm na najbliższą godzinę przypomnienia.
 * Po wystrzeleniu [ReminderReceiver] ustawia alarm ponownie na następny dzień.
 */
object ReminderScheduler {

    const val ACTION_SHOW_DOSE = "pl.hormonwzrostu.action.SHOW_DOSE"
    private const val REQUEST_CODE = 1001

    fun reschedule(context: Context, schedule: Schedule) {
        cancel(context)
        if (!schedule.enabled || !schedule.isValid()) return

        val triggerAtMillis = nextTriggerMillis(schedule.reminderHour, schedule.reminderMinute)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = buildPendingIntent(context)

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } else {
                // Brak zgody na dokładne alarmy — działamy mniej dokładnie, ale niezawodnie.
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(buildPendingIntent(context))
    }

    /** Najbliższy moment (epoch ms) o godzinie hour:minute; jeśli minął dziś — jutro. */
    fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = ZonedDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return next.toInstant().toEpochMilli()
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SHOW_DOSE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
