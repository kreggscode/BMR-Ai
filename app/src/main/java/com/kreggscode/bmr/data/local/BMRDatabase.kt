package com.kreggscode.bmr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kreggscode.bmr.data.local.dao.*
import com.kreggscode.bmr.data.local.entities.*

@Database(
    entities = [
        UserProfile::class,
        BMRRecord::class,
        FoodItem::class,
        MealEntry::class,
        DietPlan::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BMRDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun bmrDao(): BMRDao
    abstract fun foodDao(): FoodDao
    
    companion object {
        const val DATABASE_NAME = "bmr_database"
    }
}
