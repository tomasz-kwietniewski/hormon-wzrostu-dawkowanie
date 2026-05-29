package pl.hormonwzrostu

import android.app.Application
import pl.hormonwzrostu.notify.ensureNotificationChannel

class HormonApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel(this)
    }
}
