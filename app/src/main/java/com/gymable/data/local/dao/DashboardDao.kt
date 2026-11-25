package com.gymable.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class RecentRecordRow(
    @ColumnInfo(name = "exerciseName") val exerciseName: String,
    @ColumnInfo(name = "muscleGroupName") val muscleGroupName: String,
    @ColumnInfo(name = "maxWeight") val maxWeight: Float,
    @ColumnInfo(name = "maxReps") val maxReps: Int
)

@Dao
interface DashboardDao {
    @Query("""
        SELECT e.name AS exerciseName, mg.name AS muscleGroupName,
               MAX(r.maxWeight) AS maxWeight, MAX(r.maxReps) AS maxReps
        FROM records r
        JOIN exercises e ON r.exerciseId = e.id
        JOIN muscle_groups mg ON e.muscleGroupId = mg.id
        GROUP BY e.name
        ORDER BY MAX(r.maxWeight) DESC
        LIMIT 5
    """)
    fun observeTopRecords(): Flow<List<RecentRecordRow>>
}
