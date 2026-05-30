package pl.hormonwzrostu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.hormonwzrostu.R
import pl.hormonwzrostu.data.CsvLabels
import pl.hormonwzrostu.data.DayStatus
import pl.hormonwzrostu.data.Schedule
import pl.hormonwzrostu.data.buildIntakeRows
import pl.hormonwzrostu.data.dayStatus
import pl.hormonwzrostu.data.formatMg
import pl.hormonwzrostu.util.buildIntakeXlsx
import pl.hormonwzrostu.util.shareBytes
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    schedule: Schedule,
    intake: Set<String>,
    comments: Map<String, String>,
    onToggleDay: (LocalDate, Boolean) -> Unit,
    onSetComment: (LocalDate, String) -> Unit,
    onBack: () -> Unit,
) {
    val today = LocalDate.now()
    val locale = Locale.getDefault()
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("←", fontSize = 26.sp, fontWeight = FontWeight.Bold)
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
                Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.bodyLarge)
                return@Column
            }

            // Nagłówek miesiąca + nawigacja.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { month = month.minusMonths(1) }) {
                    Text("‹", fontSize = 34.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    month.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
                    style = MaterialTheme.typography.titleLarge,
                )
                TextButton(onClick = { month = month.plusMonths(1) }) {
                    Text("›", fontSize = 34.sp, fontWeight = FontWeight.Bold)
                }
            }

            CalendarGrid(
                month = month,
                schedule = schedule,
                intake = intake,
                today = today,
                locale = locale,
                onPickDay = { selected = it },
            )

            MonthSummary(month, schedule, intake, today)

            Legend()

            val context = LocalContext.current
            val exportTitle = stringResource(R.string.export_share_title)
            val csvLabels = CsvLabels(
                date = stringResource(R.string.csv_col_date),
                day = stringResource(R.string.csv_col_day),
                dose = stringResource(R.string.csv_col_dose),
                status = stringResource(R.string.csv_col_status),
                comment = stringResource(R.string.csv_col_comment),
                given = stringResource(R.string.status_given),
                missed = stringResource(R.string.status_missed),
                pending = stringResource(R.string.status_pending),
            )
            val sheetName = stringResource(R.string.xlsx_sheet)
            Button(
                onClick = {
                    val rows = buildIntakeRows(schedule, intake, comments, today, csvLabels)
                    val xlsx = buildIntakeXlsx(sheetName, csvLabels, rows)
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
    }

    selected?.let { date ->
        val idx = schedule.dayIndexInCycle(date)
        val status = dayStatus(schedule, date, today, intake)
        var comment by remember(date) { mutableStateOf(comments[date.toString()] ?: "") }

        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (idx != null) {
                        Text(
                            stringResource(
                                R.string.edit_day_info,
                                idx + 1,
                                schedule.daysPerCycle,
                                formatMg(schedule.doseForDay(idx)),
                            ),
                        )
                    }
                    Text(stringResource(R.string.edit_current, statusWord(status)))
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text(stringResource(R.string.field_comment)) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onSetComment(date, comment)
                            onToggleDay(date, true)
                            selected = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GivenColor,
                            contentColor = Color.White,
                        ),
                    ) { Text(stringResource(R.string.legend_given)) }
                    Button(
                        onClick = {
                            onSetComment(date, comment)
                            onToggleDay(date, false)
                            selected = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MissedColor,
                            contentColor = Color.White,
                        ),
                    ) { Text(stringResource(R.string.legend_missed)) }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onSetComment(date, comment)
                    selected = null
                }) { Text(stringResource(R.string.btn_save)) }
            },
        )
    }
}

@Composable
private fun statusWord(status: DayStatus): String = stringResource(
    when (status) {
        DayStatus.GIVEN -> R.string.legend_given
        DayStatus.MISSED -> R.string.legend_missed
        DayStatus.TODAY_PENDING -> R.string.legend_today
        else -> R.string.legend_upcoming
    },
)

@Composable
private fun CalendarGrid(
    month: YearMonth,
    schedule: Schedule,
    intake: Set<String>,
    today: LocalDate,
    locale: Locale,
    onPickDay: (LocalDate) -> Unit,
) {
    // Nagłówek dni tygodnia (poniedziałek-pierwszy).
    val weekDays = (0..6).map { DayOfWeek.MONDAY.plus(it.toLong()) }
    Row(Modifier.fillMaxWidth()) {
        weekDays.forEach { dow ->
            Text(
                dow.getDisplayName(TextStyle.SHORT, locale),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    val leading = month.atDay(1).dayOfWeek.value - 1 // Monday=1 -> 0 leading
    val cells = buildList<LocalDate?> {
        repeat(leading) { add(null) }
        for (d in 1..month.lengthOfMonth()) add(month.atDay(d))
    }
    val weeks = cells.chunked(7)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 0 until 7) {
                    val date = week.getOrNull(i)
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (date != null) {
                            DayCell(date, schedule, intake, today, onPickDay)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    schedule: Schedule,
    intake: Set<String>,
    today: LocalDate,
    onPickDay: (LocalDate) -> Unit,
) {
    val status = dayStatus(schedule, date, today, intake)
    val bg = statusColor(status)
    val canPick = status != DayStatus.NONE
    val isToday = date.isEqual(today)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .background(bg, RoundedCornerShape(10.dp))
            .let { if (canPick) it.clickable { onPickDay(date) } else it },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = dayContentColor(status),
        )
    }
}

@Composable
private fun MonthSummary(month: YearMonth, schedule: Schedule, intake: Set<String>, today: LocalDate) {
    var given = 0
    var missed = 0
    for (d in 1..month.lengthOfMonth()) {
        when (dayStatus(schedule, month.atDay(d), today, intake)) {
            DayStatus.GIVEN -> given++
            DayStatus.MISSED -> missed++
            else -> {}
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.history_counts, given, missed),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun Legend() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LegendItem(statusColor(DayStatus.GIVEN), stringResource(R.string.legend_given))
        LegendItem(statusColor(DayStatus.MISSED), stringResource(R.string.legend_missed))
        LegendItem(statusColor(DayStatus.TODAY_PENDING), stringResource(R.string.legend_today))
        LegendItem(statusColor(DayStatus.UPCOMING), stringResource(R.string.legend_upcoming))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(14.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

// Stałe, semantyczne kolory statusów — spójne dla kratek kalendarza i kropek legendy.
private val GivenColor = Color(0xFF2E7D32)     // zielony — podano
private val MissedColor = Color(0xFFC62828)    // czerwony — pominięto
private val TodayColor = Color(0xFFF9A825)     // bursztynowy — dziś
private val UpcomingColor = Color(0xFF4A4A4A)  // szary — później

private fun statusColor(status: DayStatus): Color = when (status) {
    DayStatus.GIVEN -> GivenColor
    DayStatus.MISSED -> MissedColor
    DayStatus.TODAY_PENDING -> TodayColor
    DayStatus.UPCOMING -> UpcomingColor
    DayStatus.NONE -> Color.Transparent
}

private fun dayContentColor(status: DayStatus): Color = when (status) {
    DayStatus.TODAY_PENDING -> Color(0xFF1A1A1A) // ciemny tekst na bursztynie
    DayStatus.UPCOMING -> Color(0xFFD6D6D6)
    DayStatus.NONE -> Color(0xFF9A9A9A)
    else -> Color.White // zielony/czerwony — biały tekst
}
