package com.kreggscode.bmr.data.local.dao

import androidx.room.*
import com.kreggscode.bmr.data.local.entities.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profiles WHERE id = :userId")
    fun getUserById(userId: Long): Flow<UserProfile?>
    
    @Query("SELECT * FROM user_profiles ORDER BY updatedAt DESC LIMIT 1")
    fun getCurrentUser(): Flow<UserProfile?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserProfile): Long
    
    @Update
    suspend fun updateUser(user: UserProfile)
    
    @Delete
    suspend fun deleteUser(user: UserProfile)
    
    @Query("SELECT EXISTS(SELECT 1 FROM user_profiles LIMIT 1)")
    suspend fun hasUser(): Boolean
}
