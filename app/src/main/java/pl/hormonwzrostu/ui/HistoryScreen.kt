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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.hormonwzrostu.R
import pl.hormonwzrostu.data.DayStatus
import pl.hormonwzrostu.data.Schedule
import pl.hormonwzrostu.data.buildIntakeCsv
import pl.hormonwzrostu.data.dayStatus
import pl.hormonwzrostu.util.shareCsv
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    schedule: Schedule,
    intake: Set<String>,
    onToggleDay: (LocalDate, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val today = LocalDate.now()
    val locale = Locale.getDefault()
    var month by remember { mutableStateOf(YearMonth.from(today)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } },
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
                TextButton(onClick = { month = month.minusMonths(1) }) { Text("‹  ") }
                Text(
                    month.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
                    style = MaterialTheme.typography.titleLarge,
                )
                TextButton(onClick = { month = month.plusMonths(1) }) { Text("  ›") }
            }

            CalendarGrid(
                month = month,
                schedule = schedule,
                intake = intake,
                today = today,
                locale = locale,
                onToggleDay = onToggleDay,
            )

            MonthSummary(month, schedule, intake, today)

            Legend()

            val context = LocalContext.current
            val exportTitle = stringResource(R.string.export_share_title)
            Button(
                onClick = {
                    val csv = buildIntakeCsv(schedule, intake, today)
                    val safeChild = schedule.childName.trim().ifBlank { "intake" }
                        .replace(Regex("[^A-Za-z0-9]+"), "_")
                    shareCsv(context, csv, "hormon_${safeChild}_$today.csv", exportTitle)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.btn_export_csv))
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    schedule: Schedule,
    intake: Set<String>,
    today: LocalDate,
    locale: Locale,
    onToggleDay: (LocalDate, Boolean) -> Unit,
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
                            DayCell(date, schedule, intake, today, onToggleDay)
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
    onToggleDay: (LocalDate, Boolean) -> Unit,
) {
    val status = dayStatus(schedule, date, today, intake)
    val bg = statusColor(status)
    val canToggle = status != DayStatus.NONE && status != DayStatus.UPCOMING
    val isToday = date.isEqual(today)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .background(bg, RoundedCornerShape(10.dp))
            .let { if (canToggle) it.clickable { onToggleDay(date, status != DayStatus.GIVEN) } else it },
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

@Composable
private fun statusColor(status: DayStatus): Color = when (status) {
    DayStatus.GIVEN -> MaterialTheme.colorScheme.primary
    DayStatus.MISSED -> MaterialTheme.colorScheme.errorContainer
    DayStatus.TODAY_PENDING -> MaterialTheme.colorScheme.tertiary
    DayStatus.UPCOMING -> MaterialTheme.colorScheme.surfaceVariant
    DayStatus.NONE -> Color.Transparent
}

@Composable
private fun dayContentColor(status: DayStatus): Color = when (status) {
    DayStatus.GIVEN -> MaterialTheme.colorScheme.onPrimary
    DayStatus.MISSED -> MaterialTheme.colorScheme.onErrorContainer
    DayStatus.TODAY_PENDING -> MaterialTheme.colorScheme.onTertiary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
