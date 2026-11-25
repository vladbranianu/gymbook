package com.gymable.data.repository

import com.gymable.data.GymDataImporter
import com.gymable.data.local.dao.ExerciseDao
import com.gymable.data.local.dao.GymDao
import com.gymable.data.local.dao.MuscleGroupDao
import com.gymable.data.local.dao.RecordDao
import com.gymable.data.local.dao.WorkoutSessionDao
import com.gymable.data.local.dao.WorkoutSetDao
import com.gymable.data.local.entity.Exercise
import com.gymable.data.local.entity.Gym
import com.gymable.data.local.entity.MuscleGroup
import com.gymable.data.local.entity.Record
import com.gymable.data.local.entity.WorkoutSession
import com.gymable.data.local.entity.WorkoutSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportRepository @Inject constructor(
    private val gymDao: GymDao,
    private val muscleGroupDao: MuscleGroupDao,
    private val exerciseDao: ExerciseDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val workoutSetDao: WorkoutSetDao,
    private val recordDao: RecordDao,
    private val importer: GymDataImporter
) {
    suspend fun importFromJson(json: String) {
        val data = importer.parse(json)

        gymDao.deleteAll()

        for (gymExport in data.gyms) {
            val gymId = gymDao.insert(Gym(name = gymExport.gym.name))

            for (mgExport in gymExport.muscleGroups) {
                val mgId = muscleGroupDao.insert(
                    MuscleGroup(name = mgExport.muscleGroup.name, gymId = gymId)
                )

                for (exExport in mgExport.exercises) {
                    val exId = exerciseDao.insert(
                        Exercise(name = exExport.exercise.name, muscleGroupId = mgId)
                    )

                    exExport.record?.let { rec ->
                        recordDao.insert(
                            Record(exerciseId = exId, maxWeight = rec.maxWeight, maxReps = rec.maxReps)
                        )
                    }

                    for (sws in exExport.sessions) {
                        val sessionId = workoutSessionDao.insert(
                            WorkoutSession(
                                exerciseId = exId,
                                date = sws.session.date,
                                rating = sws.session.rating
                            )
                        )
                        if (sws.sets.isNotEmpty()) {
                            workoutSetDao.insertAll(sws.sets.mapIndexed { i, set ->
                                WorkoutSet(
                                    sessionId = sessionId,
                                    setNumber = i + 1,
                                    reps = set.reps,
                                    weight = set.weight
                                )
                            })
                        }
                    }
                }
            }
        }
    }
}
