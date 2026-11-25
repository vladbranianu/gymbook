package com.gymable.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.gymable.data.local.entity.Exercise
import com.gymable.data.local.entity.Gym
import com.gymable.data.local.entity.MuscleGroup
import com.gymable.data.local.entity.Record
import com.gymable.data.local.entity.WorkoutSession
import com.gymable.data.local.entity.WorkoutSet

@Dao
interface ExportDao {
    @Query("SELECT * FROM gyms ORDER BY id")
    suspend fun getAllGyms(): List<Gym>

    @Query("SELECT * FROM muscle_groups WHERE gymId = :gymId ORDER BY id")
    suspend fun getMuscleGroupsForGym(gymId: Long): List<MuscleGroup>

    @Query("SELECT * FROM exercises WHERE muscleGroupId = :muscleGroupId ORDER BY id")
    suspend fun getExercisesForMuscleGroup(muscleGroupId: Long): List<Exercise>

    @Query("SELECT * FROM workout_sessions WHERE exerciseId = :exerciseId AND date >= :from AND date < :until ORDER BY date")
    suspend fun getSessionsForExercise(exerciseId: Long, from: Long, until: Long): List<WorkoutSession>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY setNumber")
    suspend fun getSetsForSession(sessionId: Long): List<WorkoutSet>

    @Query("SELECT * FROM records WHERE exerciseId = :exerciseId LIMIT 1")
    suspend fun getRecordForExercise(exerciseId: Long): Record?
}
