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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import pl.hormonwzrostu.R
import pl.hormonwzrostu.data.DayStatus
import pl.hormonwzrostu.data.Schedule
import pl.hormonwzrostu.data.dayStatus
import pl.hormonwzrostu.data.formatMg
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

// Stałe, semantyczne kolory statusów — spójne dla kratek i kropek legendy.
internal val GivenColor = Color(0xFF2E7D32)
internal val MissedColor = Color(0xFFC62828)
private val TodayColor = Color(0xFFF9A825)
private val UpcomingColor = Color(0xFF4A4A4A)

internal fun statusColor(status: DayStatus): Color = when (status) {
    DayStatus.GIVEN -> GivenColor
    DayStatus.MISSED -> MissedColor
    DayStatus.TODAY_PENDING -> TodayColor
    DayStatus.UPCOMING -> UpcomingColor
    DayStatus.NONE -> Color.Transparent
}

private fun dayContentColor(status: DayStatus): Color = when (status) {
    DayStatus.TODAY_PENDING -> Color(0xFF1A1A1A)
    DayStatus.UPCOMING -> Color(0xFFD6D6D6)
    DayStatus.NONE -> Color(0xFF9A9A9A)
    else -> Color.White
}

@Composable
internal fun statusWord(status: DayStatus): String = stringResource(
    when (status) {
        DayStatus.GIVEN -> R.string.legend_given
        DayStatus.MISSED -> R.string.legend_missed
        DayStatus.TODAY_PENDING -> R.string.legend_today
        else -> R.string.legend_upcoming
    },
)

/** Kalendarz miesięczny z nawigacją, licznikiem i legendą. Tapnięcie dnia → [onPickDay]. */
@Composable
fun CalendarCard(
    schedule: Schedule,
    intake: Set<String>,
    today: LocalDate,
    onPickDay: (LocalDate) -> Unit,
) {
    val locale = Locale.getDefault()
    var month by remember { mutableStateOf(YearMonth.from(today)) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { month = month.minusMonths(1) }) {
                    Text("‹", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    month.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
                    style = MaterialTheme.typography.titleLarge,
                )
                TextButton(onClick = { month = month.plusMonths(1) }) {
                    Text("›", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
            }

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

            val leading = month.atDay(1).dayOfWeek.value - 1
            val cells = buildList<LocalDate?> {
                repeat(leading) { add(null) }
                for (d in 1..month.lengthOfMonth()) add(month.atDay(d))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 0 until 7) {
                            val date = week.getOrNull(i)
                            Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                                if (date != null) DayCell(date, schedule, intake, today, onPickDay)
                            }
                        }
                    }
                }
            }

            var given = 0
            var missed = 0
            for (d in 1..month.lengthOfMonth()) {
                when (dayStatus(schedule, month.atDay(d), today, intake)) {
                    DayStatus.GIVEN -> given++
                    DayStatus.MISSED -> missed++
                    else -> {}
                }
            }
            Text(
                stringResource(R.string.history_counts, given, missed),
                style = MaterialTheme.typography.titleMedium,
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LegendItem(GivenColor, stringResource(R.string.legend_given))
                LegendItem(MissedColor, stringResource(R.string.legend_missed))
                LegendItem(TodayColor, stringResource(R.string.legend_today))
                LegendItem(UpcomingColor, stringResource(R.string.legend_upcoming))
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
    val canPick = status != DayStatus.NONE
    val isToday = date.isEqual(today)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .background(statusColor(status), RoundedCornerShape(10.dp))
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
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(14.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Okno edycji jednego dnia: data, dzień cyklu/dawka, pole faktycznie podanej dawki, komentarz
 * oraz akcje Podano (zielony) / Pominięto (czerwony) / Zapisz (sam komentarz).
 * Pole dawki widoczne, gdy dzień można podać (dziś, wstecz). „X" zamyka bez zmian.
 */
@Composable
fun DayEditDialog(
    date: LocalDate,
    schedule: Schedule,
    status: DayStatus,
    dayInCycle: Int?,
    plannedMg: Double,
    actualMg: Double?,
    initialComment: String,
    onConfirm: (given: Boolean, comment: String, doseMg: Double?) -> Unit,
    onSaveComment: (comment: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var comment by remember(date) { mutableStateOf(initialComment) }
    var doseText by remember(date) { mutableStateOf(formatMg(actualMg ?: plannedMg)) }
    // Pole dawki ma sens dla dni, które realnie można podać (nie dla przyszłych).
    val canDose = status != DayStatus.UPCOMING && status != DayStatus.NONE

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Box(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(end = 36.dp),
                    )
                    if (dayInCycle != null) {
                        Text(
                            stringResource(
                                R.string.edit_day_info,
                                dayInCycle,
                                schedule.daysPerCycle,
                                formatMg(actualMg ?: plannedMg),
                            ),
                        )
                    }
                    Text(stringResource(R.string.edit_current, statusWord(status)))

                    if (canDose) {
                        OutlinedTextField(
                            value = doseText,
                            onValueChange = { doseText = it },
                            label = { Text(stringResource(R.string.field_actual_dose)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text(stringResource(R.string.field_comment)) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { onConfirm(true, comment, parseDose(doseText, plannedMg)) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GivenColor, contentColor = Color.White),
                        ) { Text(stringResource(R.string.legend_given)) }
                        Button(
                            onClick = { onConfirm(false, comment, null) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MissedColor, contentColor = Color.White),
                        ) { Text(stringResource(R.string.legend_missed)) }
                    }
                    TextButton(
                        onClick = { onSaveComment(comment) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.btn_save)) }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.btn_cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Parsuje wpisaną dawkę (akceptuje przecinek i kropkę). Zwraca null, gdy puste, niepoprawne,
 * ≤ 0 lub równe dawce planowanej — null oznacza „bez override, trzymaj się rozpiski".
 */
private fun parseDose(text: String, plannedMg: Double): Double? {
    val value = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (value <= 0.0) return null
    val rounded = Math.round(value * 1000.0) / 1000.0
    val plannedRounded = Math.round(plannedMg * 1000.0) / 1000.0
    return if (rounded == plannedRounded) null else rounded
}
