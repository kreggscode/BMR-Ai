package com.kreggscode.bmr.data.local.dao

import androidx.room.*
import com.kreggscode.bmr.data.local.entities.SleepRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_records WHERE userId = :userId ORDER BY date DESC")
    fun getSleepRecordsByUser(userId: Long): Flow<List<SleepRecord>>
    
    @Query("SELECT * FROM sleep_records WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getSleepRecordByDate(userId: Long, date: Long): SleepRecord?
    
    @Query("""
        SELECT * FROM sleep_records 
        WHERE userId = :userId 
        AND date >= :startDate 
        AND date <= :endDate 
        ORDER BY date DESC
    """)
    fun getSleepRecordsByDateRange(userId: Long, startDate: Long, endDate: Long): Flow<List<SleepRecord>>
    
    @Query("""
        SELECT AVG(sleepHours) 
        FROM sleep_records 
        WHERE userId = :userId 
        AND date >= :startDate 
        AND date <= :endDate
    """)
    suspend fun getAverageSleepHours(userId: Long, startDate: Long, endDate: Long): Double?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepRecord(sleepRecord: SleepRecord): Long
    
    @Update
    suspend fun updateSleepRecord(sleepRecord: SleepRecord)
    
    @Delete
    suspend fun deleteSleepRecord(sleepRecord: SleepRecord)
    
    @Query("DELETE FROM sleep_records WHERE userId = :userId AND date = :date")
    suspend fun deleteSleepRecordByDate(userId: Long, date: Long)
}

