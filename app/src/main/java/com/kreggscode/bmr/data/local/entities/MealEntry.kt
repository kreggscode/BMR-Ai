package com.kreggscode.bmr.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_entries",
    foreignKeys = [
        ForeignKey(
            entity = UserProfile::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["userId"]),
        androidx.room.Index(value = ["foodItemId"])
    ]
)
data class MealEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val foodItemId: Long,
    val date: Long,
    val mealType: String, // breakfast, lunch, dinner, snack
    val quantity: Double,
    val caloriesCalculated: Double,
    val proteinCalculated: Double,
    val carbsCalculated: Double,
    val fatCalculated: Double,
    val source: String = "manual", // manual, scanner, barcode
    val timestamp: Long = System.currentTimeMillis()
)
