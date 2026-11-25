package com.gymable.data.repository

import com.gymable.data.local.dao.ExerciseDao
import com.gymable.data.local.dao.RecordDao
import com.gymable.data.local.dao.SessionStatsRow
import com.gymable.data.local.dao.WorkoutSessionDao
import com.gymable.data.local.dao.WorkoutSetDao
import com.gymable.data.local.entity.Record
import com.gymable.data.local.entity.WorkoutSession
import com.gymable.data.local.entity.WorkoutSet
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class SetData(val reps: Int, val weight: Float, val weightRight: Float? = null, val repsRight: Int? = null)

data class WorkoutLogEntry(
    val sessionId: Long,
    val exerciseName: String,
    val sets: List<SetData>
)

data class WorkoutPrefill(
    val lastSets: List<WorkoutSet>,
    val record: Record?
)

data class WorkoutSaveResult(
    val record: Record,
    val sessionId: Long
)

@Singleton
class WorkoutRepository @Inject constructor(
    private val workoutSessionDao: WorkoutSessionDao,
    private val workoutSetDao: WorkoutSetDao,
    private val recordDao: RecordDao,
    private val exerciseDao: ExerciseDao
) {

    fun observeSessions(exerciseId: Long): Flow<List<WorkoutSession>> =
        workoutSessionDao.observeByExercise(exerciseId)

    fun observeSessionStats(exerciseId: Long): Flow<List<SessionStatsRow>> =
        workoutSessionDao.observeStatsForExercise(exerciseId)

    fun observeSessionStatsInRange(exerciseId: Long, startMs: Long, endMs: Long): Flow<List<SessionStatsRow>> =
        workoutSessionDao.observeStatsForExerciseInRange(exerciseId, startMs, endMs)

    fun observeRecord(exerciseId: Long): Flow<Record?> =
        recordDao.observeByExercise(exerciseId)

    suspend fun getPrefill(exerciseId: Long): WorkoutPrefill {
        val lastSession = workoutSessionDao.getLastForExercise(exerciseId)
        val lastSets = if (lastSession != null) workoutSetDao.getBySession(lastSession.id) else emptyList()
        return WorkoutPrefill(
            lastSets = lastSets,
            record = recordDao.getByExercise(exerciseId)
        )
    }

    suspend fun saveWorkout(
        exerciseId: Long,
        sets: List<SetData>,
        date: Long = System.currentTimeMillis()
    ): WorkoutSaveResult {
        val session = WorkoutSession(exerciseId = exerciseId, date = date)
        val sessionId = workoutSessionDao.insert(session)
        val workoutSets = sets.mapIndexed { i, setData ->
            WorkoutSet(
                sessionId = sessionId,
                setNumber = i + 1,
                reps = setData.reps,
                weight = setData.weight,
                weightRight = setData.weightRight,
                repsRight = setData.repsRight
            )
        }
        if (workoutSets.isNotEmpty()) workoutSetDao.insertAll(workoutSets)
        val record = updateRecordIfBeaten(exerciseId, workoutSets)
        return WorkoutSaveResult(record, sessionId)
    }

    suspend fun updateSessionRatings(sessionIds: List<Long>, rating: Int) {
        if (sessionIds.isNotEmpty()) workoutSessionDao.updateRatings(sessionIds, rating)
    }

    private suspend fun updateRecordIfBeaten(exerciseId: Long, sets: List<WorkoutSet>): Record {
        val existing = recordDao.getByExercise(exerciseId)
        if (sets.isEmpty()) {
            return existing ?: Record(exerciseId = exerciseId, maxReps = 0, maxWeight = 0f)
        }

        val bestWeight = sets.maxOf { it.weight }
        val bestReps = sets.filter { it.weight == bestWeight }.maxOf { it.reps }

        if (existing != null) {
            val isBetter = bestWeight > existing.maxWeight ||
                (bestWeight == existing.maxWeight && bestReps > existing.maxReps)
            if (!isBetter) return existing
        }

        val updated = (existing ?: Record(exerciseId = exerciseId, maxReps = 0, maxWeight = 0f))
            .copy(maxReps = bestReps, maxWeight = bestWeight)
        recordDao.insert(updated)
        return updated
    }

    suspend fun restoreSessionLog(sessionIds: List<Long>): List<WorkoutLogEntry> =
        sessionIds.mapNotNull { sessionId ->
            val session = workoutSessionDao.getById(sessionId) ?: return@mapNotNull null
            val sets = workoutSetDao.getBySession(sessionId)
            val exercise = exerciseDao.getById(session.exerciseId) ?: return@mapNotNull null
            WorkoutLogEntry(
                sessionId = sessionId,
                exerciseName = exercise.name,
                sets = sets.map { SetData(it.reps, it.weight, it.weightRight, it.repsRight) }
            )
        }

    suspend fun deleteSession(session: WorkoutSession) =
        workoutSessionDao.delete(session)

    suspend fun deleteSessionById(sessionId: Long) {
        workoutSessionDao.getById(sessionId)?.let { workoutSessionDao.delete(it) }
    }
}
