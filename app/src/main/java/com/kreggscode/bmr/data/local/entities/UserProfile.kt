package com.kreggscode.bmr.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dateOfBirth: Long,
    val sex: String,
    val heightCm: Double,
    val weightKg: Double,
    val activityLevel: String,
    val units: String = "metric",
    val goalType: String = "maintain", // lose, maintain, gain
    val goalRate: Double = 0.0, // kg per week
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
