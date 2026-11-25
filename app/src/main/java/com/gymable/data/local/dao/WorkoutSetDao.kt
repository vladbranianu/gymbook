package com.gymable.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gymable.data.local.entity.WorkoutSet

@Dao
interface WorkoutSetDao {
    @Insert
    suspend fun insertAll(sets: List<WorkoutSet>)

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY setNumber")
    suspend fun getBySession(sessionId: Long): List<WorkoutSet>
}
