package com.gymable.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class WorkoutHistoryRow(
    @ColumnInfo(name = "sessionId")       val sessionId: Long,
    @ColumnInfo(name = "date")            val date: Long,
    @ColumnInfo(name = "exerciseName")    val exerciseName: String,
    @ColumnInfo(name = "muscleGroupName") val muscleGroupName: String,
    @ColumnInfo(name = "gymName")         val gymName: String,
    @ColumnInfo(name = "setCount")        val setCount: Int,
    @ColumnInfo(name = "totalVolume")     val totalVolume: Float,
    @ColumnInfo(name = "rating")          val rating: Int
)

@Dao
interface WorkoutHistoryDao {
    @Query("""
        SELECT
            ws.id                                                              AS sessionId,
            ws.date                                                            AS date,
            e.name                                                             AS exerciseName,
            mg.name                                                            AS muscleGroupName,
            g.name                                                             AS gymName,
            COUNT(wset.id)                                                     AS setCount,
            COALESCE(SUM(CAST(wset.reps AS REAL) * wset.weight), 0)           AS totalVolume,
            ws.rating                                                          AS rating
        FROM workout_sessions ws
        JOIN exercises e      ON ws.exerciseId    = e.id
        JOIN muscle_groups mg ON e.muscleGroupId  = mg.id
        JOIN gyms g           ON mg.gymId         = g.id
        LEFT JOIN workout_sets wset ON wset.sessionId = ws.id
        WHERE ws.date >= :from AND ws.date < :until
        GROUP BY ws.id
        ORDER BY ws.date DESC
    """)
    fun observeInRange(from: Long, until: Long): Flow<List<WorkoutHistoryRow>>

    @Query("""
        SELECT ws.id AS sessionId, ws.date AS date, e.name AS exerciseName,
               mg.name AS muscleGroupName, g.name AS gymName,
               COUNT(wset.id) AS setCount,
               COALESCE(SUM(CAST(wset.reps AS REAL) * wset.weight), 0) AS totalVolume,
               ws.rating AS rating
        FROM workout_sessions ws
        JOIN exercises e ON ws.exerciseId = e.id
        JOIN muscle_groups mg ON e.muscleGroupId = mg.id
        JOIN gyms g ON mg.gymId = g.id
        LEFT JOIN workout_sets wset ON wset.sessionId = ws.id
        GROUP BY ws.id
        ORDER BY ws.date DESC
        LIMIT 1
    """)
    fun observeLastSession(): Flow<WorkoutHistoryRow?>

    @Query("""
        SELECT wset.sessionId AS sessionId, wset.setNumber AS setNumber,
               wset.reps AS reps, wset.weight AS weight,
               wset.weightRight AS weightRight, wset.repsRight AS repsRight
        FROM workout_sets wset
        JOIN workout_sessions ws ON wset.sessionId = ws.id
        WHERE ws.date >= :from AND ws.date < :until
        ORDER BY wset.sessionId, wset.setNumber
    """)
    fun observeSetsInRange(from: Long, until: Long): Flow<List<WorkoutHistorySetRow>>

    @Query("""
        SELECT
            ws.id                                                              AS sessionId,
            ws.date                                                            AS date,
            e.name                                                             AS exerciseName,
            mg.name                                                            AS muscleGroupName,
            g.name                                                             AS gymName,
            COUNT(wset.id)                                                     AS setCount,
            COALESCE(SUM(CAST(wset.reps AS REAL) * wset.weight), 0)           AS totalVolume,
            ws.rating                                                          AS rating
        FROM workout_sessions ws
        JOIN exercises e      ON ws.exerciseId    = e.id
        JOIN muscle_groups mg ON e.muscleGroupId  = mg.id
        JOIN gyms g           ON mg.gymId         = g.id
        LEFT JOIN workout_sets wset ON wset.sessionId = ws.id
        GROUP BY ws.id
        ORDER BY ws.date DESC
    """)
    fun observeAll(): Flow<List<WorkoutHistoryRow>>

    @Query("""
        SELECT wset.sessionId AS sessionId, wset.setNumber AS setNumber,
               wset.reps AS reps, wset.weight AS weight,
               wset.weightRight AS weightRight, wset.repsRight AS repsRight
        FROM workout_sets wset
        JOIN workout_sessions ws ON wset.sessionId = ws.id
        ORDER BY wset.sessionId, wset.setNumber
    """)
    fun observeAllSets(): Flow<List<WorkoutHistorySetRow>>

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE date >= :from AND date < :until")
    fun observeSessionCountInRange(from: Long, until: Long): Flow<Int>

    @Query("""
        SELECT ws.date AS date,
            COALESCE(SUM(CAST(wset.reps AS REAL) * wset.weight), 0) AS totalVolume
        FROM workout_sessions ws
        LEFT JOIN workout_sets wset ON wset.sessionId = ws.id
        WHERE ws.date >= :from AND ws.date < :until
        GROUP BY ws.id
    """)
    fun observeSessionsForWeeklyActivity(from: Long, until: Long): Flow<List<WeeklySessionRow>>
}

data class WeeklySessionRow(
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "totalVolume") val totalVolume: Float
)

data class WorkoutHistorySetRow(
    @ColumnInfo(name = "sessionId")   val sessionId: Long,
    @ColumnInfo(name = "setNumber")   val setNumber: Int,
    @ColumnInfo(name = "reps")        val reps: Int,
    @ColumnInfo(name = "weight")      val weight: Float,
    @ColumnInfo(name = "weightRight") val weightRight: Float?,
    @ColumnInfo(name = "repsRight")   val repsRight: Int?
)
