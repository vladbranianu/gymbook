package com.gymable.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymable.data.local.entity.Exercise
import com.gymable.ui.components.AppDropdown
import com.gymable.ui.components.AppTextField
import com.gymable.ui.components.InfoCard
import com.gymable.ui.components.KeepScreenOn
import com.gymable.ui.components.PrimaryButton
import com.gymable.ui.components.ScreenScaffold
import com.gymable.ui.components.SecondaryButton
import com.gymable.ui.components.SectionLabel
import com.gymable.ui.main.MainViewModel
import com.gymable.ui.theme.LocalUnitSystem
import com.gymable.ui.theme.label
import com.gymable.ui.theme.toDisplayWeight
import com.gymable.ui.theme.toStorageKg
import com.gymable.data.repository.SetData
import com.gymable.ui.workout.DraftSetInput
import com.gymable.ui.workout.WorkoutViewModel
import kotlinx.coroutines.delay

@Composable
fun NewWorkoutScreen(
    onEndWorkout: () -> Unit,
    onBack: () -> Unit,
    onAddExercise: () -> Unit,
    workoutViewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
    selectionViewModel: MainViewModel = hiltViewModel()
) {
    val gyms by selectionViewModel.gyms.collectAsStateWithLifecycle()
    val selectedGym by selectionViewModel.selectedGym.collectAsStateWithLifecycle()
    val muscleGroups by selectionViewModel.muscleGroups.collectAsStateWithLifecycle()
    val selectedMuscleGroup by selectionViewModel.selectedMuscleGroup.collectAsStateWithLifecycle()
    val exercises by selectionViewModel.exercises.collectAsStateWithLifecycle()
    val workoutState by workoutViewModel.uiState.collectAsStateWithLifecycle()

    KeepScreenOn()

    val unit = LocalUnitSystem.current

    var selectedExercise by remember {
        mutableStateOf(
            if (workoutState.draftGymId != null && workoutState.draftExerciseId != null)
                workoutState.exercise
            else null
        )
    }
    var pendingEnd by remember { mutableStateOf(false) }

    var setCount by remember { mutableIntStateOf(workoutState.draftSetCount) }
    var setInputs by remember {
        mutableStateOf(workoutState.draftSets.ifEmpty { List(workoutState.draftSetCount) { DraftSetInput() } })
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    val isUnilateral = workoutState.exercise?.isUnilateral == true

    fun clearSetInputs() {
        setInputs = List(setCount) { DraftSetInput() }
    }

    fun hasAllReps(): Boolean = if (isUnilateral) {
        setInputs.all { it.reps.isNotBlank() && it.repsRight.isNotBlank() }
    } else {
        setInputs.all { it.reps.isNotBlank() }
    }

    fun resolvedSets(): List<SetData> = setInputs.mapIndexed { i, input ->
        val hint = workoutState.setHints.getOrNull(i)
        val resolvedReps = input.reps.toIntOrNull() ?: hint?.reps ?: 0
        val resolvedWeight = input.weight.toFloatOrNull()?.toStorageKg(unit) ?: hint?.weight ?: 0f
        val resolvedWeightRight = if (isUnilateral) {
            input.weightRight.toFloatOrNull()?.toStorageKg(unit) ?: hint?.weightRight ?: 0f
        } else null
        val resolvedRepsRight = if (isUnilateral) {
            input.repsRight.toIntOrNull() ?: hint?.repsRight ?: 0
        } else null
        SetData(resolvedReps, resolvedWeight, resolvedWeightRight, resolvedRepsRight)
    }

    fun beginExercise(exercise: Exercise) {
        selectedExercise = exercise
        clearSetInputs()
        workoutViewModel.load(exercise.id)
    }

    LaunchedEffect(Unit) {
        val draft = workoutState
        if (draft.draftGymId != null) {
            selectionViewModel.selectGym(draft.draftGymId)
            if (draft.draftMuscleGroupId != null) {
                selectionViewModel.selectMuscleGroup(draft.draftMuscleGroupId)
            }
        }
    }

    val latestGymId = rememberUpdatedState(selectedGym?.id)
    val latestMuscleGroupId = rememberUpdatedState(selectedMuscleGroup?.id)
    val latestExerciseId = rememberUpdatedState(selectedExercise?.id)
    val latestSetCount = rememberUpdatedState(setCount)
    val latestSetInputs = rememberUpdatedState(setInputs)
    val latestExercise = rememberUpdatedState(selectedExercise)

    DisposableEffect(Unit) {
        onDispose {
            if (latestExercise.value != null || latestGymId.value != null) {
                workoutViewModel.saveDraft(
                    gymId = latestGymId.value,
                    muscleGroupId = latestMuscleGroupId.value,
                    exerciseId = latestExerciseId.value,
                    setCount = latestSetCount.value,
                    sets = latestSetInputs.value
                )
            }
        }
    }

    // When hints load from the last session, mirror that session's set count as default.
    // Skip if we're restoring a draft (draftSets is non-empty).
    LaunchedEffect(workoutState.setHints) {
        if (workoutState.setHints.isNotEmpty() && workoutState.draftSets.isEmpty()) {
            val count = workoutState.setHints.size
            setCount = count
            setInputs = List(count) { DraftSetInput() }
        }
    }

    // Only used when End Workout needs to wait for the save coroutine to finish.
    LaunchedEffect(workoutState.saved) {
        if (workoutState.saved) {
            workoutViewModel.consumeSavedEvent()
            if (pendingEnd) {
                pendingEnd = false
                selectionViewModel.clearGymSelection()
                onEndWorkout()
            }
        }
    }

    val anySetEntered = setInputs.any { it.reps.isNotBlank() || it.weight.isNotBlank() || it.weightRight.isNotBlank() || it.repsRight.isNotBlank() }
    val record = workoutState.record

    ScreenScaffold(
        title = "New Workout",
        onBack = onBack,
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExercise,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add new exercise")
            }
        },
        bottomBar = {
            WorkoutActionBar(
                nextEnabled = selectedExercise != null && !pendingEnd,
                endEnabled = !pendingEnd && (workoutState.sessionLog.isNotEmpty() || selectedExercise != null),
                validationError = validationError,
                onNext = {
                    if (selectedExercise != null && !hasAllReps()) {
                        validationError = "Enter reps for each set or reduce the number of sets to continue."
                    } else {
                        validationError = null
                        workoutViewModel.saveWorkout(resolvedSets())
                        selectionViewModel.clearMuscleGroupSelection()
                        selectedExercise = null
                        clearSetInputs()
                    }
                },
                onEnd = {
                    if (selectedExercise != null) {
                        if (!hasAllReps()) {
                            validationError = "Enter reps for each set or reduce the number of sets to continue."
                        } else {
                            validationError = null
                            pendingEnd = true
                            workoutViewModel.saveWorkout(resolvedSets())
                        }
                    } else {
                        selectionViewModel.clearGymSelection()
                        onEndWorkout()
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionLabel("Where")
                AppDropdown(
                    label = "Gym",
                    items = gyms,
                    selected = selectedGym,
                    itemLabel = { it.name },
                    onSelect = { gym ->
                        selectionViewModel.selectGym(gym.id)
                        selectedExercise = null
                        clearSetInputs()
                    },
                    emptyHint = "No gyms yet — add one first"
                )
                AnimatedVisibility(
                    visible = selectedGym != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    AppDropdown(
                        label = "Muscle Group",
                        items = muscleGroups,
                        selected = selectedMuscleGroup,
                        itemLabel = { it.name },
                        onSelect = { mg ->
                            selectionViewModel.selectMuscleGroup(mg.id)
                            selectedExercise = null
                            clearSetInputs()
                        },
                        emptyHint = "No muscle groups for this gym"
                    )
                }
                AnimatedVisibility(
                    visible = selectedMuscleGroup != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    AppDropdown(
                        label = "Exercise",
                        items = exercises,
                        selected = selectedExercise,
                        itemLabel = { it.name },
                        onSelect = { ex -> beginExercise(ex) },
                        emptyHint = "No exercises for this muscle group"
                    )
                }
            }

            AnimatedVisibility(
                visible = selectedExercise != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionLabel("Today's Sets")

                    SetCountPicker(
                        count = setCount,
                        onCountChange = { newCount ->
                            setCount = newCount
                            setInputs = List(newCount) { i -> setInputs.getOrElse(i) { DraftSetInput() } }
                            validationError = null
                        }
                    )

                    setInputs.forEachIndexed { i, input ->
                        val hint = workoutState.setHints.getOrNull(i)
                        SetRow(
                            title = "Set ${i + 1}",
                            reps = input.reps,
                            weight = input.weight,
                            weightRight = input.weightRight,
                            repsRight = input.repsRight,
                            repsPlaceholder = hint?.reps?.toString() ?: "reps",
                            weightPlaceholder = hint?.weight?.let { w -> trimFloat(w.toDisplayWeight(unit)) } ?: unit.label(),
                            weightRightPlaceholder = hint?.weightRight?.let { w -> trimFloat(w.toDisplayWeight(unit)) } ?: unit.label(),
                            repsRightPlaceholder = hint?.repsRight?.toString() ?: "reps",
                            isUnilateral = isUnilateral,
                            onRepsChange = { v ->
                                val updated = setInputs.toMutableList()
                                updated[i] = input.copy(reps = v.filterDigits())
                                setInputs = updated
                                validationError = null
                            },
                            onWeightChange = { v ->
                                val updated = setInputs.toMutableList()
                                updated[i] = input.copy(weight = v.filterDecimal())
                                setInputs = updated
                            },
                            onWeightRightChange = { v ->
                                val updated = setInputs.toMutableList()
                                updated[i] = input.copy(weightRight = v.filterDecimal())
                                setInputs = updated
                            },
                            onRepsRightChange = { v ->
                                val updated = setInputs.toMutableList()
                                updated[i] = input.copy(repsRight = v.filterDigits())
                                setInputs = updated
                                validationError = null
                            }
                        )
                    }

                    InfoCard(
                        title = "Record",
                        value = if (record != null) "${record.maxReps} reps @ ${trimFloat(record.maxWeight.toDisplayWeight(unit))} ${unit.label()}"
                        else "No record yet",
                        icon = Icons.Rounded.EmojiEvents
                    )

                    if (!anySetEntered) {
                        Text(
                            text = "Tip: leave a field empty to keep last session's value.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            workoutState.error?.let { err ->
                LaunchedEffect(err) { delay(3000); workoutViewModel.consumeError() }
                InfoCard(title = "Error", value = err, icon = Icons.Rounded.Stop)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SetCountPicker(
    count: Int,
    onCountChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Number of sets",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = { if (count > 1) onCountChange(count - 1) },
            enabled = count > 1,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Remove,
                contentDescription = "Remove set",
                tint = if (count > 1) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        IconButton(
            onClick = { if (count < 10) onCountChange(count + 1) },
            enabled = count < 10,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add set",
                tint = if (count < 10) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun SetRow(
    title: String,
    reps: String,
    weight: String,
    weightRight: String,
    repsRight: String,
    repsPlaceholder: String,
    weightPlaceholder: String,
    weightRightPlaceholder: String,
    repsRightPlaceholder: String,
    isUnilateral: Boolean,
    onRepsChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onWeightRightChange: (String) -> Unit,
    onRepsRightChange: (String) -> Unit
) {
    val unit = LocalUnitSystem.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (isUnilateral) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "L",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(16.dp)
                    )
                    AppTextField(
                        value = reps,
                        onValueChange = onRepsChange,
                        label = "Reps",
                        placeholder = repsPlaceholder,
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                        modifier = Modifier.weight(1f)
                    )
                    AppTextField(
                        value = weight,
                        onValueChange = onWeightChange,
                        label = "Weight (${unit.label()})",
                        placeholder = weightPlaceholder,
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "R",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(16.dp)
                    )
                    AppTextField(
                        value = repsRight,
                        onValueChange = onRepsRightChange,
                        label = "Reps",
                        placeholder = repsRightPlaceholder,
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                        modifier = Modifier.weight(1f)
                    )
                    AppTextField(
                        value = weightRight,
                        onValueChange = onWeightRightChange,
                        label = "Weight (${unit.label()})",
                        placeholder = weightRightPlaceholder,
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(
                    value = reps,
                    onValueChange = onRepsChange,
                    label = "Reps",
                    placeholder = repsPlaceholder,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.weight(1f)
                )
                AppTextField(
                    value = weight,
                    onValueChange = onWeightChange,
                    label = "Weight (${unit.label()})",
                    placeholder = weightPlaceholder,
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WorkoutActionBar(
    nextEnabled: Boolean,
    endEnabled: Boolean,
    validationError: String?,
    onNext: () -> Unit,
    onEnd: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (validationError != null) {
                Text(
                    text = validationError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            PrimaryButton(
                text = "Next Exercise",
                onClick = onNext,
                enabled = nextEnabled,
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                modifier = Modifier.fillMaxWidth()
            )
            SecondaryButton(
                text = "End Workout",
                onClick = onEnd,
                enabled = endEnabled,
                icon = Icons.Rounded.Stop,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun String.filterDigits(): String = filter { it.isDigit() }.take(4)

private fun String.filterDecimal(): String {
    val cleaned = filter { it.isDigit() || it == '.' }
    val firstDot = cleaned.indexOf('.')
    val result = if (firstDot < 0) cleaned
    else cleaned.substring(0, firstDot + 1) + cleaned.substring(firstDot + 1).filter { it != '.' }
    return result.take(7)
}

private fun trimFloat(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else value.toString()
