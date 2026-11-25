package com.gymable.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymable.data.local.entity.WorkoutSession
import kotlinx.coroutines.flow.Flow

data class SessionStatsRow(
    @ColumnInfo(name = "sessionId") val sessionId: Long,
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "maxWeight") val maxWeight: Float,
    @ColumnInfo(name = "totalReps") val totalReps: Int,
    @ColumnInfo(name = "totalVolume") val totalVolume: Float
)

@Dao
interface WorkoutSessionDao {

    @Query("SELECT * FROM workout_sessions WHERE exerciseId = :exerciseId ORDER BY date DESC")
    fun observeByExercise(exerciseId: Long): Flow<List<WorkoutSession>>

    @Query("""
        SELECT ws.id AS sessionId, ws.date AS date,
            COALESCE(MAX(wset.weight), 0) AS maxWeight,
            COALESCE(SUM(wset.reps), 0) AS totalReps,
            COALESCE(SUM(CAST(wset.reps AS REAL) * wset.weight), 0) AS totalVolume
        FROM workout_sessions ws
        LEFT JOIN workout_sets wset ON wset.sessionId = ws.id
        WHERE ws.exerciseId = :exerciseId
        GROUP BY ws.id
        ORDER BY ws.date ASC
    """)
    fun observeStatsForExercise(exerciseId: Long): Flow<List<SessionStatsRow>>

    @Query("""
        SELECT ws.id AS sessionId, ws.date AS date,
            COALESCE(MAX(wset.weight), 0) AS maxWeight,
            COALESCE(SUM(wset.reps), 0) AS totalReps,
            COALESCE(SUM(CAST(wset.reps AS REAL) * wset.weight), 0) AS totalVolume
        FROM workout_sessions ws
        LEFT JOIN workout_sets wset ON wset.sessionId = ws.id
        WHERE ws.exerciseId = :exerciseId AND ws.date >= :startMs AND ws.date < :endMs
        GROUP BY ws.id
        ORDER BY ws.date ASC
    """)
    fun observeStatsForExerciseInRange(exerciseId: Long, startMs: Long, endMs: Long): Flow<List<SessionStatsRow>>

    @Query("SELECT * FROM workout_sessions WHERE exerciseId = :exerciseId ORDER BY date DESC LIMIT 1")
    suspend fun getLastForExercise(exerciseId: Long): WorkoutSession?

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    suspend fun getById(sessionId: Long): WorkoutSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkoutSession): Long

    @Update
    suspend fun update(session: WorkoutSession)

    @Query("UPDATE workout_sessions SET rating = :rating WHERE id IN (:ids)")
    suspend fun updateRatings(ids: List<Long>, rating: Int)

    @Delete
    suspend fun delete(session: WorkoutSession)

    @Query("SELECT COUNT(*) FROM workout_sessions")
    suspend fun countAll(): Int
}
