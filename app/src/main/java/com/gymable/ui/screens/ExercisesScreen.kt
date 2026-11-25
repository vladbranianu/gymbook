package com.gymable.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymable.data.local.dao.ExerciseOverviewRow
import com.gymable.ui.components.ScreenScaffold
import com.gymable.ui.components.SectionLabel
import com.gymable.ui.overview.ExerciseGroup
import com.gymable.ui.overview.OverviewViewModel
import com.gymable.ui.theme.LocalUnitSystem
import com.gymable.ui.theme.UnitSystem
import com.gymable.ui.theme.fmtWeight

@Composable
fun ExercisesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OverviewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = "My Exercises",
        modifier = modifier,
        onBack = onBack
    ) {
        when {
            state.loading -> Unit

            state.groups.isEmpty() -> EmptyExercises()

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                state.groups.forEach { group ->
                    item(key = "${group.gymName}|${group.muscleGroupName}") {
                        GroupHeader(group = group)
                    }
                    items(
                        items = group.exercises,
                        key = { it.exerciseId }
                    ) { exercise ->
                        ExerciseCard(
                            row = exercise,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun EmptyExercises() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "No exercises yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Add your first exercise to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GroupHeader(group: ExerciseGroup) {
    Column(modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)) {
        Text(
            text = group.gymName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        SectionLabel(group.muscleGroupName)
    }
}

@Composable
private fun ExerciseCard(
    row: ExerciseOverviewRow,
    modifier: Modifier = Modifier
) {
    val unit = LocalUnitSystem.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = row.exerciseName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            if (row.lastDate != null) {
                Text(
                    text = "Last workout: ${row.lastDate.toRelativeDate()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (row.lastSetCount > 0) {
                    val topWeightStr = row.lastTopWeight?.fmtWeight(unit)
                    Text(
                        text = "${row.lastSetCount} sets" + (if (topWeightStr != null) " · up to $topWeightStr" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Text(
                    text = "No workouts yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (row.recordMaxWeight != null && row.recordMaxReps != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Record: ${row.recordMaxWeight.fmtWeight(unit)} × ${row.recordMaxReps} reps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun Long.toRelativeDate(): String {
    val diffMs = System.currentTimeMillis() - this
    val days = diffMs / 86_400_000L
    return when {
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 7L  -> "$days days ago"
        days < 30L -> "${days / 7} weeks ago"
        else       -> "${days / 30} months ago"
    }
}
