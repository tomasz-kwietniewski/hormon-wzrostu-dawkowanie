package pl.hormonwzrostu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
import pl.hormonwzrostu.data.InjectionSites
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

// Obwódka dnia, od którego liczona jest nowa ampułka (re-kotwica).
internal val AmpouleStartColor = Color(0xFF80D8FF)

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
    skipped: Set<String>,
    ampouleStarts: Set<String>,
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
                                if (date != null) {
                                    DayCell(
                                        date, schedule, intake, skipped, today,
                                        isAmpouleStart = ampouleStarts.contains(date.toString()),
                                        onPickDay = onPickDay,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            var given = 0
            var missed = 0
            for (d in 1..month.lengthOfMonth()) {
                when (dayStatus(schedule, month.atDay(d), today, intake, skipped)) {
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
    skipped: Set<String>,
    today: LocalDate,
    isAmpouleStart: Boolean,
    onPickDay: (LocalDate) -> Unit,
) {
    val status = dayStatus(schedule, date, today, intake, skipped)
    val canPick = status != DayStatus.NONE
    val isToday = date.isEqual(today)
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .background(statusColor(status), shape)
            // Marker re-kotwicy: jasna obwódka na dniu otwarcia nowej ampułki.
            .let { if (isAmpouleStart) it.border(2.dp, AmpouleStartColor, shape) else it }
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
 * Okno edycji jednego dnia z ODROCZONYM zapisem: edytujesz dawkę, komentarz, miejsce i ew.
 * „nową ampułkę", a status (Podano/Pominięto) tylko ZAZNACZASZ — dopiero „Zapisz" zatwierdza
 * całość i zamyka okno. „✕" zamyka bez zapisu. Strzałki/swipe przełączają sąsiednie dni.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayEditDialog(
    date: LocalDate,
    schedule: Schedule,
    status: DayStatus,
    dayInCycle: Int?,
    plannedMg: Double,
    actualMg: Double?,
    initialComment: String,
    isAmpouleStart: Boolean,
    canToggleAmpoule: Boolean,
    selectedSite: String?,
    suggestedSite: String,
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSave: (given: Boolean?, comment: String, doseMg: Double?, site: String?, ampouleStart: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var comment by remember(date) { mutableStateOf(initialComment) }
    var doseText by remember(date) { mutableStateOf(formatMg(actualMg ?: plannedMg)) }
    var siteLocal by remember(date) { mutableStateOf(selectedSite) }
    var ampouleLocal by remember(date) { mutableStateOf(isAmpouleStart) }
    // Wybór statusu jest lokalny, zapisywany dopiero „Zapisz". Start = odbicie zapisanego stanu dnia.
    var chosenGiven by remember(date) {
        mutableStateOf(
            when (status) {
                DayStatus.GIVEN -> true
                DayStatus.MISSED -> false
                else -> null
            },
        )
    }
    // Pole dawki/miejsce/status mają sens dla dni, które realnie można podać (nie dla przyszłych).
    val canDose = status != DayStatus.UPCOMING && status != DayStatus.NONE

    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 56.dp.toPx() }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(
                Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
                    // Swipe lewo/prawo = poprzedni/następny dzień (w zakresie start ... dziś).
                    .pointerInput(date, canPrev, canNext) {
                        var dragX = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragX = 0f },
                            onDragCancel = { dragX = 0f },
                            onDragEnd = {
                                if (dragX > swipeThresholdPx && canPrev) onPrev()
                                else if (dragX < -swipeThresholdPx && canNext) onNext()
                                dragX = 0f
                            },
                            onHorizontalDrag = { _, amount -> dragX += amount },
                        )
                    },
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Nagłówek: ‹ data › oraz zamknięcie.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NavArrow("‹", enabled = canPrev, onClick = onPrev)
                    Text(
                        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    NavArrow("›", enabled = canNext, onClick = onNext)
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.btn_cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Podtytuł: status słowny + (gdy podano) dzień cyklu i dawka.
                val subtitle = if (dayInCycle != null) {
                    statusWord(status) + " · " + stringResource(
                        R.string.edit_day_info,
                        dayInCycle,
                        schedule.daysPerCycle,
                        formatMg(actualMg ?: plannedMg),
                    )
                } else {
                    statusWord(status)
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (canDose) {
                    // Wyróżnione pole dawki: tekst pomocniczy z dawką planową, akcent przy korekcie,
                    // by realne dawki trafiały tutaj (a nie do komentarza).
                    val isCorrection = parseDose(doseText, plannedMg) != null
                    val accent = if (isCorrection) AmpouleStartColor else MaterialTheme.colorScheme.primary
                    OutlinedTextField(
                        value = doseText,
                        onValueChange = { doseText = it },
                        label = { Text(stringResource(R.string.field_actual_dose)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = {
                            Text(
                                if (isCorrection) {
                                    stringResource(R.string.dose_correction_note, formatMg(plannedMg))
                                } else {
                                    stringResource(R.string.dose_helper, formatMg(plannedMg))
                                },
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            focusedLabelColor = accent,
                            focusedSupportingTextColor = accent,
                            unfocusedSupportingTextColor = if (isCorrection) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
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

                // Miejsce wkłucia — chipy z rotacją; gdy nic nie wybrano, pokazujemy podpowiedź.
                if (canDose) {
                    Text(
                        stringResource(R.string.edit_site_label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (siteLocal == null) {
                        Text(
                            stringResource(R.string.site_suggested, siteLabel(suggestedSite)),
                            style = MaterialTheme.typography.bodySmall,
                            color = AmpouleStartColor,
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        InjectionSites.ROTATION.forEach { token ->
                            val isSel = token == siteLocal
                            FilterChip(
                                selected = isSel,
                                onClick = { siteLocal = if (isSel) null else token },
                                label = { Text(siteLabel(token)) },
                            )
                        }
                    }
                }

                // „Nowa ampułka od tego dnia" — pole wyboru; zapis dopiero przy „Zapisz".
                if (canToggleAmpoule && canDose) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { ampouleLocal = !ampouleLocal },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(checked = ampouleLocal, onCheckedChange = { ampouleLocal = it })
                        Text(stringResource(R.string.btn_new_ampoule), color = AmpouleStartColor)
                    }
                }

                // Status — zaznaczany wybór (NIE zamyka okna); zatwierdza go dopiero „Zapisz".
                if (canDose) {
                    Text(
                        stringResource(R.string.edit_status_label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StatusToggle(
                            selected = chosenGiven == true,
                            color = GivenColor,
                            label = stringResource(R.string.legend_given),
                            onClick = { chosenGiven = true },
                        )
                        StatusToggle(
                            selected = chosenGiven == false,
                            color = MissedColor,
                            label = stringResource(R.string.legend_missed),
                            onClick = { chosenGiven = false },
                        )
                    }
                }

                Button(
                    onClick = {
                        onSave(chosenGiven, comment, parseDose(doseText, plannedMg), siteLocal, ampouleLocal)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.btn_save)) }
                Text(
                    stringResource(R.string.save_and_close_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Segmentowany przycisk statusu: wypełniony [color] gdy [selected], inaczej przygaszony obrys. */
@Composable
private fun RowScope.StatusToggle(
    selected: Boolean,
    color: Color,
    label: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val base = Modifier
        .weight(1f)
        .height(48.dp)
        .clip(shape)
        .clickable(onClick = onClick)
    val styled = if (selected) {
        base.background(color)
    } else {
        base.border(1.5.dp, MaterialTheme.colorScheme.outline, shape)
    }
    Box(styled, contentAlignment = Alignment.Center) {
        Text(
            (if (selected) "✓ " else "") + label,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Strzałka nawigacji dni w oknie edycji; wyszarzona, gdy nieaktywna. */
@Composable
private fun NavArrow(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Text(
            glyph,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            },
        )
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
