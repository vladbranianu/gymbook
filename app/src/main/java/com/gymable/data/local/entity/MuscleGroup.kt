package com.gymable.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "muscle_groups",
    foreignKeys = [
        ForeignKey(
            entity = Gym::class,
            parentColumns = ["id"],
            childColumns = ["gymId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("gymId")]
)
data class MuscleGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val gymId: Long
)
