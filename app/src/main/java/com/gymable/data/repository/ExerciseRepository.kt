package com.gymable.data.repository

import com.gymable.data.local.dao.ExerciseDao
import com.gymable.data.local.entity.Exercise
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao
) {
    fun observeAllExercises(): Flow<List<Exercise>> = exerciseDao.observeAll()

    fun observeExercises(muscleGroupId: Long): Flow<List<Exercise>> =
        exerciseDao.observeByMuscleGroup(muscleGroupId)

    fun observeExercise(exerciseId: Long): Flow<Exercise?> =
        exerciseDao.observeById(exerciseId)

    suspend fun getExercise(exerciseId: Long): Exercise? =
        exerciseDao.getById(exerciseId)

    suspend fun addExercise(name: String, muscleGroupId: Long, isUnilateral: Boolean = false): Long =
        exerciseDao.insert(Exercise(name = name, muscleGroupId = muscleGroupId, isUnilateral = isUnilateral))

    suspend fun updateExercise(exercise: Exercise) = exerciseDao.update(exercise)

    suspend fun deleteExercise(exercise: Exercise) = exerciseDao.delete(exercise)
}
