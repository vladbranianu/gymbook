package com.gymable.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymable.data.local.dao.WorkoutHistorySetRow
import com.gymable.ui.components.ScreenScaffold
import com.gymable.ui.history.SessionWithSets
import com.gymable.ui.history.WorkoutHistoryViewModel
import com.gymable.ui.theme.CardBorder
import com.gymable.ui.theme.ErrorRed
import com.gymable.ui.theme.LocalUnitSystem
import com.gymable.ui.theme.RecordGold
import com.gymable.ui.theme.SurfaceHigh
import com.gymable.ui.theme.TextSecondary
import com.gymable.ui.theme.UnitSystem
import com.gymable.ui.theme.label
import com.gymable.ui.theme.toDisplayWeight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutHistoryViewModel = hiltViewModel()
) {
    val currentDay by viewModel.currentDay.collectAsStateWithLifecycle()
    val canGoPrev by viewModel.canGoPrev.collectAsStateWithLifecycle()
    val canGoNext by viewModel.canGoNext.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<SessionWithSets?>(null) }

    pendingDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete session?") },
            text = {
                Text(
                    "This will permanently remove the ${session.session.exerciseName} session. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(session.session.sessionId)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    ScreenScaffold(
        title = "History",
        onBack = onBack,
        modifier = modifier
    ) {
        if (currentDay == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "No workouts yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val (dayMs, sessions) = currentDay!!
            Column(modifier = Modifier.fillMaxSize()) {
                DaySelector(
                    dayMs = dayMs,
                    canGoPrev = canGoPrev,
                    canGoNext = canGoNext,
                    onPrev = viewModel::prevDay,
                    onNext = viewModel::nextDay
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        DayHeadsUpRow(
                            muscleGroupSummary = muscleGroupSummary(sessions),
                            rating = sessions.first().session.rating
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    item {
                        DayCard(
                            sessions = sessions,
                            onLongPressSession = { pendingDelete = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySelector(
    dayMs: Long,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val label = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(dayMs))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev, enabled = canGoPrev) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Previous day",
                tint = if (canGoPrev) MaterialTheme.colorScheme.onBackground else TextSecondary
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Next day",
                tint = if (canGoNext) MaterialTheme.colorScheme.onBackground else TextSecondary
            )
        }
    }
}

@Composable
private fun DayHeadsUpRow(muscleGroupSummary: String, rating: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = muscleGroupSummary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        RatingBadge(rating = rating)
    }
}

private fun muscleGroupSummary(sessions: List<SessionWithSets>): String {
    val groups = sessions.map { it.session.muscleGroupName }.distinct()
    return when (groups.size) {
        0 -> ""
        1 -> groups[0]
        else -> groups.dropLast(1).joinToString(", ") + " & " + groups.last()
    }
}

@Composable
private fun RatingBadge(rating: Int) {
    val bgColor = when {
        rating >= 8 -> MaterialTheme.colorScheme.primary
        rating >= 5 -> RecordGold
        else        -> ErrorRed
    }
    val textColor = when {
        rating >= 8 -> MaterialTheme.colorScheme.onPrimary
        else        -> SurfaceHigh
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = "$rating/10",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCard(
    sessions: List<SessionWithSets>,
    onLongPressSession: (SessionWithSets) -> Unit
) {
    val unit = LocalUnitSystem.current
    val totalVolume = sessions.sumOf { it.session.totalVolume.toDouble() }.toFloat()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            sessions.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                }
                SessionRow(
                    item = item,
                    unit = unit,
                    onLongPress = { onLongPressSession(item) }
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total volume",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = formatVolume(totalVolume, unit),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionRow(item: SessionWithSets, unit: UnitSystem, onLongPress: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = item.session.exerciseName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${item.session.muscleGroupName} · ${item.session.gymName}",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        item.sets.forEachIndexed { i, set ->
            HistorySetLine(label = "Set ${i + 1}", set = set, unit = unit)
        }
    }
}

@Composable
private fun HistorySetLine(label: String, set: WorkoutHistorySetRow, unit: UnitSystem) {
    fun Float.fmt(): String {
        val d = toDisplayWeight(unit)
        return if (d % 1f == 0f) "${d.toInt()} ${unit.label()}" else "%.1f ${unit.label()}".format(d)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 4.dp, top = 2.dp)
        )
        if (set.weightRight != null) {
            val dl = set.weight.toDisplayWeight(unit)
            val dr = set.weightRight.toDisplayWeight(unit)
            val fl = if (dl % 1f == 0f) "${dl.toInt()}" else "%.1f".format(dl)
            val fr = if (dr % 1f == 0f) "${dr.toInt()}" else "%.1f".format(dr)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "L: ${set.reps} × $fl ${unit.label()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "R: ${set.repsRight ?: set.reps} × $fr ${unit.label()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Text(
                text = "${set.reps} × ${set.weight.fmt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatVolume(volume: Float, unit: UnitSystem): String {
    val v = volume.toDisplayWeight(unit)
    return if (v % 1f == 0f) "${v.toInt()} ${unit.label()}" else "%.1f ${unit.label()}".format(v)
}
