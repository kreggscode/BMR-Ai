package com.kreggscode.bmr.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val servingSize: String,
    val servingUnit: String,
    val barcode: String? = null,
    val imageUrl: String? = null,
    val category: String? = null,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
