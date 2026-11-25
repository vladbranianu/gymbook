package com.gymable.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymable.data.local.entity.Gym
import com.gymable.data.local.entity.MuscleGroup
import com.gymable.data.repository.ExerciseRepository
import com.gymable.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddExerciseUiState(
    val selectedGymId: Long? = null,
    val selectedMuscleGroupId: Long? = null,
    val exerciseName: String = "",
    val isUnilateral: Boolean = false,
    val saving: Boolean = false,
    val savedExerciseId: Long? = null,
    val error: String? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class AddExerciseViewModel @Inject constructor(
    private val gymRepository: GymRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddExerciseUiState())
    val uiState: StateFlow<AddExerciseUiState> = _uiState.asStateFlow()

    private val selectedGymIdFlow = MutableStateFlow<Long?>(null)

    val gyms: StateFlow<List<Gym>> = gymRepository.observeGyms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val muscleGroups: StateFlow<List<MuscleGroup>> = selectedGymIdFlow
        .flatMapLatest { gymId ->
            if (gymId == null) flowOf(emptyList()) else gymRepository.observeMuscleGroups(gymId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectGym(gymId: Long) {
        selectedGymIdFlow.value = gymId
        _uiState.update { it.copy(selectedGymId = gymId, selectedMuscleGroupId = null) }
    }

    fun selectMuscleGroup(muscleGroupId: Long) = _uiState.update {
        it.copy(selectedMuscleGroupId = muscleGroupId)
    }

    fun setExerciseName(name: String) = _uiState.update { it.copy(exerciseName = name) }

    fun setIsUnilateral(value: Boolean) = _uiState.update { it.copy(isUnilateral = value) }

    fun createGym(name: String, onCreated: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = gymRepository.addGym(name.trim())
        selectedGymIdFlow.value = id
        _uiState.update { it.copy(selectedGymId = id, selectedMuscleGroupId = null) }
        onCreated(id)
    }

    fun createMuscleGroup(name: String, onCreated: (Long) -> Unit = {}) = viewModelScope.launch {
        val gymId = _uiState.value.selectedGymId ?: run {
            _uiState.update { it.copy(error = "Select a gym first") }
            return@launch
        }
        val id = gymRepository.addMuscleGroup(name.trim(), gymId)
        _uiState.update { it.copy(selectedMuscleGroupId = id) }
        onCreated(id)
    }

    fun saveExercise() {
        val state = _uiState.value
        val muscleGroupId = state.selectedMuscleGroupId ?: run {
            _uiState.update { it.copy(error = "Select a muscle group first") }
            return
        }
        if (state.exerciseName.isBlank()) {
            _uiState.update { it.copy(error = "Exercise name cannot be empty") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null) }
            val id = exerciseRepository.addExercise(state.exerciseName.trim(), muscleGroupId, state.isUnilateral)
            _uiState.update { it.copy(saving = false, savedExerciseId = id) }
        }
    }

    fun deselectGym() {
        selectedGymIdFlow.value = null
        _uiState.update { it.copy(selectedGymId = null, selectedMuscleGroupId = null) }
    }

    fun deselectMuscleGroup() = _uiState.update { it.copy(selectedMuscleGroupId = null) }

    fun consumeSavedEvent() = _uiState.update { it.copy(savedExerciseId = null) }

    fun consumeError() = _uiState.update { it.copy(error = null) }

    fun reset() {
        selectedGymIdFlow.value = null
        _uiState.update { AddExerciseUiState() }
    }
}
