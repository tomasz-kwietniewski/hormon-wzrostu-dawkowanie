package pl.hormonwzrostu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.hormonwzrostu.R
import pl.hormonwzrostu.data.CsvLabels
import pl.hormonwzrostu.data.Schedule
import pl.hormonwzrostu.data.buildIntakeRows
import pl.hormonwzrostu.data.buildTimeline
import pl.hormonwzrostu.data.InjectionSites
import pl.hormonwzrostu.data.dayStatus
import pl.hormonwzrostu.data.formatMg
import pl.hormonwzrostu.data.nextDose
import pl.hormonwzrostu.notify.isIgnoringBatteryOptimizations
import pl.hormonwzrostu.notify.requestIgnoreBatteryOptimizations
import pl.hormonwzrostu.util.buildIntakeXlsx
import pl.hormonwzrostu.util.shareBytes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    schedule: Schedule,
    intake: Set<String>,
    comments: Map<String, String>,
    doses: Map<String, Double>,
    skipped: Set<String>,
    ampouleStarts: Set<String>,
    sites: Map<String, String>,
    onSetGiven: (LocalDate, Boolean) -> Unit,
    onSetSkipped: (LocalDate, Boolean) -> Unit,
    onSetComment: (LocalDate, String) -> Unit,
    onSetActualDose: (LocalDate, Double?) -> Unit,
    onSetAmpouleStart: (LocalDate, Boolean) -> Unit,
    onSetSite: (LocalDate, String?) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val today = LocalDate.now()
    var selected by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AppLogoBadge()
                        Text(
                            stringResource(R.string.app_name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.btn_settings),
                        )
                    }
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!schedule.isValid()) {
                NotConfiguredCard()
                Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.btn_configure))
                }
            } else {
                TodayDoseCard(
                    schedule = schedule,
                    intake = intake,
                    doses = doses,
                    skipped = skipped,
                    ampouleStarts = ampouleStarts,
                    sites = sites,
                    today = today,
                    onMark = { selected = today },
                    onUndo = {
                        onSetActualDose(today, null)
                        onSetSkipped(today, false)
                        onSetGiven(today, false)
                    },
                )
                CalendarCard(schedule, intake, skipped, ampouleStarts, today, onPickDay = { selected = it })
                ExportButton(schedule, intake, doses, comments, skipped, ampouleStarts, sites, today)
                ScheduleSummaryCard(schedule)
                Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.btn_settings))
                }
            }

            BatteryReliabilityCard()

            Text(
                text = stringResource(R.string.disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    selected?.let { date ->
        val status = dayStatus(schedule, date, today, intake, skipped)
        val timeline = buildTimeline(schedule, intake, doses, ampouleStarts)
        val event = timeline.firstOrNull { it.date == date }
        // Planowana dawka dla tego dnia: z przebiegu (gdy podano) lub projekcja na ten dzień.
        val plannedMg = event?.plannedMg
            ?: nextDose(schedule, intake, doses, date, ampouleStarts)?.plannedMg
            ?: schedule.dailyDoseMg
        // Nawigacja w zakresie start ... dziś (przyszłych dni nie ma po co edytować).
        val start = schedule.startDate()
        val canPrev = start != null && date.isAfter(start)
        val canNext = date.isBefore(today)
        val isStartDay = start != null && date.isEqual(start)
        DayEditDialog(
            date = date,
            schedule = schedule,
            status = status,
            dayInCycle = event?.dayInCycle,
            plannedMg = plannedMg,
            actualMg = doses[date.toString()],
            initialComment = comments[date.toString()] ?: "",
            isAmpouleStart = ampouleStarts.contains(date.toString()),
            canToggleAmpoule = !isStartDay,
            selectedSite = sites[date.toString()],
            suggestedSite = InjectionSites.suggestedFor(sites, date),
            canPrev = canPrev,
            canNext = canNext,
            onPrev = { if (canPrev) selected = date.minusDays(1) },
            onNext = { if (canNext) selected = date.plusDays(1) },
            onSave = { given, comment, doseMg, site, ampouleStart ->
                onSetComment(date, comment)
                onSetSite(date, site)
                onSetAmpouleStart(date, ampouleStart)
                // Re-kotwica implikuje podanie — gdy zaznaczono ampułkę bez statusu, traktuj jak „podano".
                val effectiveGiven = if (ampouleStart && given == null) true else given
                when (effectiveGiven) {
                    true -> {
                        onSetActualDose(date, doseMg)
                        onSetGiven(date, true)
                    }
                    false -> {
                        onSetActualDose(date, null)
                        onSetSkipped(date, true)
                    }
                    null -> {}
                }
                selected = null
            },
            onDismiss = { selected = null },
        )
    }
}

/** Mały znak marki w pasku: zaokrąglona plakietka w kolorze marki z białą kroplą+krzyżem. */
@Composable
private fun AppLogoBadge() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(Color(0xFF2E6B5E), RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_logo_drop),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(22.dp),
        )
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
    intake: Set<String>,
    doses: Map<String, Double>,
    skipped: Set<String>,
    ampouleStarts: Set<String>,
    sites: Map<String, String>,
    today: LocalDate,
    onMark: () -> Unit,
    onUndo: () -> Unit,
) {
    val given = intake.contains(today.toString())
    val skippedToday = skipped.contains(today.toString())
    // Gdy dziś już podano — stan z faktycznego przebiegu (uwzględnia korektę dawki);
    // gdy jeszcze nie podano — projekcja następnej dawki.
    val event = if (given) buildTimeline(schedule, intake, doses, ampouleStarts).firstOrNull { it.date == today } else null
    val next = if (event == null) nextDose(schedule, intake, doses, today, ampouleStarts) else null
    val dayInCycle = event?.dayInCycle ?: next?.dayInCycle
    val shownDose = event?.actualMg ?: next?.plannedMg
    val isLast = event?.isLastInCycle ?: next?.isLastInCycle ?: false

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.today_dose_title), style = MaterialTheme.typography.titleMedium)

            if (skippedToday) {
                Text(
                    stringResource(R.string.skipped_today_done),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                TextButton(onClick = onUndo) {
                    Text(stringResource(R.string.btn_unmark_given))
                }
            } else if (dayInCycle == null || shownDose == null) {
                Text(stringResource(R.string.cycle_not_started), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    stringResource(R.string.mg_value, formatMg(shownDose)),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(
                        R.string.day_of_cycle_est,
                        schedule.childName,
                        dayInCycle,
                        schedule.daysPerCycle,
                    ),
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
                if (given) {
                    Text(
                        stringResource(R.string.given_today_done),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    TextButton(onClick = onUndo) {
                        Text(stringResource(R.string.btn_unmark_given))
                    }
                } else {
                    // Podpowiedź rotacji miejsc — następne po ostatnio użytym.
                    Text(
                        stringResource(R.string.today_next_site, siteLabel(InjectionSites.suggestedFor(sites, today))),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Button(onClick = onMark, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.btn_mark_given))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportButton(
    schedule: Schedule,
    intake: Set<String>,
    doses: Map<String, Double>,
    comments: Map<String, String>,
    skipped: Set<String>,
    ampouleStarts: Set<String>,
    sites: Map<String, String>,
    today: LocalDate,
) {
    val context = LocalContext.current
    val exportTitle = stringResource(R.string.export_share_title)
    val sheetName = stringResource(R.string.xlsx_sheet)
    val siteLabels = siteLabelsMap()
    val labels = CsvLabels(
        date = stringResource(R.string.csv_col_date),
        day = stringResource(R.string.csv_col_day),
        dose = stringResource(R.string.csv_col_dose),
        status = stringResource(R.string.csv_col_status),
        comment = stringResource(R.string.csv_col_comment),
        given = stringResource(R.string.status_given),
        missed = stringResource(R.string.status_missed),
        pending = stringResource(R.string.status_pending),
        site = stringResource(R.string.csv_col_site),
    )
    Button(
        onClick = {
            val rows = buildIntakeRows(
                schedule, intake, doses, comments, today, labels,
                ampouleStarts, skipped, sites, siteLabels,
            )
            val xlsx = buildIntakeXlsx(sheetName, labels, rows)
            val safeChild = schedule.childName.trim().ifBlank { "intake" }
                .replace(Regex("[^A-Za-z0-9]+"), "_")
            shareBytes(
                context,
                xlsx,
                "hormon_${safeChild}_$today.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                exportTitle,
            )
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.btn_export_xlsx))
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
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
