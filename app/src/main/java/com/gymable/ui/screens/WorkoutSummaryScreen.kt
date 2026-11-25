package com.gymable.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymable.ui.components.KeepScreenOn
import com.gymable.ui.components.PrimaryButton
import com.gymable.ui.theme.CardBorder
import com.gymable.ui.components.ScreenScaffold
import com.gymable.ui.theme.LocalUnitSystem
import com.gymable.ui.theme.UnitSystem
import com.gymable.ui.theme.label
import com.gymable.ui.theme.toDisplayWeight
import com.gymable.data.repository.SetData
import com.gymable.data.repository.WorkoutLogEntry
import com.gymable.ui.workout.WorkoutViewModel

@Composable
fun WorkoutSummaryScreen(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutViewModel
) {
    KeepScreenOn()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionLog = uiState.sessionLog
    var selectedRating by remember { mutableStateOf(5) }
    val context = LocalContext.current

    val totalVolume = sessionLog.sumOf { it.volume().toDouble() }.toFloat()

    LaunchedEffect(uiState.showReviewRequest) {
        if (uiState.showReviewRequest) {
            (context as? Activity)?.let { viewModel.launchReviewFlow(it) }
        }
    }

    LaunchedEffect(uiState.workoutFinished) {
        if (uiState.workoutFinished) onConfirm()
    }

    ScreenScaffold(
        title = "Workout Summary",
        onBack = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "${sessionLog.size} exercise${if (sessionLog.size != 1) "s" else ""} logged",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            sessionLog.forEach { entry ->
                ExerciseSummaryCard(entry)
            }

            TotalVolumeCard(totalVolume = totalVolume)

            Spacer(Modifier.height(2.dp))

            RatingPicker(
                selected = selectedRating,
                onSelect = { selectedRating = it }
            )

            Spacer(Modifier.height(6.dp))

            PrimaryButton(
                text = "Confirm",
                onClick = { viewModel.applyRating(selectedRating) },
                icon = Icons.Rounded.Check,
                large = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TotalVolumeCard(totalVolume: Float) {
    val unit = LocalUnitSystem.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total Volume",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = formatVolume(totalVolume, unit),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun RatingPicker(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "How was it?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            (1..10).forEach { n ->
                val isSelected = n == selected
                Surface(
                    onClick = { onSelect(n) },
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(36.dp).weight(1f).padding(horizontal = 2.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$n",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseSummaryCard(entry: WorkoutLogEntry) {
    val unit = LocalUnitSystem.current
    val volume = entry.volume()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.exerciseName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatVolume(volume, unit),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                entry.sets.forEachIndexed { i, set ->
                    SetLine(label = "Set ${i + 1}", set = set)
                }
            }
        }
    }
}

@Composable
private fun SetLine(label: String, set: SetData) {
    val unit = LocalUnitSystem.current
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
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "R: ${set.repsRight ?: set.reps} × $fr ${unit.label()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Text(
                text = "${set.reps} × ${set.weight.fmt()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun WorkoutLogEntry.volume(): Float =
    sets.sumOf { set ->
        val leftVol = set.reps * set.weight
        val rightVol = (set.repsRight ?: set.reps) * (set.weightRight ?: 0f)
        (leftVol + rightVol).toDouble()
    }.toFloat()

private fun formatVolume(volume: Float, unit: UnitSystem): String {
    val v = volume.toDisplayWeight(unit)
    return if (v % 1f == 0f) "${v.toInt()} ${unit.label()}" else "%.1f ${unit.label()}".format(v)
}
