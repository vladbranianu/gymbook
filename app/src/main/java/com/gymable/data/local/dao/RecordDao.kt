package com.gymable.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymable.data.local.entity.Record
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {

    @Query("SELECT * FROM records WHERE exerciseId = :exerciseId")
    fun observeByExercise(exerciseId: Long): Flow<Record?>

    @Query("SELECT * FROM records WHERE exerciseId = :exerciseId")
    suspend fun getByExercise(exerciseId: Long): Record?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: Record): Long

    @Update
    suspend fun update(record: Record)

    @Delete
    suspend fun delete(record: Record)
}
