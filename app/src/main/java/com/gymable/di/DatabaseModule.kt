package com.gymable.di

import android.content.Context
import androidx.room.Room
import com.gymable.data.local.GymDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideGymDatabase(@ApplicationContext context: Context): GymDatabase =
        Room.databaseBuilder(
            context,
            GymDatabase::class.java,
            GymDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideGymDao(db: GymDatabase): GymDao = db.gymDao()

    @Provides
    fun provideExportDao(db: GymDatabase): ExportDao = db.exportDao()

    @Provides
    fun provideMuscleGroupDao(db: GymDatabase): MuscleGroupDao = db.muscleGroupDao()

    @Provides
    fun provideExerciseDao(db: GymDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideWorkoutSessionDao(db: GymDatabase): WorkoutSessionDao = db.workoutSessionDao()

    @Provides
    fun provideWorkoutSetDao(db: GymDatabase): WorkoutSetDao = db.workoutSetDao()

    @Provides
    fun provideRecordDao(db: GymDatabase): RecordDao = db.recordDao()

    @Provides
    fun provideExerciseOverviewDao(db: GymDatabase): ExerciseOverviewDao = db.exerciseOverviewDao()

    @Provides
    fun provideWorkoutHistoryDao(db: GymDatabase): WorkoutHistoryDao = db.workoutHistoryDao()

    @Provides
    fun provideDashboardDao(db: GymDatabase): DashboardDao = db.dashboardDao()
}
