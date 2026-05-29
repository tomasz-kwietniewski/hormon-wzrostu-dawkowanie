package pl.hormonwzrostu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.hormonwzrostu.R
import pl.hormonwzrostu.data.Schedule
import pl.hormonwzrostu.data.formatMg
import pl.hormonwzrostu.notify.isIgnoringBatteryOptimizations
import pl.hormonwzrostu.notify.requestIgnoreBatteryOptimizations
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    schedule: Schedule,
    givenToday: Boolean,
    onToggleGivenToday: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!schedule.isValid()) {
                NotConfiguredCard()
            } else {
                TodayDoseCard(schedule, givenToday, onToggleGivenToday)
                ScheduleSummaryCard(schedule)
            }

            BatteryReliabilityCard()

            if (schedule.isValid()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onOpenHistory, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.btn_history))
                    }
                    Button(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.btn_settings))
                    }
                }
            } else {
                Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.btn_configure))
                }
            }

            Text(
                text = stringResource(R.string.disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NotConfiguredCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.not_configured_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.not_configured_body), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TodayDoseCard(
    schedule: Schedule,
    givenToday: Boolean,
    onToggleGivenToday: (Boolean) -> Unit,
) {
    val today = LocalDate.now()
    val dayIndex = schedule.dayIndexInCycle(today)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.today_dose_title), style = MaterialTheme.typography.titleMedium)

            if (dayIndex == null) {
                Text(stringResource(R.string.cycle_not_started), style = MaterialTheme.typography.bodyMedium)
            } else {
                val dose = schedule.doseForDay(dayIndex)
                val isLast = schedule.isLastDayOfCycle(dayIndex)

                Text(
                    stringResource(R.string.mg_value, formatMg(dose)),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.day_of_cycle, schedule.childName, dayIndex + 1, schedule.daysPerCycle),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (isLast) {
                    Text(
                        stringResource(R.string.last_dose_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.padding(2.dp))
                if (givenToday) {
                    FilledTonalButton(onClick = { onToggleGivenToday(false) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.given_today_done))
                    }
                } else {
                    Button(
                        onClick = { onToggleGivenToday(true) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text(stringResource(R.string.btn_mark_given))
                    }
                }
                if (givenToday) {
                    Text(
                        stringResource(R.string.btn_unmark_given),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleSummaryCard(schedule: Schedule) {
    val time = "%02d:%02d".format(schedule.reminderHour, schedule.reminderMinute)
    val start = schedule.startDate()
        ?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) ?: "—"
    val enabledText =
        if (schedule.enabled) stringResource(R.string.state_enabled) else stringResource(R.string.state_disabled)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(schedule.medName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.padding(2.dp))
            InfoRow(stringResource(R.string.summary_reminder_at), time)
            InfoRow(
                stringResource(R.string.summary_scheme),
                stringResource(
                    R.string.summary_scheme_value,
                    formatMg(schedule.dailyDoseMg),
                    schedule.regularDays,
                    formatMg(schedule.lastDayDoseMg),
                ),
            )
            InfoRow(
                stringResource(R.string.summary_ampoule),
                stringResource(R.string.summary_ampoule_value, formatMg(schedule.ampouleMg), schedule.daysPerCycle),
            )
            InfoRow(stringResource(R.string.summary_start), start)
            InfoRow(stringResource(R.string.summary_notifications), enabledText)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Pokazuje się tylko, gdy aplikacja nie jest zwolniona z optymalizacji baterii. */
@Composable
private fun BatteryReliabilityCard() {
    val context = LocalContext.current
    var ignoring by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }

    if (ignoring) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.battery_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.battery_body), style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = {
                    requestIgnoreBatteryOptimizations(context)
                    ignoring = isIgnoringBatteryOptimizations(context)
                },
            ) {
                Text(stringResource(R.string.btn_allow))
            }
        }
    }
}
