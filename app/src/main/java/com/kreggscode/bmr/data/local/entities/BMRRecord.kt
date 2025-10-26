package com.kreggscode.bmr.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "bmr_records",
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
data class BMRRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val formula: String, // mifflin or harris
    val bmrValue: Double,
    val tdeeValue: Double,
    val activityMultiplier: Double,
    val targetCalories: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double
)
