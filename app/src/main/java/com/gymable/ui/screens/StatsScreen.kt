package com.gymable.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymable.ui.components.AppDropdown
import com.gymable.ui.components.ScreenScaffold
import com.gymable.ui.stats.DateRange
import com.gymable.ui.stats.PeriodMode
import com.gymable.ui.stats.StatMode
import com.gymable.ui.stats.StatPoint
import com.gymable.ui.stats.StatsViewModel
import com.gymable.ui.theme.CardBorder
import com.gymable.ui.theme.LocalUnitSystem
import com.gymable.ui.theme.Outline
import com.gymable.ui.theme.SurfaceElevated
import com.gymable.ui.theme.SurfaceHigh
import com.gymable.ui.theme.TextSecondary
import com.gymable.ui.theme.UnitSystem
import com.gymable.ui.theme.label
import com.gymable.ui.theme.toDisplayWeight
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val gyms by viewModel.gyms.collectAsStateWithLifecycle()
    val selectedGym by viewModel.selectedGym.collectAsStateWithLifecycle()
    val muscleGroups by viewModel.muscleGroups.collectAsStateWithLifecycle()
    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val selectedExercise by viewModel.selectedExercise.collectAsStateWithLifecycle()
    val statMode by viewModel.statMode.collectAsStateWithLifecycle()
    val dataPoints by viewModel.dataPoints.collectAsStateWithLifecycle()
    val latestPoint by viewModel.latestPoint.collectAsStateWithLifecycle()
    val periodMode by viewModel.periodMode.collectAsStateWithLifecycle()
    val periodLabel by viewModel.periodLabel.collectAsStateWithLifecycle()
    val canNavigateForward by viewModel.canNavigateForward.collectAsStateWithLifecycle()
    val customRange by viewModel.customRange.collectAsStateWithLifecycle()

    val unitSystem = LocalUnitSystem.current
    var showDatePicker by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "Stats",
        modifier = modifier,
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppDropdown(
                label = "Gym",
                items = gyms,
                selected = selectedGym,
                itemLabel = { it.name },
                onSelect = viewModel::selectGym,
                modifier = Modifier.fillMaxWidth()
            )

            AppDropdown(
                label = "Muscle Group",
                items = muscleGroups,
                selected = selectedMuscleGroup,
                itemLabel = { it.name },
                onSelect = viewModel::selectMuscleGroup,
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedGym != null
            )

            AppDropdown(
                label = "Exercise",
                items = exercises,
                selected = selectedExercise,
                itemLabel = { it.name },
                onSelect = viewModel::selectExercise,
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedMuscleGroup != null
            )

            StatModeSelector(
                selected = statMode,
                onSelect = viewModel::setStatMode,
                modifier = Modifier.fillMaxWidth()
            )

            PeriodRow(
                periodMode = periodMode,
                periodLabel = periodLabel,
                canNavigateForward = canNavigateForward,
                onPeriodModeChange = { mode ->
                    viewModel.setPeriodMode(mode)
                    if (mode == PeriodMode.CUSTOM) showDatePicker = true
                },
                onNavigate = viewModel::navigatePeriod,
                onOpenCustomPicker = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            )

            if (selectedExercise == null) {
                ExercisePlaceholder(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            } else {
                latestPoint?.let { point ->
                    ValueCard(
                        point = point,
                        mode = statMode,
                        unitSystem = unitSystem,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                StatsChart(
                    points = dataPoints,
                    mode = statMode,
                    unitSystem = unitSystem,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showDatePicker) {
        DateRangePickerDialog(
            initialRange = customRange,
            onConfirm = { range ->
                viewModel.setCustomRange(range.startMs, range.endMs)
                showDatePicker = false
            },
            onDismiss = {
                if (customRange == null) viewModel.setPeriodMode(PeriodMode.MONTH)
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun PeriodRow(
    periodMode: PeriodMode,
    periodLabel: String,
    canNavigateForward: Boolean,
    onPeriodModeChange: (PeriodMode) -> Unit,
    onNavigate: (Int) -> Unit,
    onOpenCustomPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PeriodModeChip(mode = periodMode, onModeChange = onPeriodModeChange)

        if (periodMode != PeriodMode.CUSTOM) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onNavigate(-1) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Previous period",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = periodLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 110.dp),
                    maxLines = 1
                )
                IconButton(
                    onClick = { onNavigate(1) },
                    enabled = canNavigateForward
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Next period",
                        tint = if (canNavigateForward) MaterialTheme.colorScheme.onSurface
                               else TextSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            Surface(
                onClick = onOpenCustomPicker,
                shape = RoundedCornerShape(10.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodModeChip(mode: PeriodMode, onModeChange: (PeriodMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(10.dp),
            color = SurfaceElevated,
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = when (mode) {
                        PeriodMode.MONTH  -> "Month"
                        PeriodMode.WEEK   -> "Week"
                        PeriodMode.CUSTOM -> "Custom"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            PeriodMode.values().forEach { m ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = when (m) {
                                PeriodMode.MONTH  -> "Month"
                                PeriodMode.WEEK   -> "Week"
                                PeriodMode.CUSTOM -> "Custom"
                            },
                            color = if (m == mode) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = { onModeChange(m); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun DateRangePickerDialog(
    initialRange: DateRange?,
    onConfirm: (DateRange) -> Unit,
    onDismiss: () -> Unit
) {
    var calYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var calMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var selStart by remember { mutableStateOf<Long?>(initialRange?.startMs) }
    var selEnd by remember { mutableStateOf<Long?>(initialRange?.endMs) }

    val monthDisplayText = remember(calYear, calMonth) {
        val cal = Calendar.getInstance().apply { set(calYear, calMonth, 1) }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            .format(cal.time)
            .replaceFirstChar { it.uppercase() }
    }

    val days = remember(calYear, calMonth) {
        val first = Calendar.getInstance().apply {
            set(calYear, calMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dim = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dow = first.get(Calendar.DAY_OF_WEEK)
        val offset = (dow - Calendar.MONDAY + 7) % 7
        buildList<Long?> {
            repeat(offset) { add(null) }
            for (d in 1..dim) {
                val c = first.clone() as Calendar
                c.set(Calendar.DAY_OF_MONTH, d)
                add(c.timeInMillis)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Select Date Range",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        if (calMonth == 0) { calMonth = 11; calYear-- } else calMonth--
                    }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Previous month",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = monthDisplayText,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = {
                        if (calMonth == 11) { calMonth = 0; calYear++ } else calMonth++
                    }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Next month",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
                Row(modifier = Modifier.fillMaxWidth()) {
                    dayNames.forEach { d ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                text = d,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                val weeks = days.chunked(7)
                weeks.forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEachIndexed { colIndex, dayMs ->
                            if (dayMs == null) {
                                Box(modifier = Modifier.weight(1f).height(40.dp))
                            } else {
                                val isStart = selStart?.let { isSameDay(it, dayMs) } == true
                                val isEnd = selEnd?.let { isSameDay(it, dayMs) } == true
                                val hasRange = selStart != null && selEnd != null
                                val inRange = hasRange && dayMs > selStart!! && dayMs < selEnd!!
                                val isLastInRow = colIndex == 6 || colIndex == week.lastIndex

                                DayCell(
                                    dayMs = dayMs,
                                    isStart = isStart,
                                    isEnd = isEnd,
                                    inRange = inRange,
                                    hasRange = hasRange,
                                    isFirstInRow = colIndex == 0,
                                    isLastInRow = isLastInRow,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        when {
                                            selStart == null || (selStart != null && selEnd != null) -> {
                                                selStart = dayMs; selEnd = null
                                            }
                                            dayMs >= selStart!! -> selEnd = dayMs
                                            else -> { selEnd = selStart; selStart = dayMs }
                                        }
                                    }
                                )
                            }
                        }
                        repeat(7 - week.size) {
                            Box(modifier = Modifier.weight(1f).height(40.dp))
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val s = selStart; val e = selEnd
                            if (s != null && e != null) onConfirm(DateRange(s, e))
                        },
                        enabled = selStart != null && selEnd != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        )
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    dayMs: Long,
    isStart: Boolean,
    isEnd: Boolean,
    inRange: Boolean,
    hasRange: Boolean,
    isFirstInRow: Boolean,
    isLastInRow: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val onAccentColor = MaterialTheme.colorScheme.onPrimary
    val dayNumber = remember(dayMs) {
        Calendar.getInstance().apply { timeInMillis = dayMs }.get(Calendar.DAY_OF_MONTH)
    }
    val textColor = when {
        isStart || isEnd -> onAccentColor
        inRange -> accentColor
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(40.dp)
            .drawBehind {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f
                val circleR = minOf(w, h) * 0.42f

                if (inRange) {
                    drawRect(color = containerColor, size = Size(w, h))
                }
                if (isStart && hasRange && !isLastInRow) {
                    drawRect(
                        color = containerColor,
                        topLeft = Offset(cx, 0f),
                        size = Size(w - cx, h)
                    )
                }
                if (isEnd && hasRange && !isFirstInRow) {
                    drawRect(
                        color = containerColor,
                        topLeft = Offset(0f, 0f),
                        size = Size(cx, h)
                    )
                }
                if (isStart || isEnd) {
                    drawCircle(color = accentColor, radius = circleR, center = Offset(cx, cy))
                }
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayNumber.toString(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isStart || isEnd) FontWeight.Bold else FontWeight.Normal
            ),
            color = textColor
        )
    }
}

private fun isSameDay(ms1: Long, ms2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = ms1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = ms2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
           c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun StatModeSelector(
    selected: StatMode,
    onSelect: (StatMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = SurfaceElevated,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            StatMode.values().forEach { mode ->
                val isActive = mode == selected

                val activeColor = MaterialTheme.colorScheme.primary
                val activeOnColor = MaterialTheme.colorScheme.onPrimary
                val bgColor by animateColorAsState(
                    targetValue = if (isActive) activeColor else Color.Transparent,
                    animationSpec = tween(200),
                    label = "statModeBg_${mode.name}"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isActive) activeOnColor else TextSecondary,
                    animationSpec = tween(200),
                    label = "statModeText_${mode.name}"
                )

                Surface(
                    onClick = { onSelect(mode) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = bgColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = when (mode) {
                                StatMode.WEIGHT -> "Weight"
                                StatMode.REPS   -> "Reps"
                                StatMode.VOLUME -> "Volume"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                                color = textColor
                            ),
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExercisePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.BarChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "Select an exercise",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ValueCard(
    point: StatPoint,
    mode: StatMode,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier
) {
    val label = when (mode) {
        StatMode.WEIGHT -> "MAX WEIGHT"
        StatMode.REPS   -> "TOTAL REPS"
        StatMode.VOLUME -> "TOTAL VOLUME"
    }
    val displayValue = when (mode) {
        StatMode.WEIGHT, StatMode.VOLUME -> point.value.toDisplayWeight(unitSystem)
        StatMode.REPS -> point.value
    }
    val valueText = when (mode) {
        StatMode.WEIGHT -> if (displayValue % 1f == 0f) displayValue.toInt().toString()
                           else "%.1f".format(displayValue)
        StatMode.REPS   -> displayValue.toInt().toString()
        StatMode.VOLUME -> "%.0f".format(displayValue)
    }
    val unit = when (mode) {
        StatMode.WEIGHT -> unitSystem.label()
        StatMode.REPS   -> ""
        StatMode.VOLUME -> unitSystem.label()
    }
    val dateText = if (point.dateMs.isToday()) "Today"
                   else SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(point.dateMs)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (unit.isNotEmpty()) {
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                }
            }
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun StatsChart(
    points: List<StatPoint>,
    mode: StatMode,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(240.dp),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceElevated,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        if (points.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "No workouts in this period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            ChartCanvas(points = points, mode = mode, unitSystem = unitSystem)
        }
    }
}

@Composable
private fun ChartCanvas(
    points: List<StatPoint>,
    mode: StatMode,
    unitSystem: UnitSystem
) {
    val textMeasurer = rememberTextMeasurer()
    val xDateFmt = SimpleDateFormat("dd/MM", Locale.getDefault())
    val accentColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background

    Canvas(modifier = Modifier.fillMaxSize()) {
        val padLeft = 52.dp.toPx()
        val padRight = 8.dp.toPx()
        val padTop = 16.dp.toPx()
        val padBottom = 28.dp.toPx()

        val chartLeft = padLeft
        val chartRight = size.width - padRight
        val chartTop = padTop
        val chartBottom = size.height - padBottom

        val displayPoints = points.map { p ->
            if (mode == StatMode.REPS) p else p.copy(value = p.value.toDisplayWeight(unitSystem))
        }

        val values = displayPoints.map { it.value }
        val minVal = values.min()
        val maxVal = values.max()
        val rawRange = maxVal - minVal
        val range = if (rawRange == 0f) 1f else rawRange
        val paddedMin = minVal - range * 0.1f
        val paddedMax = maxVal + range * 0.1f
        val paddedRange = paddedMax - paddedMin

        fun valueToY(v: Float): Float =
            chartBottom - ((v - paddedMin) / paddedRange) * (chartBottom - chartTop)

        fun indexToX(i: Int): Float =
            if (displayPoints.size == 1) (chartLeft + chartRight) / 2f
            else chartLeft + (i.toFloat() / (displayPoints.size - 1).toFloat()) * (chartRight - chartLeft)

        val gridCount = 4
        val yLabelStyle = TextStyle(color = TextSecondary, fontSize = 10.sp)

        for (i in 0 until gridCount) {
            val fraction = i.toFloat() / (gridCount - 1).toFloat()
            val y = chartTop + fraction * (chartBottom - chartTop)
            drawLine(
                color = Outline.copy(alpha = 0.5f),
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 1.dp.toPx()
            )

            val gridValue = paddedMax - fraction * paddedRange
            val gridLabel = when (mode) {
                StatMode.WEIGHT -> if (gridValue % 1f == 0f) "${gridValue.toInt()}" else "%.1f".format(gridValue)
                StatMode.REPS   -> "${gridValue.toInt()}"
                StatMode.VOLUME -> "%.0f".format(gridValue)
            }
            val measured = textMeasurer.measure(gridLabel, yLabelStyle)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = chartLeft - measured.size.width - 6.dp.toPx(),
                    y = y - measured.size.height / 2f
                )
            )
        }

        if (displayPoints.size > 1) {
            val fillPath = Path().apply {
                moveTo(indexToX(0), valueToY(displayPoints[0].value))
                for (i in 1 until displayPoints.size) lineTo(indexToX(i), valueToY(displayPoints[i].value))
                lineTo(indexToX(displayPoints.size - 1), chartBottom)
                lineTo(indexToX(0), chartBottom)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(accentColor.copy(alpha = 0.25f), accentColor.copy(alpha = 0f)),
                    startY = chartTop,
                    endY = chartBottom
                )
            )

            val linePath = Path().apply {
                moveTo(indexToX(0), valueToY(displayPoints[0].value))
                for (i in 1 until displayPoints.size) lineTo(indexToX(i), valueToY(displayPoints[i].value))
            }
            drawPath(
                path = linePath,
                color = accentColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        displayPoints.forEachIndexed { i, point ->
            val cx = indexToX(i)
            val cy = valueToY(point.value)

            if (i == displayPoints.size - 1) {
                drawCircle(color = accentColor, radius = 6.dp.toPx(), center = Offset(cx, cy))
                drawCircle(color = backgroundColor, radius = 3.dp.toPx(), center = Offset(cx, cy))
            } else {
                drawCircle(color = SurfaceHigh, radius = 4.dp.toPx(), center = Offset(cx, cy))
                drawCircle(
                    color = accentColor,
                    radius = 4.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5f)
                )
            }
        }

        val xLabelStyle = TextStyle(color = TextSecondary, fontSize = 10.sp)
        val totalPoints = displayPoints.size
        val maxLabels = 5
        val labelIndices = mutableSetOf<Int>()

        if (totalPoints <= maxLabels) {
            for (i in 0 until totalPoints) labelIndices.add(i)
        } else {
            for (step in 0 until maxLabels) {
                labelIndices.add((step.toFloat() / (maxLabels - 1) * (totalPoints - 1)).toInt())
            }
            labelIndices.add(totalPoints - 1)
        }

        labelIndices.forEach { i ->
            val label = xDateFmt.format(displayPoints[i].dateMs)
            val measured = textMeasurer.measure(label, xLabelStyle)
            val cx = indexToX(i)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = (cx - measured.size.width / 2f).coerceIn(0f, size.width - measured.size.width),
                    y = chartBottom + 6.dp.toPx()
                )
            )
        }
    }
}

private fun Long.isToday(): Boolean {
    val today = Calendar.getInstance()
    val pointDay = Calendar.getInstance().apply { timeInMillis = this@isToday }
    return today.get(Calendar.YEAR) == pointDay.get(Calendar.YEAR) &&
           today.get(Calendar.DAY_OF_YEAR) == pointDay.get(Calendar.DAY_OF_YEAR)
}
