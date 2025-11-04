package com.kreggscode.bmr.data.local.dao

import androidx.room.*
import com.kreggscode.bmr.data.local.entities.DietPlan
import kotlinx.coroutines.flow.Flow

@Dao
interface DietPlanDao {
    @Query("SELECT * FROM diet_plans WHERE userId = :userId ORDER BY createdAt DESC")
    fun getDietPlansByUser(userId: Long): Flow<List<DietPlan>>
    
    @Query("SELECT * FROM diet_plans WHERE userId = :userId AND isActive = 1 LIMIT 1")
    fun getActiveDietPlan(userId: Long): Flow<DietPlan?>
    
    @Query("SELECT * FROM diet_plans WHERE id = :planId")
    suspend fun getDietPlanById(planId: Long): DietPlan?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDietPlan(dietPlan: DietPlan): Long
    
    @Update
    suspend fun updateDietPlan(dietPlan: DietPlan)
    
    @Delete
    suspend fun deleteDietPlan(dietPlan: DietPlan)
    
    @Query("UPDATE diet_plans SET isActive = 0 WHERE userId = :userId")
    suspend fun deactivateAllPlans(userId: Long)
    
    @Query("UPDATE diet_plans SET isActive = 1 WHERE id = :planId")
    suspend fun activatePlan(planId: Long)
}

