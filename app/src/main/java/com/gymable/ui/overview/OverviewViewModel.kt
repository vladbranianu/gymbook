package com.gymable.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymable.data.local.dao.ExerciseOverviewRow
import com.gymable.data.repository.OverviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ExerciseGroup(
    val gymName: String,
    val muscleGroupName: String,
    val exercises: List<ExerciseOverviewRow>
)

data class OverviewUiState(
    val groups: List<ExerciseGroup> = emptyList(),
    val loading: Boolean = true
)

@HiltViewModel
class OverviewViewModel @Inject constructor(
    overviewRepository: OverviewRepository
) : ViewModel() {

    val uiState: StateFlow<OverviewUiState> = overviewRepository
        .observeExerciseOverviews()
        .map { rows ->
            val groups = rows
                .groupBy { it.gymName to it.muscleGroupName }
                .map { (key, exercises) ->
                    ExerciseGroup(
                        gymName = key.first,
                        muscleGroupName = key.second,
                        exercises = exercises
                    )
                }
            OverviewUiState(groups = groups, loading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = OverviewUiState(loading = true)
        )
}
