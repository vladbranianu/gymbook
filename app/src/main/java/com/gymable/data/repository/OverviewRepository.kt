package com.gymable.data.repository

import com.gymable.data.local.dao.ExerciseOverviewDao
import com.gymable.data.local.dao.ExerciseOverviewRow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverviewRepository @Inject constructor(
    private val overviewDao: ExerciseOverviewDao
) {
    fun observeExerciseOverviews(): Flow<List<ExerciseOverviewRow>> =
        overviewDao.observeAll()
}
