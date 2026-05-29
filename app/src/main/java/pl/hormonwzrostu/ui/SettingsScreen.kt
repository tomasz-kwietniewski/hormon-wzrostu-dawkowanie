package pl.hormonwzrostu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import pl.hormonwzrostu.data.Schedule
import pl.hormonwzrostu.data.formatMg
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initial: Schedule,
    onSave: (Schedule) -> Unit,
    onCancel: () -> Unit,
) {
    var childName by remember { mutableStateOf(initial.childName) }
    var medName by remember { mutableStateOf(initial.medName) }
    var ampoule by remember { mutableStateOf(formatMg(initial.ampouleMg)) }
    var dailyDose by remember { mutableStateOf(formatMg(initial.dailyDoseMg)) }
    var days by remember { mutableStateOf(initial.daysPerCycle.toString()) }
    var startDate by remember {
        mutableStateOf(initial.startDate() ?: LocalDate.now())
    }
    var hour by remember { mutableStateOf(initial.reminderHour) }
    var minute by remember { mutableStateOf(initial.reminderMinute) }
    var enabled by remember { mutableStateOf(initial.enabled) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val ampouleVal = ampoule.toDoubleOrNullPl()
    val dailyVal = dailyDose.toDoubleOrNullPl()
    val daysVal = days.toIntOrNull()
    val lastDose = if (ampouleVal != null && dailyVal != null && daysVal != null && daysVal >= 1) {
        ampouleVal - (daysVal - 1) * dailyVal
    } else {
        null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ustawienia dawkowania") },
                navigationIcon = {
                    TextButton(onClick = onCancel) { Text("Anuluj") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = childName,
                onValueChange = { childName = it },
                label = { Text("Imię dziecka") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = medName,
                onValueChange = { medName = it },
                label = { Text("Nazwa leku") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ampoule,
                onValueChange = { ampoule = it },
                label = { Text("Pojemność ampułki (mg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = dailyDose,
                onValueChange = { dailyDose = it },
                label = { Text("Dawka dzienna (mg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = days,
                onValueChange = { days = it },
                label = { Text("Liczba dni cyklu (na 1 ampułkę)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Wyliczona ostatnia dawka", style = MaterialTheme.typography.labelLarge)
                    if (lastDose != null && lastDose > 0) {
                        Text(
                            "${formatMg(dailyVal!!)} mg × ${daysVal!! - 1} dni " +
                                "+ ${formatMg(lastDose)} mg (dzień $daysVal)",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    } else {
                        Text(
                            "Uzupełnij pojemność, dawkę i liczbę dni.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Dzień startu cyklu: " + startDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy")))
            }
            OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Godzina przypomnienia: %02d:%02d".format(hour, minute))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Powiadomienia włączone", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = {
                    val problem = validate(childName, ampouleVal, dailyVal, daysVal, lastDose)
                    if (problem != null) {
                        error = problem
                    } else {
                        onSave(
                            Schedule(
                                childName = childName.trim(),
                                medName = medName.trim().ifBlank { "Lek" },
                                ampouleMg = ampouleVal!!,
                                dailyDoseMg = dailyVal!!,
                                daysPerCycle = daysVal!!,
                                startDateIso = startDate.toString(),
                                reminderHour = hour,
                                reminderMinute = minute,
                                enabled = enabled,
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Zapisz")
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        startDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Anuluj") }
            },
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    hour = state.hour
                    minute = state.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Anuluj") }
            },
            text = { TimePicker(state = state) },
        )
    }
}

private fun validate(
    childName: String,
    ampoule: Double?,
    daily: Double?,
    days: Int?,
    lastDose: Double?,
): String? = when {
    childName.isBlank() -> "Podaj imię dziecka."
    ampoule == null || ampoule <= 0 -> "Podaj poprawną pojemność ampułki."
    daily == null || daily <= 0 -> "Podaj poprawną dawkę dzienną."
    days == null || days < 1 -> "Podaj poprawną liczbę dni cyklu."
    lastDose == null || lastDose <= 0 ->
        "Dawka dzienna × dni przekracza pojemność ampułki — sprawdź wartości."
    else -> null
}

private fun String.toDoubleOrNullPl(): Double? =
    trim().replace(',', '.').toDoubleOrNull()
