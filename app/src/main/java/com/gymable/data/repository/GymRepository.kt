package com.gymable.data.repository

import com.gymable.data.local.dao.GymDao
import com.gymable.data.local.dao.MuscleGroupDao
import com.gymable.data.local.entity.Gym
import com.gymable.data.local.entity.MuscleGroup
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GymRepository @Inject constructor(
    private val gymDao: GymDao,
    private val muscleGroupDao: MuscleGroupDao
) {
    fun observeGyms(): Flow<List<Gym>> = gymDao.observeAll()

    fun observeGym(gymId: Long): Flow<Gym?> = gymDao.observeById(gymId)

    suspend fun getGym(gymId: Long): Gym? = gymDao.getById(gymId)

    suspend fun addGym(name: String): Long = gymDao.insert(Gym(name = name))

    suspend fun updateGym(gym: Gym) = gymDao.update(gym)

    suspend fun deleteGym(gym: Gym) = gymDao.delete(gym)

    fun observeAllMuscleGroups(): Flow<List<MuscleGroup>> = muscleGroupDao.observeAll()

    fun observeMuscleGroups(gymId: Long): Flow<List<MuscleGroup>> =
        muscleGroupDao.observeByGym(gymId)

    fun observeMuscleGroup(muscleGroupId: Long): Flow<MuscleGroup?> =
        muscleGroupDao.observeById(muscleGroupId)

    suspend fun getMuscleGroup(muscleGroupId: Long): MuscleGroup? =
        muscleGroupDao.getById(muscleGroupId)

    suspend fun addMuscleGroup(name: String, gymId: Long): Long =
        muscleGroupDao.insert(MuscleGroup(name = name, gymId = gymId))

    suspend fun updateMuscleGroup(muscleGroup: MuscleGroup) =
        muscleGroupDao.update(muscleGroup)

    suspend fun deleteMuscleGroup(muscleGroup: MuscleGroup) =
        muscleGroupDao.delete(muscleGroup)
}
