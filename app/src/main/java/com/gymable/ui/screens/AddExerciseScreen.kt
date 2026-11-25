package com.gymable.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymable.ui.components.AppTextField
import com.gymable.ui.components.InfoCard
import com.gymable.ui.components.PrimaryButton
import com.gymable.ui.components.ScreenScaffold
import com.gymable.ui.components.SecondaryButton
import com.gymable.ui.components.SectionLabel
import com.gymable.ui.components.SelectableListItem
import com.gymable.ui.components.StepProgress
import com.gymable.ui.exercise.AddExerciseViewModel
import kotlinx.coroutines.delay

private enum class StepMode { ADD_NEW, SELECT }

@Composable
fun AddExerciseScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddExerciseViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val gyms by viewModel.gyms.collectAsStateWithLifecycle()
    val muscleGroups by viewModel.muscleGroups.collectAsStateWithLifecycle()

    val currentStep = when {
        state.selectedGymId == null -> 1
        state.selectedMuscleGroupId == null -> 2
        else -> 3
    }

    // Per-step mode: steps 1 & 2 open directly in SELECT; step 3 goes straight to ADD_NEW.
    var mode by remember { mutableStateOf(StepMode.SELECT) }
    var newName by remember { mutableStateOf("") }
    LaunchedEffect(currentStep) {
        mode = if (currentStep == 3) StepMode.ADD_NEW else StepMode.SELECT
        newName = ""
    }

    // Reset everything when entering the screen.
    LaunchedEffect(Unit) { viewModel.reset() }

    // Navigate away once the exercise is persisted.
    LaunchedEffect(state.savedExerciseId) {
        if (state.savedExerciseId != null) {
            delay(450) // let the success card breathe
            viewModel.consumeSavedEvent()
            onClose()
        }
    }

    val onBack: () -> Unit = when (currentStep) {
        2 -> viewModel::deselectGym
        3 -> viewModel::deselectMuscleGroup
        else -> onClose
    }

    ScreenScaffold(
        title = "Add New Exercise",
        modifier = modifier,
        onBack = onBack,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                SecondaryButton(
                    text = "Cancel",
                    onClick = onClose,
                    icon = Icons.Rounded.Close,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            StepProgress(currentStep = currentStep, totalSteps = 3)

            Text(
                text = when (currentStep) {
                    1 -> "Step 1 of 3 — Gym"
                    2 -> "Step 2 of 3 — Muscle group"
                    else -> "Step 3 of 3 — Exercise name"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            AnimatedContent(
                targetState = currentStep to mode,
                transitionSpec = {
                    (slideInHorizontally(tween(260)) { it / 3 } + fadeIn(tween(260))) togetherWith
                        (slideOutHorizontally(tween(260)) { -it / 4 } + fadeOut(tween(260)))
                },
                label = "wizardStep"
            ) { (step, m) ->
                when (step) {
                    1 -> GymStep(
                        mode = m,
                        gymNames = gyms.map { it.id to it.name },
                        newName = newName,
                        onModeChange = { mode = it; newName = "" },
                        onNewNameChange = { newName = it },
                        onConfirmNew = {
                            val name = newName.trim()
                            if (name.isNotEmpty()) viewModel.createGym(name)
                        },
                        onSelect = { id -> viewModel.selectGym(id) }
                    )

                    2 -> ChoiceStep(
                        mode = m,
                        entityLabelSingular = "muscle group",
                        existing = muscleGroups.map { it.id to it.name },
                        newName = newName,
                        onModeChange = { mode = it; newName = "" },
                        onNewNameChange = { newName = it },
                        onConfirmNew = {
                            val name = newName.trim()
                            if (name.isNotEmpty()) viewModel.createMuscleGroup(name)
                        },
                        onSelect = { id -> viewModel.selectMuscleGroup(id) }
                    )

                    else -> ExerciseNameStep(
                        name = state.exerciseName,
                        isUnilateral = state.isUnilateral,
                        saving = state.saving,
                        saved = state.savedExerciseId != null,
                        onNameChange = viewModel::setExerciseName,
                        onIsUnilateralChange = viewModel::setIsUnilateral,
                        onSave = viewModel::saveExercise
                    )
                }
            }

            state.error?.let { err ->
                LaunchedEffect(err) { delay(3000); viewModel.consumeError() }
                InfoCard(title = "Error", value = err, icon = Icons.AutoMirrored.Rounded.FormatListBulleted)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun GymStep(
    mode: StepMode,
    gymNames: List<Pair<Long, String>>,
    newName: String,
    onModeChange: (StepMode) -> Unit,
    onNewNameChange: (String) -> Unit,
    onConfirmNew: () -> Unit,
    onSelect: (Long) -> Unit
) = ChoiceStep(
    mode = mode,
    entityLabelSingular = "gym",
    existing = gymNames,
    newName = newName,
    onModeChange = onModeChange,
    onNewNameChange = onNewNameChange,
    onConfirmNew = onConfirmNew,
    onSelect = onSelect
)

@Composable
private fun ChoiceStep(
    mode: StepMode,
    entityLabelSingular: String,
    existing: List<Pair<Long, String>>,
    newName: String,
    onModeChange: (StepMode) -> Unit,
    onNewNameChange: (String) -> Unit,
    onConfirmNew: () -> Unit,
    onSelect: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        when (mode) {
            StepMode.SELECT -> {
                if (existing.isEmpty()) {
                    Text(
                        text = "No ${entityLabelSingular}s yet — add your first one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        existing.forEach { (id, name) ->
                            SelectableListItem(text = name, onClick = { onSelect(id) })
                        }
                    }
                }
                SecondaryButton(
                    text = "Add New ${entityLabelSingular.titleEachWord()}",
                    onClick = { onModeChange(StepMode.ADD_NEW) },
                    icon = Icons.Rounded.Add,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            StepMode.ADD_NEW -> {
                SectionLabel("New ${entityLabelSingular.titleEachWord()}")
                AppTextField(
                    value = newName,
                    onValueChange = onNewNameChange,
                    label = "${entityLabelSingular.titleEachWord()} name",
                    placeholder = "e.g. ${exampleFor(entityLabelSingular)}",
                    imeAction = ImeAction.Done,
                    onImeAction = onConfirmNew
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        text = "Back",
                        onClick = { onModeChange(StepMode.SELECT) },
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = "Confirm",
                        onClick = onConfirmNew,
                        enabled = newName.isNotBlank(),
                        icon = Icons.Rounded.Check,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseNameStep(
    name: String,
    isUnilateral: Boolean,
    saving: Boolean,
    saved: Boolean,
    onNameChange: (String) -> Unit,
    onIsUnilateralChange: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        SectionLabel("Exercise")
        AppTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Exercise name",
            placeholder = "e.g. Bench Press",
            enabled = !saving && !saved,
            imeAction = ImeAction.Done,
            onImeAction = onSave
        )
        SectionLabel("Movement Type")
        MovementTypePicker(isUnilateral = isUnilateral, onSelect = onIsUnilateralChange)
        if (saved) {
            InfoCard(
                title = "Saved",
                value = "Exercise added successfully",
                icon = Icons.Rounded.Check,
                accent = true
            )
        }
        PrimaryButton(
            text = if (saving) "Saving…" else "Save Exercise",
            onClick = onSave,
            enabled = name.isNotBlank() && !saving && !saved,
            icon = Icons.Rounded.Check,
            large = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MovementTypePicker(isUnilateral: Boolean, onSelect: (Boolean) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        listOf(false to "Bilateral", true to "Unilateral").forEach { (value, label) ->
            val selected = isUnilateral == value
            Surface(
                onClick = { onSelect(value) },
                shape = RoundedCornerShape(10.dp),
                color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 14.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun String.titleEachWord(): String =
    split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

private fun exampleFor(entity: String): String = when (entity) {
    "gym" -> "Iron Temple"
    "muscle group" -> "Chest"
    else -> "Bench Press"
}
