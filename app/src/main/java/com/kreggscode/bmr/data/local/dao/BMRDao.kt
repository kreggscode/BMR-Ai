package com.kreggscode.bmr.data.local.dao

import androidx.room.*
import com.kreggscode.bmr.data.local.entities.BMRRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface BMRDao {
    @Query("SELECT * FROM bmr_records WHERE userId = :userId ORDER BY timestamp DESC")
    fun getUserBMRRecords(userId: Long): Flow<List<BMRRecord>>
    
    @Query("SELECT * FROM bmr_records WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestBMRRecord(userId: Long): Flow<BMRRecord?>
    
    @Query("SELECT * FROM bmr_records WHERE userId = :userId AND timestamp >= :startDate AND timestamp <= :endDate ORDER BY timestamp")
    fun getBMRRecordsByDateRange(userId: Long, startDate: Long, endDate: Long): Flow<List<BMRRecord>>
    
    @Insert
    suspend fun insertBMRRecord(record: BMRRecord): Long
    
    @Update
    suspend fun updateBMRRecord(record: BMRRecord)
    
    @Delete
    suspend fun deleteBMRRecord(record: BMRRecord)
    
    @Query("DELETE FROM bmr_records WHERE userId = :userId")
    suspend fun deleteAllUserBMRRecords(userId: Long)
    
    @Query("DELETE FROM bmr_records WHERE userId = :userId")
    suspend fun deleteBMRRecordsForUser(userId: Long)
}
