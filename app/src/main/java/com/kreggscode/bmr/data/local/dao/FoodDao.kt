package com.kreggscode.bmr.data.local.dao

import androidx.room.*
import com.kreggscode.bmr.data.local.entities.FoodItem
import com.kreggscode.bmr.data.local.entities.MealEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_items ORDER BY name")
    fun getAllFoodItems(): Flow<List<FoodItem>>
    
    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' ORDER BY name")
    fun searchFoodItems(query: String): Flow<List<FoodItem>>
    
    @Query("SELECT * FROM food_items WHERE id = :foodId")
    suspend fun getFoodItemById(foodId: Long): FoodItem?
    
    @Query("SELECT * FROM food_items WHERE barcode = :barcode")
    suspend fun getFoodItemByBarcode(barcode: String): FoodItem?
    
    @Query("SELECT * FROM meal_entries WHERE id = :mealId")
    suspend fun getMealEntryById(mealId: Long): MealEntry?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(foodItem: FoodItem): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItems(foodItems: List<FoodItem>)
    
    @Update
    suspend fun updateFoodItem(foodItem: FoodItem)
    
    @Delete
    suspend fun deleteFoodItem(foodItem: FoodItem)
    
    // Meal entries
    @Query("SELECT * FROM meal_entries WHERE userId = :userId AND date = :date ORDER BY mealType, timestamp")
    fun getMealsByDate(userId: Long, date: Long): Flow<List<MealEntry>>
    
    @Query("""
        SELECT * FROM meal_entries 
        WHERE userId = :userId 
        AND date >= :startDate 
        AND date <= :endDate 
        ORDER BY date DESC, mealType
    """)
    fun getMealsByDateRange(userId: Long, startDate: Long, endDate: Long): Flow<List<MealEntry>>
    
    @Query("""
        SELECT SUM(caloriesCalculated) 
        FROM meal_entries 
        WHERE userId = :userId AND date = :date
    """)
    suspend fun getTotalCaloriesForDate(userId: Long, date: Long): Double?
    
    @Insert
    suspend fun insertMealEntry(mealEntry: MealEntry): Long
    
    @Update
    suspend fun updateMealEntry(mealEntry: MealEntry)
    
    @Delete
    suspend fun deleteMealEntry(mealEntry: MealEntry)
    
    @Query("DELETE FROM meal_entries WHERE userId = :userId AND date = :date")
    suspend fun deleteMealsByDate(userId: Long, date: Long)
    
    @Query("DELETE FROM meal_entries WHERE userId = :userId")
    suspend fun deleteMealEntriesForUser(userId: Long)
    
    @Query("DELETE FROM food_items")
    suspend fun deleteAllFoodItems()
}
