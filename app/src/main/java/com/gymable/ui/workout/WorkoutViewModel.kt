package com.gymable.ui.workout

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymable.data.local.dao.WorkoutSessionDao
import com.gymable.data.local.entity.Exercise
import com.gymable.data.local.entity.Record
import com.gymable.data.local.entity.WorkoutSet
import com.gymable.data.repository.ExerciseRepository
import com.gymable.data.repository.PrefsRepository
import com.gymable.data.repository.ReviewRepository
import com.gymable.data.repository.SetData
import com.gymable.data.repository.WorkoutLogEntry
import com.gymable.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val REVIEW_REQUEST_WORKOUT_THRESHOLD = 5

data class DraftSetInput(
    val reps: String = "",
    val weight: String = "",
    val weightRight: String = "",
    val repsRight: String = ""
)

data class WorkoutUiState(
    val exercise: Exercise? = null,
    val record: Record? = null,
    val setHints: List<WorkoutSet> = emptyList(),
    val loading: Boolean = true,
    val saved: Boolean = false,
    val newRecord: Boolean = false,
    val error: String? = null,
    val sessionLog: List<WorkoutLogEntry> = emptyList(),
    val workoutFinished: Boolean = false,
    val showDonationNudge: Boolean = false,
    val showReviewRequest: Boolean = false,
    val draftGymId: Long? = null,
    val draftMuscleGroupId: Long? = null,
    val draftExerciseId: Long? = null,
    val draftSetCount: Int = 3,
    val draftSets: List<DraftSetInput> = emptyList()
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val prefsRepository: PrefsRepository,
    private val workoutSessionDao: WorkoutSessionDao,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private var currentExerciseId: Long? = null

    init {
        val savedIds = prefsRepository.getWorkoutSessionIds()
        if (savedIds.isNotEmpty()) {
            viewModelScope.launch {
                val restored = workoutRepository.restoreSessionLog(savedIds)
                if (restored.isNotEmpty()) {
                    _uiState.update { it.copy(sessionLog = restored) }
                } else {
                    prefsRepository.clearWorkoutSession()
                }
            }
        }
    }

    fun load(exerciseId: Long) {
        currentExerciseId = exerciseId
        val existingLog = _uiState.value.sessionLog
        _uiState.value = WorkoutUiState(loading = true, sessionLog = existingLog)
        viewModelScope.launch {
            val exercise = exerciseRepository.getExercise(exerciseId)
            val prefill = workoutRepository.getPrefill(exerciseId)
            _uiState.value = WorkoutUiState(
                exercise = exercise,
                record = prefill.record,
                setHints = prefill.lastSets,
                loading = false,
                sessionLog = existingLog
            )
        }
    }

    fun saveWorkout(sets: List<SetData>) {
        val exerciseId = currentExerciseId ?: run {
            _uiState.update { it.copy(error = "Niciun exercitiu selectat") }
            return
        }
        viewModelScope.launch {
            val previousRecord = _uiState.value.record
            val result = workoutRepository.saveWorkout(exerciseId = exerciseId, sets = sets)
            val isNewRecord = previousRecord == null ||
                result.record.maxWeight != previousRecord.maxWeight ||
                result.record.maxReps != previousRecord.maxReps
            val exerciseName = _uiState.value.exercise?.name.orEmpty()
            prefsRepository.addWorkoutSessionId(result.sessionId)
            _uiState.update {
                it.copy(
                    record = result.record,
                    saved = true,
                    newRecord = isNewRecord,
                    sessionLog = it.sessionLog + WorkoutLogEntry(
                        sessionId = result.sessionId,
                        exerciseName = exerciseName,
                        sets = sets
                    ),
                    draftSets = emptyList(),
                    draftSetCount = 3
                )
            }
        }
    }

    fun applyRating(rating: Int) {
        val ids = _uiState.value.sessionLog.map { it.sessionId }
        viewModelScope.launch {
            workoutRepository.updateSessionRatings(ids, rating)
            val shouldNudge = !prefsRepository.donationNudgeShown &&
                workoutSessionDao.countAll() >= 10
            if (shouldNudge) prefsRepository.donationNudgeShown = true

            val completedWorkouts = prefsRepository.incrementCompletedWorkouts()
            // Skip the review ask if the donation nudge is about to show this time - never stack two asks
            val shouldRequestReview = !shouldNudge &&
                !prefsRepository.reviewRequested &&
                completedWorkouts >= REVIEW_REQUEST_WORKOUT_THRESHOLD
            if (shouldRequestReview) prefsRepository.reviewRequested = true

            _uiState.update {
                it.copy(
                    workoutFinished = true,
                    showDonationNudge = shouldNudge,
                    showReviewRequest = shouldRequestReview
                )
            }
        }
    }

    fun launchReviewFlow(activity: Activity) {
        viewModelScope.launch {
            reviewRepository.launchReviewFlow(activity)
        }
    }

    fun consumeSavedEvent() {
        _uiState.update { it.copy(saved = false, newRecord = false) }
    }

    fun consumeError() {
        _uiState.update { it.copy(error = null) }
    }

    fun saveDraft(gymId: Long?, muscleGroupId: Long?, exerciseId: Long?, setCount: Int, sets: List<DraftSetInput>) {
        _uiState.update {
            it.copy(
                draftGymId = gymId,
                draftMuscleGroupId = muscleGroupId,
                draftExerciseId = exerciseId,
                draftSetCount = setCount,
                draftSets = sets
            )
        }
    }

    fun resetWorkout() {
        prefsRepository.clearWorkoutSession()
        currentExerciseId = null
        _uiState.value = WorkoutUiState()
    }
}
