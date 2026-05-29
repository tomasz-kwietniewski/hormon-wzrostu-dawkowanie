package pl.hormonwzrostu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.hormonwzrostu.data.Schedule
import pl.hormonwzrostu.data.formatMg
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    schedule: Schedule,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Hormon Wzrostu Dawkowanie") }) },
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
                NotConfiguredCard(onOpenSettings)
            } else {
                TodayDoseCard(schedule)
                ScheduleSummaryCard(schedule)
            }

            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (schedule.isValid()) "Ustawienia / zmiana dawkowania" else "Skonfiguruj")
            }

            Text(
                text = "To narzędzie tylko przypomina o dawce. Nie zastępuje ulotki leku ani " +
                    "zaleceń lekarza prowadzącego. W razie wątpliwości skontaktuj się z lekarzem.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NotConfiguredCard(onOpenSettings: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Brak konfiguracji", style = MaterialTheme.typography.titleLarge)
            Text(
                "Uzupełnij imię dziecka, lek, dawkę dzienną, liczbę dni cyklu i datę startu, " +
                    "aby zacząć dostawać codzienne przypomnienia.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TodayDoseCard(schedule: Schedule) {
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
            Text("Dzisiejsza dawka", style = MaterialTheme.typography.titleMedium)

            if (dayIndex == null) {
                Text(
                    "Cykl jeszcze się nie rozpoczął (data startu w przyszłości).",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                val dose = schedule.doseForDay(dayIndex)
                val dayNumber = dayIndex + 1
                val isLast = schedule.isLastDayOfCycle(dayIndex)

                Text(
                    "${formatMg(dose)} mg",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${schedule.childName} • dzień $dayNumber/${schedule.daysPerCycle}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (isLast) {
                    Text(
                        "⚠ Ostatnia dawka z ampułki — jutro otwórz nową.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleSummaryCard(schedule: Schedule) {
    val time = "%02d:%02d".format(schedule.reminderHour, schedule.reminderMinute)
    val start = schedule.startDate()?.format(DateTimeFormatter.ofPattern("d MMMM yyyy")) ?: "—"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(schedule.medName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.padding(2.dp))
            InfoRow("Przypomnienie codziennie o", time)
            InfoRow(
                "Schemat",
                "${formatMg(schedule.dailyDoseMg)} mg × ${schedule.regularDays} dni " +
                    "+ ${formatMg(schedule.lastDayDoseMg)} mg",
            )
            InfoRow("Ampułka", "${formatMg(schedule.ampouleMg)} mg na ${schedule.daysPerCycle} dni")
            InfoRow("Start cyklu", start)
            InfoRow("Powiadomienia", if (schedule.enabled) "włączone" else "wyłączone")
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
