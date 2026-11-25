package com.gymable.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymable.data.local.entity.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises WHERE muscleGroupId = :muscleGroupId ORDER BY name ASC")
    fun observeByMuscleGroup(muscleGroupId: Long): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    fun observeById(exerciseId: Long): Flow<Exercise?>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun getById(exerciseId: Long): Exercise?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: Exercise): Long

    @Update
    suspend fun update(exercise: Exercise)

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun observeAll(): Flow<List<Exercise>>

    @Delete
    suspend fun delete(exercise: Exercise)
}
