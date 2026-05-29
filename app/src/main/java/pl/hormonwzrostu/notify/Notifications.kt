package pl.hormonwzrostu.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pl.hormonwzrostu.MainActivity
import pl.hormonwzrostu.R

const val CHANNEL_ID = "dose_reminders"
private const val NOTIFICATION_ID = 1

/** Tworzy kanał powiadomień (wymagany od Androida 8.0). Bezpieczne do wielokrotnego wywołania. */
fun ensureNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_ID,
        "Przypomnienia o dawce",
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = "Codzienne przypomnienie o dawce leku o ustalonej godzinie."
    }
    context.getSystemService(NotificationManager::class.java)
        .createNotificationChannel(channel)
}

fun hasNotificationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

fun showDoseNotification(context: Context, title: String, text: String) {
    ensureNotificationChannel(context)
    if (!hasNotificationPermission(context)) return

    val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val contentIntent = PendingIntent.getActivity(
        context,
        0,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_dose)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setAutoCancel(true)
        .setContentIntent(contentIntent)
        .build()

    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
}
