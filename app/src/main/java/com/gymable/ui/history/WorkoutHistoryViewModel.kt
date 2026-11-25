package com.gymable.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymable.data.local.dao.WorkoutHistoryDao
import com.gymable.data.local.dao.WorkoutHistoryRow
import com.gymable.data.local.dao.WorkoutHistorySetRow
import com.gymable.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class SessionWithSets(
    val session: WorkoutHistoryRow,
    val sets: List<WorkoutHistorySetRow>
)

@HiltViewModel
class WorkoutHistoryViewModel @Inject constructor(
    private val historyDao: WorkoutHistoryDao,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _selectedIndex = MutableStateFlow(0)

    // All days with sessions, sorted newest first (index 0 = most recent)
    private val allGrouped: StateFlow<List<Pair<Long, List<SessionWithSets>>>> =
        combine(
            historyDao.observeAll(),
            historyDao.observeAllSets()
        ) { rows, sets ->
            val setsBySession = sets.groupBy { it.sessionId }
            rows.groupBy { dayKey(it.date) }
                .entries
                .sortedByDescending { it.key }
                .map { (dayKey, sessions) ->
                    dayKey to sessions.map { session ->
                        SessionWithSets(session, setsBySession[session.sessionId] ?: emptyList())
                    }
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentDay: StateFlow<Pair<Long, List<SessionWithSets>>?> =
        combine(allGrouped, _selectedIndex) { grouped, index ->
            grouped.getOrNull(index)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val canGoNext: StateFlow<Boolean> =
        combine(allGrouped, _selectedIndex) { _, index ->
            index > 0
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val canGoPrev: StateFlow<Boolean> =
        combine(allGrouped, _selectedIndex) { grouped, index ->
            index < grouped.size - 1
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun prevDay() {
        if (canGoPrev.value) _selectedIndex.value++
    }

    fun nextDay() {
        if (canGoNext.value) _selectedIndex.value--
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            workoutRepository.deleteSessionById(sessionId)
        }
    }
}

private fun dayKey(ms: Long): Long = Calendar.getInstance().run {
    timeInMillis = ms
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    timeInMillis
}
