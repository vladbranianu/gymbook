package com.gymable.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymable.data.local.entity.Gym
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {

    @Query("SELECT * FROM gyms ORDER BY name ASC")
    fun observeAll(): Flow<List<Gym>>

    @Query("SELECT * FROM gyms WHERE id = :gymId")
    fun observeById(gymId: Long): Flow<Gym?>

    @Query("SELECT * FROM gyms WHERE id = :gymId")
    suspend fun getById(gymId: Long): Gym?

    @Query("SELECT COUNT(*) FROM gyms")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gym: Gym): Long

    @Update
    suspend fun update(gym: Gym)

    @Delete
    suspend fun delete(gym: Gym)

    @Query("DELETE FROM gyms")
    suspend fun deleteAll()
}
