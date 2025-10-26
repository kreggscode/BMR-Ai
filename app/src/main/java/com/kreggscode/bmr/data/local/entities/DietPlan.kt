package com.kreggscode.bmr.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "diet_plans",
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
data class DietPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val daysJson: String, // JSON string containing meal plan for each day
    val totalCalories: Double,
    val macrosJson: String, // JSON string with protein, carbs, fat targets
    val dietaryPreferences: String? = null, // vegetarian, vegan, keto, etc.
    val allergies: String? = null,
    val shoppingListJson: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
