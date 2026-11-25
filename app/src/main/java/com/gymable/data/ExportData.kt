package com.gymable.data

import com.gymable.data.local.entity.Exercise
import com.gymable.data.local.entity.Gym
import com.gymable.data.local.entity.MuscleGroup
import com.gymable.data.local.entity.Record
import com.gymable.data.local.entity.WorkoutSession
import com.gymable.data.local.entity.WorkoutSet

data class ExportData(
    val gyms: List<GymExport>,
    val exportDateMs: Long = System.currentTimeMillis()
)

data class GymExport(
    val gym: Gym,
    val muscleGroups: List<MuscleGroupExport>
)

data class MuscleGroupExport(
    val muscleGroup: MuscleGroup,
    val exercises: List<ExerciseExport>
)

data class ExerciseExport(
    val exercise: Exercise,
    val record: Record?,
    val sessions: List<SessionWithSets>
)

data class SessionWithSets(
    val session: WorkoutSession,
    val sets: List<WorkoutSet>
)
