package com.gymable.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymable.data.local.dao.SessionStatsRow
import com.gymable.data.local.entity.Exercise
import com.gymable.data.local.entity.Gym
import com.gymable.data.local.entity.MuscleGroup
import com.gymable.data.repository.ExerciseRepository
import com.gymable.data.repository.GymRepository
import com.gymable.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

enum class StatMode { WEIGHT, REPS, VOLUME }
enum class PeriodMode { MONTH, WEEK, CUSTOM }

data class StatPoint(val dateMs: Long, val value: Float)
data class DateRange(val startMs: Long, val endMs: Long)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val gymRepository: GymRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _selectedGymId = MutableStateFlow<Long?>(null)
    private val _selectedMuscleGroupId = MutableStateFlow<Long?>(null)
    private val _selectedExerciseId = MutableStateFlow<Long?>(null)
    private val _statMode = MutableStateFlow(StatMode.WEIGHT)
    private val _periodMode = MutableStateFlow(PeriodMode.MONTH)
    private val _periodOffset = MutableStateFlow(0)
    private val _customRange = MutableStateFlow<DateRange?>(null)

    val statMode: StateFlow<StatMode> = _statMode
    val periodMode: StateFlow<PeriodMode> = _periodMode
    val customRange: StateFlow<DateRange?> = _customRange

    val gyms: StateFlow<List<Gym>> = gymRepository.observeGyms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedGym: StateFlow<Gym?> = _selectedGymId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else gymRepository.observeGym(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val muscleGroups: StateFlow<List<MuscleGroup>> = _selectedGymId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else gymRepository.observeMuscleGroups(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedMuscleGroup: StateFlow<MuscleGroup?> = _selectedMuscleGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else gymRepository.observeMuscleGroup(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val exercises: StateFlow<List<Exercise>> = _selectedMuscleGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else exerciseRepository.observeExercises(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedExercise: StateFlow<Exercise?> = _selectedExerciseId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else exerciseRepository.observeExercise(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val currentDateRange: StateFlow<DateRange> = combine(
        _periodMode, _periodOffset, _customRange
    ) { mode, offset, custom ->
        when (mode) {
            PeriodMode.MONTH -> monthRange(offset)
            PeriodMode.WEEK  -> weekRange(offset)
            PeriodMode.CUSTOM -> custom ?: monthRange(0)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), monthRange(0))

    val periodLabel: StateFlow<String> = combine(
        _periodMode, _periodOffset, _customRange
    ) { mode, offset, custom ->
        when (mode) {
            PeriodMode.MONTH  -> monthLabel(offset)
            PeriodMode.WEEK   -> weekLabel(offset)
            PeriodMode.CUSTOM -> if (custom != null) customLabel(custom) else monthLabel(0)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), monthLabel(0))

    val canNavigateForward: StateFlow<Boolean> = _periodOffset
        .map { it < 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val sessionStats: StateFlow<List<SessionStatsRow>> = combine(
        _selectedExerciseId, currentDateRange
    ) { id, range -> id to range }
        .flatMapLatest { (id, range) ->
            if (id == null) flowOf(emptyList())
            else workoutRepository.observeSessionStatsInRange(id, range.startMs, range.endMs)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dataPoints: StateFlow<List<StatPoint>> = combine(sessionStats, _statMode) { statList, mode ->
        statList
            .groupByDay()
            .map { (dayMs, statsForDay) ->
                StatPoint(dateMs = dayMs, value = statsForDay.computeStat(mode))
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val latestPoint: StateFlow<StatPoint?> = dataPoints
        .map { it.lastOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectGym(gym: Gym) {
        _selectedGymId.value = gym.id
        _selectedMuscleGroupId.value = null
        _selectedExerciseId.value = null
    }

    fun selectMuscleGroup(mg: MuscleGroup) {
        _selectedMuscleGroupId.value = mg.id
        _selectedExerciseId.value = null
    }

    fun selectExercise(exercise: Exercise) {
        _selectedExerciseId.value = exercise.id
    }

    fun setStatMode(mode: StatMode) {
        _statMode.value = mode
    }

    fun setPeriodMode(mode: PeriodMode) {
        _periodMode.value = mode
        if (mode != PeriodMode.CUSTOM) _periodOffset.value = 0
    }

    fun navigatePeriod(delta: Int) {
        val next = _periodOffset.value + delta
        if (next > 0) return
        _periodOffset.value = next
    }

    fun setCustomRange(startMs: Long, endMs: Long) {
        _customRange.value = DateRange(startMs, endMs)
    }
}

private fun monthRange(offset: Int): DateRange {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    cal.add(Calendar.MONTH, offset)
    val start = cal.timeInMillis
    cal.add(Calendar.MONTH, 1)
    return DateRange(start, cal.timeInMillis)
}

private fun weekRange(offset: Int): DateRange {
    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    cal.add(Calendar.WEEK_OF_YEAR, offset)
    val start = cal.timeInMillis
    cal.add(Calendar.DAY_OF_MONTH, 7)
    return DateRange(start, cal.timeInMillis)
}

private fun monthLabel(offset: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, offset)
    return SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        .format(cal.time)
        .replaceFirstChar { it.uppercase() }
}

private fun weekLabel(offset: Int): String {
    val range = weekRange(offset)
    val start = Calendar.getInstance().apply { timeInMillis = range.startMs }
    val end = Calendar.getInstance().apply { timeInMillis = range.endMs - 1L }
    val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
    return if (start.get(Calendar.MONTH) == end.get(Calendar.MONTH)) {
        "${start.get(Calendar.DAY_OF_MONTH)} – ${fmt.format(end.time)}"
    } else {
        "${fmt.format(start.time)} – ${fmt.format(end.time)}"
    }
}

private fun customLabel(range: DateRange): String {
    val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
    return "${fmt.format(range.startMs)} – ${fmt.format(range.endMs)}"
}

private fun List<SessionStatsRow>.groupByDay(): List<Pair<Long, List<SessionStatsRow>>> {
    val grouped = LinkedHashMap<Long, MutableList<SessionStatsRow>>()
    for (row in this) {
        val dayKey = Calendar.getInstance().run {
            timeInMillis = row.date
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            timeInMillis
        }
        grouped.getOrPut(dayKey) { mutableListOf() }.add(row)
    }
    return grouped.entries.sortedBy { it.key }.map { it.key to it.value }
}

private fun List<SessionStatsRow>.computeStat(mode: StatMode): Float = when (mode) {
    StatMode.WEIGHT -> maxOf { it.maxWeight }
    StatMode.REPS   -> sumOf { it.totalReps }.toFloat()
    StatMode.VOLUME -> sumOf { it.totalVolume.toDouble() }.toFloat()
}
