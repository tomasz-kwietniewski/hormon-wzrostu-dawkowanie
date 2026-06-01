package pl.hormonwzrostu.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.hormonwzrostu.BuildConfig
import pl.hormonwzrostu.R
import pl.hormonwzrostu.data.Schedule
import pl.hormonwzrostu.data.computeCycleDays
import pl.hormonwzrostu.data.formatMg
import pl.hormonwzrostu.util.shareBytes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initial: Schedule,
    currentLang: String,
    onSelectLang: (String) -> Unit,
    onExportBackup: () -> String,
    onImportBackup: (String) -> Boolean,
    onSave: (Schedule) -> Unit,
    onCancel: () -> Unit,
) {
    val defaultMed = stringResource(R.string.field_med_default)
    var childName by remember { mutableStateOf(initial.childName) }
    var medName by remember { mutableStateOf(initial.medName) }
    var ampoule by remember { mutableStateOf(formatMg(initial.ampouleMg)) }
    var dailyDose by remember { mutableStateOf(formatMg(initial.dailyDoseMg)) }
    var startDate by remember { mutableStateOf(initial.startDate() ?: LocalDate.now()) }
    var hour by remember { mutableStateOf(initial.reminderHour) }
    var minute by remember { mutableStateOf(initial.reminderMinute) }
    var enabled by remember { mutableStateOf(initial.enabled) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var errorRes by remember { mutableStateOf<Int?>(null) }

    val ampouleVal = ampoule.toDoubleOrNullPl()
    val dailyVal = dailyDose.toDoubleOrNullPl()
    // Długość cyklu liczy się automatycznie z pojemności i dawki dziennej (nie wpisuje się jej ręcznie).
    val daysVal = if (ampouleVal != null && dailyVal != null) {
        computeCycleDays(ampouleVal, dailyVal).takeIf { it >= 1 }
    } else {
        null
    }
    val lastDose = if (ampouleVal != null && dailyVal != null && daysVal != null) {
        ampouleVal - (daysVal - 1) * dailyVal
    } else {
        null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.btn_cancel)) }
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
                label = { Text(stringResource(R.string.field_child)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = medName,
                onValueChange = { medName = it },
                label = { Text(stringResource(R.string.field_med)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ampoule,
                onValueChange = { ampoule = it },
                label = { Text(stringResource(R.string.field_ampoule)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = dailyDose,
                onValueChange = { dailyDose = it },
                label = { Text(stringResource(R.string.field_daily)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.last_dose_calc_title), style = MaterialTheme.typography.labelLarge)
                    if (lastDose != null && lastDose > 0) {
                        Text(
                            stringResource(
                                R.string.last_dose_calc_value,
                                formatMg(dailyVal!!),
                                daysVal!! - 1,
                                formatMg(lastDose),
                                daysVal,
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    } else {
                        Text(
                            stringResource(R.string.last_dose_calc_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(
                        R.string.start_day_button,
                        startDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
                    ),
                )
            }
            OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.reminder_time_button, "%02d:%02d".format(hour, minute)))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.notifications_enabled), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }

            LanguageSelector(currentLang, onSelectLang)

            BackupSection(onExportBackup, onImportBackup)

            errorRes?.let {
                Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = {
                    val problem = validate(childName, ampouleVal, dailyVal, lastDose)
                    if (problem != null) {
                        errorRes = problem
                    } else {
                        onSave(
                            Schedule(
                                childName = childName.trim(),
                                medName = medName.trim().ifBlank { defaultMed },
                                ampouleMg = ampouleVal!!,
                                dailyDoseMg = dailyVal!!,
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
                Text(stringResource(R.string.btn_save))
            }

            Text(
                stringResource(R.string.version_label, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
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
                }) { Text(stringResource(R.string.btn_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.btn_cancel)) }
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
                }) { Text(stringResource(R.string.btn_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.btn_cancel)) }
            },
            text = { TimePicker(state = state) },
        )
    }
}

@Composable
private fun LanguageSelector(currentTag: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.language_label), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = currentTag == "",
                onClick = { onSelect("") },
                label = { Text(stringResource(R.string.language_system)) },
            )
            FilterChip(
                selected = currentTag == "pl",
                onClick = { onSelect("pl") },
                label = { Text("Polski") },
            )
            FilterChip(
                selected = currentTag == "en",
                onClick = { onSelect("en") },
                label = { Text("English") },
            )
        }
    }
}

@Composable
private fun BackupSection(
    onExportBackup: () -> String,
    onImportBackup: (String) -> Boolean,
) {
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.backup_share_title)
    val okMsg = stringResource(R.string.import_ok)
    val failMsg = stringResource(R.string.import_fail)

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            }.getOrNull()
            val ok = text != null && onImportBackup(text)
            Toast.makeText(context, if (ok) okMsg else failMsg, Toast.LENGTH_LONG).show()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.backup_section), style = MaterialTheme.typography.labelLarge)
        Text(
            stringResource(R.string.backup_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = {
                val json = onExportBackup()
                shareBytes(
                    context,
                    json.toByteArray(Charsets.UTF_8),
                    "hormon_backup_${LocalDate.now()}.json",
                    "application/json",
                    shareTitle,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.btn_export_backup))
        }
        OutlinedButton(
            onClick = { importLauncher.launch("*/*") },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.btn_import_backup))
        }
    }
}

/** Zwraca id zasobu z komunikatem błędu lub null, gdy dane są poprawne. */
private fun validate(
    childName: String,
    ampoule: Double?,
    daily: Double?,
    lastDose: Double?,
): Int? = when {
    childName.isBlank() -> R.string.err_child
    ampoule == null || ampoule <= 0 -> R.string.err_ampoule
    daily == null || daily <= 0 -> R.string.err_daily
    lastDose == null || lastDose <= 0 -> R.string.err_lastdose
    else -> null
}

private fun String.toDoubleOrNullPl(): Double? =
    trim().replace(',', '.').toDoubleOrNull()
