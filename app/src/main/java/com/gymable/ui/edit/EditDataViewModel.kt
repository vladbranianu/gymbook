package com.gymable.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymable.data.local.entity.Exercise
import com.gymable.data.local.entity.Gym
import com.gymable.data.local.entity.MuscleGroup
import com.gymable.data.repository.ExerciseRepository
import com.gymable.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DeleteTarget {
    data class GymTarget(val gym: Gym) : DeleteTarget()
    data class MuscleGroupTarget(val mg: MuscleGroup) : DeleteTarget()
    data class ExerciseTarget(val ex: Exercise) : DeleteTarget()
}

enum class ItemType { GYM, MUSCLE_GROUP, EXERCISE }

data class EditingItem(
    val id: Long,
    val type: ItemType,
    val currentName: String
)

data class EditDataUiState(
    val gyms: List<Gym> = emptyList(),
    val muscleGroups: List<MuscleGroup> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val expandedGymIds: Set<Long> = emptySet(),
    val expandedMuscleGroupIds: Set<Long> = emptySet(),
    val deleteTarget: DeleteTarget? = null,
    val editingItem: EditingItem? = null
)

@HiltViewModel
class EditDataViewModel @Inject constructor(
    private val gymRepository: GymRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _extra = MutableStateFlow(UiExtra())

    val uiState: StateFlow<EditDataUiState> = combine(
        gymRepository.observeGyms(),
        gymRepository.observeAllMuscleGroups(),
        exerciseRepository.observeAllExercises(),
        _extra
    ) { gyms, mgs, exs, extra ->
        EditDataUiState(
            gyms = gyms,
            muscleGroups = mgs,
            exercises = exs,
            expandedGymIds = extra.expandedGymIds,
            expandedMuscleGroupIds = extra.expandedMuscleGroupIds,
            deleteTarget = extra.deleteTarget,
            editingItem = extra.editingItem
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditDataUiState())

    fun toggleGym(gymId: Long) = _extra.update { e ->
        e.copy(expandedGymIds = if (gymId in e.expandedGymIds) e.expandedGymIds - gymId else e.expandedGymIds + gymId)
    }

    fun toggleMuscleGroup(mgId: Long) = _extra.update { e ->
        e.copy(expandedMuscleGroupIds = if (mgId in e.expandedMuscleGroupIds) e.expandedMuscleGroupIds - mgId else e.expandedMuscleGroupIds + mgId)
    }

    fun startEdit(id: Long, type: ItemType, name: String) =
        _extra.update { it.copy(editingItem = EditingItem(id, type, name)) }

    fun saveEdit(newName: String) {
        val item = _extra.value.editingItem ?: return
        _extra.update { it.copy(editingItem = null) }
        if (newName.isBlank()) return
        viewModelScope.launch {
            when (item.type) {
                ItemType.GYM -> {
                    val gym = gymRepository.getGym(item.id) ?: return@launch
                    gymRepository.updateGym(gym.copy(name = newName))
                }
                ItemType.MUSCLE_GROUP -> {
                    val mg = gymRepository.getMuscleGroup(item.id) ?: return@launch
                    gymRepository.updateMuscleGroup(mg.copy(name = newName))
                }
                ItemType.EXERCISE -> {
                    val ex = exerciseRepository.getExercise(item.id) ?: return@launch
                    exerciseRepository.updateExercise(ex.copy(name = newName))
                }
            }
        }
    }

    fun cancelEdit() = _extra.update { it.copy(editingItem = null) }

    fun requestDelete(target: DeleteTarget) = _extra.update { it.copy(deleteTarget = target) }

    fun confirmDelete() {
        val target = _extra.value.deleteTarget ?: return
        _extra.update { it.copy(deleteTarget = null) }
        viewModelScope.launch {
            when (target) {
                is DeleteTarget.GymTarget -> gymRepository.deleteGym(target.gym)
                is DeleteTarget.MuscleGroupTarget -> gymRepository.deleteMuscleGroup(target.mg)
                is DeleteTarget.ExerciseTarget -> exerciseRepository.deleteExercise(target.ex)
            }
        }
    }

    fun cancelDelete() = _extra.update { it.copy(deleteTarget = null) }

    private data class UiExtra(
        val expandedGymIds: Set<Long> = emptySet(),
        val expandedMuscleGroupIds: Set<Long> = emptySet(),
        val deleteTarget: DeleteTarget? = null,
        val editingItem: EditingItem? = null
    )
}
