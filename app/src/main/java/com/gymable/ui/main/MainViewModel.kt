package com.gymable.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymable.data.local.dao.RecentRecordRow
import com.gymable.data.local.dao.WeeklySessionRow
import com.gymable.data.local.dao.WorkoutHistoryRow
import java.util.Calendar
import com.gymable.data.local.entity.Exercise
import com.gymable.data.local.entity.Gym
import com.gymable.data.local.entity.MuscleGroup
import com.gymable.data.repository.DashboardRepository
import com.gymable.data.repository.ExerciseRepository
import com.gymable.data.repository.GymRepository
import com.gymable.data.repository.PrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeeklyActivityState(
    val sessionCount: Int = 0,
    val totalVolumeKg: Float = 0f,
    val activeDays: Set<Int> = emptySet()
)

data class DashboardUiState(
    val lastSession: WorkoutHistoryRow? = null,
    val monthlySessionCount: Int = 0,
    val topRecords: List<RecentRecordRow> = emptyList(),
    val weeklyActivity: WeeklyActivityState = WeeklyActivityState()
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val gymRepository: GymRepository,
    private val exerciseRepository: ExerciseRepository,
    private val dashboardRepository: DashboardRepository,
    private val prefsRepository: PrefsRepository
) : ViewModel() {

    private val selectedGymId = MutableStateFlow<Long?>(null)
    private val selectedMuscleGroupId = MutableStateFlow<Long?>(null)

    val gyms: StateFlow<List<Gym>> = gymRepository.observeGyms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedGym: StateFlow<Gym?> = selectedGymId
        .flatMapLatest { id -> if (id == null) flowOf(null) else gymRepository.observeGym(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val muscleGroups: StateFlow<List<MuscleGroup>> = selectedGymId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else gymRepository.observeMuscleGroups(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedMuscleGroup: StateFlow<MuscleGroup?> = selectedMuscleGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else gymRepository.observeMuscleGroup(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val exercises: StateFlow<List<Exercise>> = selectedMuscleGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else exerciseRepository.observeExercises(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val maxDonationTier: StateFlow<Int> = prefsRepository.maxDonationTierFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val dashboardState: StateFlow<DashboardUiState> = combine(
        dashboardRepository.observeLastSession(),
        dashboardRepository.observeMonthlySessionCount(),
        dashboardRepository.observeTopRecords(),
        dashboardRepository.observeWeeklySessions()
    ) { lastSession, monthlyCount, topRecords, weeklySessions ->
        DashboardUiState(
            lastSession = lastSession,
            monthlySessionCount = monthlyCount,
            topRecords = topRecords,
            weeklyActivity = computeWeeklyActivity(weeklySessions)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private fun computeWeeklyActivity(sessions: List<WeeklySessionRow>): WeeklyActivityState {
        val activeDays = sessions.map { s ->
            val c = Calendar.getInstance().also { it.timeInMillis = s.date }
            (c.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        }.toSet()
        val volume = sessions.sumOf { it.totalVolume.toDouble() }.toFloat()
        return WeeklyActivityState(sessions.size, volume, activeDays)
    }

    fun selectGym(gymId: Long) {
        selectedGymId.value = gymId
        selectedMuscleGroupId.value = null
    }

    fun selectMuscleGroup(muscleGroupId: Long) {
        selectedMuscleGroupId.value = muscleGroupId
    }

    fun clearGymSelection() {
        selectedGymId.value = null
        selectedMuscleGroupId.value = null
    }

    fun clearMuscleGroupSelection() {
        selectedMuscleGroupId.value = null
    }

    fun addGym(name: String) = viewModelScope.launch {
        gymRepository.addGym(name.trim())
    }

    fun addMuscleGroup(name: String, gymId: Long) = viewModelScope.launch {
        gymRepository.addMuscleGroup(name.trim(), gymId)
    }

    fun deleteGym(gym: Gym) = viewModelScope.launch {
        gymRepository.deleteGym(gym)
        if (selectedGymId.value == gym.id) clearGymSelection()
    }

    fun deleteMuscleGroup(muscleGroup: MuscleGroup) = viewModelScope.launch {
        gymRepository.deleteMuscleGroup(muscleGroup)
        if (selectedMuscleGroupId.value == muscleGroup.id) clearMuscleGroupSelection()
    }

    fun deleteExercise(exercise: Exercise) = viewModelScope.launch {
        exerciseRepository.deleteExercise(exercise)
    }
}
