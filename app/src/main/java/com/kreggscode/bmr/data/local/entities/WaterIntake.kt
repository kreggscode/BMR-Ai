package com.kreggscode.bmr.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "water_intake",
    foreignKeys = [
        ForeignKey(
            entity = UserProfile::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["userId", "date"])]
)
data class WaterIntake(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val date: Long, // Date at midnight (start of day)
    val totalMl: Int, // Total milliliters consumed today
    val glasses: Int, // Number of glasses consumed
    val lastUpdated: Long = System.currentTimeMillis()
)

