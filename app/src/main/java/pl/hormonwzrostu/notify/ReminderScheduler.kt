package pl.hormonwzrostu.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import pl.hormonwzrostu.MainActivity
import pl.hormonwzrostu.data.Schedule
import java.time.ZonedDateTime

/**
 * Planuje pojedynczy alarm typu „budzik" na najbliższą godzinę przypomnienia.
 * Po wystrzeleniu [ReminderReceiver] ustawia alarm ponownie na następny dzień.
 *
 * Używamy [AlarmManager.setAlarmClock], bo alarmy budzika są zwolnione z trybu Doze
 * i z oszczędzania baterii — odpalają punktualnie nawet na agresywnych nakładkach
 * (OnePlus, Samsung) bez konieczności zmiany ustawień przez użytkownika. Dodatkowo
 * nie wymagają uprawnienia SCHEDULE_EXACT_ALARM.
 */
object ReminderScheduler {

    const val ACTION_SHOW_DOSE = "pl.hormonwzrostu.action.SHOW_DOSE"
    private const val REQUEST_CODE_ALARM = 1001
    private const val REQUEST_CODE_SHOW = 1002

    fun reschedule(context: Context, schedule: Schedule) {
        cancel(context)
        if (!schedule.enabled || !schedule.isValid()) return

        val triggerAtMillis = nextTriggerMillis(schedule.reminderHour, schedule.reminderMinute)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val operation = buildAlarmIntent(context)

        // setAlarmClock daje najwyższy priorytet i odporność na Doze, ale na części
        // urządzeń wymaga uprawnienia do dokładnych alarmów. Gdyby go zabrakło,
        // łapiemy wyjątek i planujemy alarm mniej dokładny — aplikacja nigdy się nie wywali.
        try {
            val alarmInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, buildShowIntent(context))
            alarmManager.setAlarmClock(alarmInfo, operation)
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(buildAlarmIntent(context))
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

    /** Wystrzeliwany o czasie — broadcast do [ReminderReceiver] pokazujący powiadomienie. */
    private fun buildAlarmIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SHOW_DOSE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Otwierany po tapnięciu w informację o alarmie (np. na ekranie blokady). */
    private fun buildShowIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_SHOW,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
