package com.gymable.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class ExerciseOverviewRow(
    @ColumnInfo(name = "exerciseId")      val exerciseId: Long,
    @ColumnInfo(name = "exerciseName")    val exerciseName: String,
    @ColumnInfo(name = "muscleGroupName") val muscleGroupName: String,
    @ColumnInfo(name = "gymName")         val gymName: String,
    @ColumnInfo(name = "lastDate")        val lastDate: Long?,
    @ColumnInfo(name = "lastSetCount")    val lastSetCount: Int,
    @ColumnInfo(name = "lastTopWeight")   val lastTopWeight: Float?,
    @ColumnInfo(name = "recordMaxReps")   val recordMaxReps: Int?,
    @ColumnInfo(name = "recordMaxWeight") val recordMaxWeight: Float?
)

@Dao
interface ExerciseOverviewDao {

    @Query("""
        SELECT
            e.id              AS exerciseId,
            e.name            AS exerciseName,
            mg.name           AS muscleGroupName,
            g.name            AS gymName,
            (SELECT date FROM workout_sessions WHERE exerciseId = e.id ORDER BY id DESC LIMIT 1) AS lastDate,
            (SELECT COUNT(*) FROM workout_sets WHERE sessionId =
                (SELECT id FROM workout_sessions WHERE exerciseId = e.id ORDER BY id DESC LIMIT 1)
            ) AS lastSetCount,
            (SELECT MAX(weight) FROM workout_sets WHERE sessionId =
                (SELECT id FROM workout_sessions WHERE exerciseId = e.id ORDER BY id DESC LIMIT 1)
            ) AS lastTopWeight,
            r.maxReps         AS recordMaxReps,
            r.maxWeight       AS recordMaxWeight
        FROM exercises e
        INNER JOIN muscle_groups mg ON e.muscleGroupId = mg.id
        INNER JOIN gyms g ON mg.gymId = g.id
        LEFT JOIN records r ON r.exerciseId = e.id
        ORDER BY g.name ASC, mg.name ASC, e.name ASC
    """)
    fun observeAll(): Flow<List<ExerciseOverviewRow>>
}
