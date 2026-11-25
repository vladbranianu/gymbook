package com.gymable.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymable.data.local.entity.MuscleGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface MuscleGroupDao {

    @Query("SELECT * FROM muscle_groups WHERE gymId = :gymId ORDER BY name ASC")
    fun observeByGym(gymId: Long): Flow<List<MuscleGroup>>

    @Query("SELECT * FROM muscle_groups WHERE id = :muscleGroupId")
    fun observeById(muscleGroupId: Long): Flow<MuscleGroup?>

    @Query("SELECT * FROM muscle_groups WHERE id = :muscleGroupId")
    suspend fun getById(muscleGroupId: Long): MuscleGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(muscleGroup: MuscleGroup): Long

    @Update
    suspend fun update(muscleGroup: MuscleGroup)

    @Query("SELECT * FROM muscle_groups ORDER BY name ASC")
    fun observeAll(): Flow<List<MuscleGroup>>

    @Delete
    suspend fun delete(muscleGroup: MuscleGroup)
}
