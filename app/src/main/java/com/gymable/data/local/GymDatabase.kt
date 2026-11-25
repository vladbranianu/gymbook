package com.gymable.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gymable.data.local.dao.DashboardDao
import com.gymable.data.local.dao.ExerciseDao
import com.gymable.data.local.dao.ExerciseOverviewDao
import com.gymable.data.local.dao.ExportDao
import com.gymable.data.local.dao.GymDao
import com.gymable.data.local.dao.MuscleGroupDao
import com.gymable.data.local.dao.RecordDao
import com.gymable.data.local.dao.WorkoutHistoryDao
import com.gymable.data.local.dao.WorkoutSessionDao
import com.gymable.data.local.dao.WorkoutSetDao
import com.gymable.data.local.entity.Exercise
import com.gymable.data.local.entity.Gym
import com.gymable.data.local.entity.MuscleGroup
import com.gymable.data.local.entity.Record
import com.gymable.data.local.entity.WorkoutSession
import com.gymable.data.local.entity.WorkoutSet

@Database(
    entities = [
        Gym::class,
        MuscleGroup::class,
        Exercise::class,
        WorkoutSession::class,
        WorkoutSet::class,
        Record::class
    ],
    version = GymDatabase.VERSION,
    exportSchema = true
)
abstract class GymDatabase : RoomDatabase() {
    abstract fun gymDao(): GymDao
    abstract fun exportDao(): ExportDao
    abstract fun muscleGroupDao(): MuscleGroupDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun workoutSetDao(): WorkoutSetDao
    abstract fun recordDao(): RecordDao
    abstract fun exerciseOverviewDao(): ExerciseOverviewDao
    abstract fun workoutHistoryDao(): WorkoutHistoryDao
    abstract fun dashboardDao(): DashboardDao

    companion object {
        const val DATABASE_NAME = "gym_database.db"
        const val VERSION = 7
    }
}
