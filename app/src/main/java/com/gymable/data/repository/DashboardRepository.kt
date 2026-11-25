package com.gymable.data.repository

import com.gymable.data.local.dao.DashboardDao
import com.gymable.data.local.dao.RecentRecordRow
import com.gymable.data.local.dao.WeeklySessionRow
import com.gymable.data.local.dao.WorkoutHistoryDao
import com.gymable.data.local.dao.WorkoutHistoryRow
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val workoutHistoryDao: WorkoutHistoryDao,
    private val dashboardDao: DashboardDao
) {
    fun observeLastSession(): Flow<WorkoutHistoryRow?> =
        workoutHistoryDao.observeLastSession()

    fun observeMonthlySessionCount(): Flow<Int> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val from = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val until = cal.timeInMillis
        return workoutHistoryDao.observeSessionCountInRange(from, until)
    }

    fun observeTopRecords(): Flow<List<RecentRecordRow>> =
        dashboardDao.observeTopRecords()

    fun observeWeeklySessions(): Flow<List<WeeklySessionRow>> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            val daysFromMonday = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
            add(Calendar.DAY_OF_YEAR, -daysFromMonday)
        }
        val from = cal.timeInMillis
        val until = from + 7L * 24 * 60 * 60 * 1000
        return workoutHistoryDao.observeSessionsForWeeklyActivity(from, until)
    }
}
