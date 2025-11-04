package com.kreggscode.bmr.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "sleep_records",
    foreignKeys = [
        ForeignKey(
            entity = UserProfile::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["userId"])]
)
data class SleepRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val date: Long, // Date at midnight (start of day)
    val sleepHours: Double, // Hours of sleep (e.g., 7.5 for 7 hours 30 minutes)
    val bedtime: Long, // Timestamp when user went to bed
    val wakeTime: Long, // Timestamp when user woke up
    val quality: Int = 0, // Sleep quality rating 0-5 (0 = poor, 5 = excellent)
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

