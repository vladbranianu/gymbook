package com.gymable.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gyms")
data class Gym(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)
