package com.kreggscode.bmr.data.local.dao

import androidx.room.*
import com.kreggscode.bmr.data.local.entities.WaterIntake
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_intake WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getWaterIntakeByDate(userId: Long, date: Long): WaterIntake?
    
    @Query("SELECT * FROM water_intake WHERE userId = :userId AND date = :date LIMIT 1")
    fun getWaterIntakeByDateFlow(userId: Long, date: Long): Flow<WaterIntake?>
    
    @Query("SELECT * FROM water_intake WHERE userId = :userId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestWaterIntake(userId: Long): WaterIntake?
    
    @Query("""
        SELECT * FROM water_intake 
        WHERE userId = :userId 
        AND date >= :startDate 
        AND date <= :endDate 
        ORDER BY date DESC
    """)
    fun getWaterIntakeByDateRange(userId: Long, startDate: Long, endDate: Long): Flow<List<WaterIntake>>
    
    @Query("SELECT * FROM water_intake WHERE userId = :userId ORDER BY date DESC")
    fun getAllWaterIntake(userId: Long): Flow<List<WaterIntake>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterIntake(waterIntake: WaterIntake): Long
    
    @Update
    suspend fun updateWaterIntake(waterIntake: WaterIntake)
    
    @Delete
    suspend fun deleteWaterIntake(waterIntake: WaterIntake)
    
    @Query("DELETE FROM water_intake WHERE userId = :userId AND date = :date")
    suspend fun deleteWaterIntakeByDate(userId: Long, date: Long)
    
    @Query("DELETE FROM water_intake WHERE userId = :userId")
    suspend fun deleteAllWaterIntake(userId: Long)
}

