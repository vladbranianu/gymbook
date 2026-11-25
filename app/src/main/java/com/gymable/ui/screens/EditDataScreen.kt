package com.gymable.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymable.data.local.entity.Exercise
import com.gymable.data.local.entity.Gym
import com.gymable.data.local.entity.MuscleGroup
import com.gymable.ui.components.AppTextField
import com.gymable.ui.components.ScreenScaffold
import com.gymable.ui.edit.DeleteTarget
import com.gymable.ui.edit.EditDataUiState
import com.gymable.ui.edit.EditDataViewModel
import com.gymable.ui.edit.EditingItem
import com.gymable.ui.edit.ItemType
import com.gymable.ui.theme.CardBorder
import com.gymable.ui.theme.ErrorRed
import com.gymable.ui.theme.TextSecondary

@Composable
fun EditDataScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditDataViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DeleteDialog(target = state.deleteTarget, onConfirm = viewModel::confirmDelete, onDismiss = viewModel::cancelDelete)
    EditDialog(item = state.editingItem, onSave = viewModel::saveEdit, onDismiss = viewModel::cancelEdit)

    ScreenScaffold(title = "Edit Exercises & Gyms", onBack = onBack, modifier = modifier) {
        if (state.gyms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No gyms found.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                state.gyms.forEach { gym ->
                    GymSection(
                        gym = gym,
                        state = state,
                        onToggle = { viewModel.toggleGym(gym.id) },
                        onEdit = { viewModel.startEdit(gym.id, ItemType.GYM, gym.name) },
                        onDelete = { viewModel.requestDelete(DeleteTarget.GymTarget(gym)) },
                        onToggleMg = { viewModel.toggleMuscleGroup(it) },
                        onEditMg = { mg -> viewModel.startEdit(mg.id, ItemType.MUSCLE_GROUP, mg.name) },
                        onDeleteMg = { mg -> viewModel.requestDelete(DeleteTarget.MuscleGroupTarget(mg)) },
                        onEditEx = { ex -> viewModel.startEdit(ex.id, ItemType.EXERCISE, ex.name) },
                        onDeleteEx = { ex -> viewModel.requestDelete(DeleteTarget.ExerciseTarget(ex)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GymSection(
    gym: Gym,
    state: EditDataUiState,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleMg: (Long) -> Unit,
    onEditMg: (MuscleGroup) -> Unit,
    onDeleteMg: (MuscleGroup) -> Unit,
    onEditEx: (Exercise) -> Unit,
    onDeleteEx: (Exercise) -> Unit
) {
    val expanded = gym.id in state.expandedGymIds
    val muscleGroups = state.muscleGroups.filter { it.gymId == gym.id }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            DataRow(
                name = gym.name,
                startPadding = 0.dp,
                expandIcon = if (muscleGroups.isNotEmpty()) {
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore
                } else null,
                onToggle = onToggle,
                onEdit = onEdit,
                onDelete = onDelete,
                nameStyle = MaterialTheme.typography.bodyMedium,
                nameFontWeight = FontWeight.SemiBold
            )

            AnimatedVisibility(visible = expanded) {
                Column {
                    if (muscleGroups.isEmpty()) {
                        Text(
                            text = "No muscle groups",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 40.dp, bottom = 10.dp)
                        )
                    } else {
                        muscleGroups.forEach { mg ->
                            MuscleGroupSection(
                                mg = mg,
                                state = state,
                                onToggle = { onToggleMg(mg.id) },
                                onEdit = { onEditMg(mg) },
                                onDelete = { onDeleteMg(mg) },
                                onEditEx = onEditEx,
                                onDeleteEx = onDeleteEx
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MuscleGroupSection(
    mg: MuscleGroup,
    state: EditDataUiState,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEditEx: (Exercise) -> Unit,
    onDeleteEx: (Exercise) -> Unit
) {
    val expanded = mg.id in state.expandedMuscleGroupIds
    val exercises = state.exercises.filter { it.muscleGroupId == mg.id }

    Column {
        HorizontalDivider(color = CardBorder, modifier = Modifier.padding(start = 16.dp))
        DataRow(
            name = mg.name,
            startPadding = 16.dp,
            expandIcon = if (exercises.isNotEmpty()) {
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore
            } else null,
            onToggle = onToggle,
            onEdit = onEdit,
            onDelete = onDelete,
            nameStyle = MaterialTheme.typography.bodyMedium,
            nameFontWeight = FontWeight.Normal
        )

        AnimatedVisibility(visible = expanded) {
            Column {
                if (exercises.isEmpty()) {
                    Text(
                        text = "No exercises",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 56.dp, bottom = 10.dp)
                    )
                } else {
                    exercises.forEach { ex ->
                        HorizontalDivider(color = CardBorder, modifier = Modifier.padding(start = 32.dp))
                        DataRow(
                            name = ex.name,
                            startPadding = 32.dp,
                            expandIcon = null,
                            onToggle = {},
                            onEdit = { onEditEx(ex) },
                            onDelete = { onDeleteEx(ex) },
                            nameStyle = MaterialTheme.typography.bodySmall,
                            nameFontWeight = FontWeight.Normal,
                            nameColor = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DataRow(
    name: String,
    startPadding: Dp,
    expandIcon: ImageVector?,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    nameStyle: TextStyle,
    nameFontWeight: FontWeight,
    nameColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (expandIcon != null) {
            IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = expandIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(36.dp))
        }

        Text(
            text = name,
            style = nameStyle,
            fontWeight = nameFontWeight,
            color = if (nameColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else nameColor,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = "Rename",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "Delete",
                tint = ErrorRed.copy(alpha = 0.75f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun EditDialog(item: EditingItem?, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    if (item == null) return

    var text by remember(item) { mutableStateOf(item.currentName) }
    val typeLabel = when (item.type) {
        ItemType.GYM -> "gym"
        ItemType.MUSCLE_GROUP -> "muscle group"
        ItemType.EXERCISE -> "exercise"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename $typeLabel") },
        text = {
            AppTextField(
                value = text,
                onValueChange = { text = it },
                label = "Name",
                onImeAction = { if (text.isNotBlank()) onSave(text.trim()) }
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(text.trim()) },
                enabled = text.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DeleteDialog(target: DeleteTarget?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    if (target == null) return

    val (title, message) = when (target) {
        is DeleteTarget.GymTarget -> Pair(
            "Delete \"${target.gym.name}\"?",
            "This will permanently delete the gym, all its muscle groups and exercises, and every workout session recorded under it. This cannot be undone."
        )
        is DeleteTarget.MuscleGroupTarget -> Pair(
            "Delete \"${target.mg.name}\"?",
            "This will permanently delete the muscle group, all its exercises, and every workout session recorded under it. This cannot be undone."
        )
        is DeleteTarget.ExerciseTarget -> Pair(
            "Delete \"${target.ex.name}\"?",
            "This will permanently delete the exercise and all workout sessions recorded for it. This cannot be undone."
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Text(message, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
