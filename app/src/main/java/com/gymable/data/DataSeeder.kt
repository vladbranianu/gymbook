package com.gymable.data

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
class DataSeeder @Inject constructor(
    private val gymDao: GymDao,
    private val muscleGroupDao: MuscleGroupDao,
    private val exerciseDao: ExerciseDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val workoutSetDao: WorkoutSetDao,
    private val recordDao: RecordDao
) {
    suspend fun seed() {
        if (gymDao.count() > 0) return
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        val base = now - 90 * day

        // ── Gym 1: Iron Paradise ────────────────────────────────────────────
        val ip = gymDao.insert(Gym(name = "Iron Paradise"))

        val chest     = muscleGroupDao.insert(MuscleGroup(name = "Chest",     gymId = ip))
        val back      = muscleGroupDao.insert(MuscleGroup(name = "Back",      gymId = ip))
        val ipLegs    = muscleGroupDao.insert(MuscleGroup(name = "Legs",      gymId = ip))
        val shoulders = muscleGroupDao.insert(MuscleGroup(name = "Shoulders", gymId = ip))
        val arms      = muscleGroupDao.insert(MuscleGroup(name = "Arms",      gymId = ip))

        val benchPress    = exerciseDao.insert(Exercise(name = "Bench Press",           muscleGroupId = chest))
        val inclinePress  = exerciseDao.insert(Exercise(name = "Incline Dumbbell Press", muscleGroupId = chest))
        val cableFly      = exerciseDao.insert(Exercise(name = "Cable Fly",             muscleGroupId = chest))

        val deadlift      = exerciseDao.insert(Exercise(name = "Deadlift",              muscleGroupId = back))
        val pullUps       = exerciseDao.insert(Exercise(name = "Pull-ups",              muscleGroupId = back))
        val seatedRow     = exerciseDao.insert(Exercise(name = "Seated Row",            muscleGroupId = back))

        val squat         = exerciseDao.insert(Exercise(name = "Squat",                muscleGroupId = ipLegs))
        val legPress      = exerciseDao.insert(Exercise(name = "Leg Press",            muscleGroupId = ipLegs))
        val romanianDL    = exerciseDao.insert(Exercise(name = "Romanian Deadlift",    muscleGroupId = ipLegs))

        val ohp           = exerciseDao.insert(Exercise(name = "Overhead Press",       muscleGroupId = shoulders))
        val lateralRaise  = exerciseDao.insert(Exercise(name = "Lateral Raise",        muscleGroupId = shoulders))

        val barbellCurl   = exerciseDao.insert(Exercise(name = "Barbell Curl",         muscleGroupId = arms))
        val tricepPD      = exerciseDao.insert(Exercise(name = "Tricep Pushdown",      muscleGroupId = arms))

        // ── Gym 2: Planet Fitness ───────────────────────────────────────────
        val pf = gymDao.insert(Gym(name = "Planet Fitness"))

        val push = muscleGroupDao.insert(MuscleGroup(name = "Push", gymId = pf))
        val pull = muscleGroupDao.insert(MuscleGroup(name = "Pull", gymId = pf))
        val legs = muscleGroupDao.insert(MuscleGroup(name = "Legs", gymId = pf))

        val dbBench    = exerciseDao.insert(Exercise(name = "Dumbbell Bench Press", muscleGroupId = push))
        val chestMachine = exerciseDao.insert(Exercise(name = "Chest Fly Machine",  muscleGroupId = push))
        val latPulldown  = exerciseDao.insert(Exercise(name = "Lat Pulldown",       muscleGroupId = pull))
        val cableRow     = exerciseDao.insert(Exercise(name = "Cable Row",          muscleGroupId = pull))
        val legPressPF   = exerciseDao.insert(Exercise(name = "Leg Press",          muscleGroupId = legs))
        val hackSquat    = exerciseDao.insert(Exercise(name = "Hack Squat",         muscleGroupId = legs))

        // ── Sessions  (exerciseId, startKg, endKg, reps1, reps2, count, intervalDays) ──
        sessions(benchPress,   70f, 87.5f, 8, 6, 13, 7,  base, day)
        sessions(inclinePress, 24f, 32f,  10, 8, 10, 9,  base, day)
        sessions(cableFly,     15f, 20f,  12, 10, 8, 11, base, day)

        sessions(deadlift,    100f, 132.5f, 5, 4, 10, 9, base, day)
        sessions(pullUps,       0f,  12.5f, 8, 6, 12, 7, base, day)
        sessions(seatedRow,    60f,  82.5f,10, 8, 10, 9, base, day)

        sessions(squat,        80f, 112.5f, 6, 5, 13, 7, base, day)
        sessions(legPress,    120f,  185f,  10, 8, 10, 9, base, day)
        sessions(romanianDL,   70f,   95f,   8, 6, 10, 9, base, day)

        sessions(ohp,          50f,  65f,   6, 5, 10, 9,  base, day)
        sessions(lateralRaise, 12f,  18f,  12, 10, 10, 9, base, day)

        sessions(barbellCurl,  30f,  42.5f, 10, 8, 10, 9, base, day)
        sessions(tricepPD,     35f,   50f,  12, 10,10, 9, base, day)

        sessions(dbBench,      28f,   38f,  10, 8, 8, 11, base, day)
        sessions(chestMachine, 40f,   57.5f,12, 10, 8, 11, base, day)
        sessions(latPulldown,  60f,   82.5f,10, 8, 10, 9,  base, day)
        sessions(cableRow,     55f,   77.5f,10, 8, 10, 9,  base, day)
        sessions(legPressPF,  140f,  192.5f,10, 8, 8, 11,  base, day)
        sessions(hackSquat,    60f,    92.5f,8, 6, 8, 11,  base, day)
    }

    private suspend fun sessions(
        exerciseId: Long,
        startKg: Float,
        endKg: Float,
        reps1: Int,
        reps2: Int,
        count: Int,
        intervalDays: Int,
        base: Long,
        day: Long
    ) {
        val step = if (count > 1) (endKg - startKg) / (count - 1) else 0f
        var maxWeight = 0f
        var maxReps = 0

        for (i in 0 until count) {
            val raw = startKg + i * step
            val weight = (raw * 2).toLong().toFloat() / 2f    // floor to nearest 0.5 kg
            val date = base + i.toLong() * intervalDays * day

            val sessionId = workoutSessionDao.insert(
                WorkoutSession(exerciseId = exerciseId, date = date)
            )
            workoutSetDao.insertAll(listOf(
                WorkoutSet(sessionId = sessionId, setNumber = 1, reps = reps1, weight = weight),
                WorkoutSet(sessionId = sessionId, setNumber = 2, reps = reps2, weight = weight)
            ))

            if (weight > maxWeight || (weight == maxWeight && reps1 > maxReps)) {
                maxWeight = weight
                maxReps = reps1
            }
        }

        recordDao.insert(Record(exerciseId = exerciseId, maxWeight = maxWeight, maxReps = maxReps))
    }
}
