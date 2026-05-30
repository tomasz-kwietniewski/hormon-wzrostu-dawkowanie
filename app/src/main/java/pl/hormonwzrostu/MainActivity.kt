package pl.hormonwzrostu

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.hormonwzrostu.data.ScheduleRepository
import pl.hormonwzrostu.notify.ReminderScheduler
import pl.hormonwzrostu.ui.HistoryScreen
import pl.hormonwzrostu.ui.HormonTheme
import pl.hormonwzrostu.ui.MainScreen
import pl.hormonwzrostu.ui.SettingsScreen
import pl.hormonwzrostu.util.wrapLocale
import java.time.LocalDate

private enum class Screen { MAIN, SETTINGS, HISTORY }

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* obsłużone w UI */ }

    override fun attachBaseContext(newBase: Context) {
        val tag = ScheduleRepository(newBase).loadLang()
        super.attachBaseContext(wrapLocale(newBase, tag))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Uzbrojenie alarmu przy każdym otwarciu (idempotentne) — gwarantuje,
        // że przypomnienie jest zaplanowane także po reinstalacji aplikacji.
        ReminderScheduler.reschedule(this, ScheduleRepository(this).load())

        val activity = this
        val repo = ScheduleRepository(this)

        setContent {
            HormonTheme {
                val vm: MainViewModel = viewModel()
                var screen by remember { mutableStateOf(Screen.MAIN) }

                when (screen) {
                    Screen.MAIN -> MainScreen(
                        schedule = vm.schedule,
                        givenToday = vm.isGiven(LocalDate.now()),
                        onToggleGivenToday = { vm.setGiven(LocalDate.now(), it) },
                        onOpenSettings = { screen = Screen.SETTINGS },
                        onOpenHistory = { screen = Screen.HISTORY },
                    )

                    Screen.SETTINGS -> SettingsScreen(
                        initial = vm.schedule,
                        currentLang = repo.loadLang(),
                        onSelectLang = { tag ->
                            repo.saveLang(tag)
                            activity.recreate()
                        },
                        onExportBackup = { repo.exportBackup() },
                        onImportBackup = { text ->
                            val ok = repo.importBackup(text)
                            if (ok) {
                                vm.reload()
                                activity.recreate()
                            }
                            ok
                        },
                        onSave = {
                            vm.update(it)
                            screen = Screen.MAIN
                        },
                        onCancel = { screen = Screen.MAIN },
                    )

                    Screen.HISTORY -> HistoryScreen(
                        schedule = vm.schedule,
                        intake = vm.intake,
                        comments = vm.comments,
                        onToggleDay = { date, given -> vm.setGiven(date, given) },
                        onSetComment = { date, text -> vm.setComment(date, text) },
                        onBack = { screen = Screen.MAIN },
                    )
                }
            }
        }
    }
}
