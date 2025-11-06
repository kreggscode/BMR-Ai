package com.kreggscode.bmr.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreggscode.bmr.data.local.dao.BMRDao
import com.kreggscode.bmr.data.local.dao.FoodDao
import com.kreggscode.bmr.data.local.dao.SleepDao
import com.kreggscode.bmr.data.local.dao.UserDao
import com.kreggscode.bmr.data.local.dao.WaterDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val userDao: UserDao,
    private val bmrDao: BMRDao,
    private val foodDao: FoodDao,
    private val sleepDao: SleepDao,
    private val waterDao: WaterDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()
    
    init {
        loadProgressData()
        loadUserWeight()
    }
    
    fun refreshData() {
        // Force reload by updating period (triggers recalculation)
        val currentPeriod = _uiState.value.selectedPeriod
        selectPeriod(currentPeriod)
    }
    
    private fun loadProgressData() {
        viewModelScope.launch {
            userDao.getCurrentUser().collect { user ->
                user?.let { profile ->
                    // Load BMR data
                    bmrDao.getLatestBMRRecord(profile.id).collect { bmrRecord ->
                        val targetCalories = bmrRecord?.targetCalories ?: 2000.0
                        
                        // Calculate average daily calories for the selected period
                        val period = _uiState.value.selectedPeriod
                        val (startDate, endDate, days) = when (period) {
                            TimePeriod.WEEK -> {
                                val start = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, -6)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                val end = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 23)
                                    set(Calendar.MINUTE, 59)
                                    set(Calendar.SECOND, 59)
                                    set(Calendar.MILLISECOND, 999)
                                }.timeInMillis
                                Triple(start, end, 7)
                            }
                            TimePeriod.MONTH -> {
                                val start = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, -29)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                val end = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 23)
                                    set(Calendar.MINUTE, 59)
                                    set(Calendar.SECOND, 59)
                                    set(Calendar.MILLISECOND, 999)
                                }.timeInMillis
                                Triple(start, end, 30)
                            }
                            TimePeriod.YEAR -> {
                                val start = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, -364)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                val end = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 23)
                                    set(Calendar.MINUTE, 59)
                                    set(Calendar.SECOND, 59)
                                    set(Calendar.MILLISECOND, 999)
                                }.timeInMillis
                                Triple(start, end, 365)
                            }
                        }
                        
                        // Get all meals in the period
                        val meals = foodDao.getMealsByDateRange(profile.id, startDate, endDate).first()
                        val totalCalories = meals.sumOf { it.caloriesCalculated }
                        val avgCalories = if (days > 0) totalCalories / days else 0.0
                        
                        // Get today's calories
                        val today = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val todayMeals = meals.filter { it.date >= today }
                        val todayCalories = todayMeals.sumOf { it.caloriesCalculated }
                        
                        // Calculate progress based on period
                        // For WEEK: show weekly average (total calories / 7 days)
                        // For MONTH: show monthly average (total calories / 30 days)
                        // For YEAR: show yearly average (total calories / 365 days)
                        val caloriesForProgress = when (period) {
                            TimePeriod.WEEK -> {
                                // Weekly: show average of last 7 days
                                val weekAvg = if (days > 0) totalCalories / days else 0.0
                                weekAvg
                            }
                            TimePeriod.MONTH -> {
                                // Monthly: show average of last 30 days
                                avgCalories
                            }
                            TimePeriod.YEAR -> {
                                // Yearly: show average of last 365 days
                                avgCalories
                            }
                        }
                        val progress = (caloriesForProgress / targetCalories).toFloat().coerceIn(0f, 1f)
                        
                        // Calculate deficit/surplus based on period
                        val deficit = targetCalories - caloriesForProgress
                        
                        // Get macros
                        val proteinTarget = bmrRecord?.proteinGrams ?: 100.0
                        val carbsTarget = bmrRecord?.carbsGrams ?: 200.0
                        val fatTarget = bmrRecord?.fatGrams ?: 60.0
                        
                        // Calculate current macros from actual logged meals
                        val proteinCurrent = if (meals.isNotEmpty()) {
                            (meals.sumOf { it.proteinCalculated } / days).toInt()
                        } else {
                            (avgCalories * 0.3 / 4).toInt()
                        }
                        val carbsCurrent = if (meals.isNotEmpty()) {
                            (meals.sumOf { it.carbsCalculated } / days).toInt()
                        } else {
                            (avgCalories * 0.4 / 4).toInt()
                        }
                        val fatCurrent = if (meals.isNotEmpty()) {
                            (meals.sumOf { it.fatCalculated } / days).toInt()
                        } else {
                            (avgCalories * 0.3 / 9).toInt()
                        }
                        
                        // Calculate BMR and TDEE
                        val bmr = bmrRecord?.bmrValue ?: 0.0
                        val tdee = bmrRecord?.targetCalories ?: bmr
                        val burned = bmr.toInt()
                        val active = (tdee - bmr).toInt().coerceAtLeast(0)
                        
                        // Calculate water average for the period
                        val waterAvg = calculateWaterAverage(profile.id, startDate, endDate, days)
                        
                        _uiState.update { state ->
                            state.copy(
                                avgCalories = avgCalories.toInt(),
                                todayCalories = todayCalories.toInt(),
                                targetCalories = targetCalories.toInt(),
                                progress = progress,
                                deficit = deficit.toInt(),
                                burned = burned,
                                active = active,
                                proteinCurrent = proteinCurrent,
                                proteinTarget = proteinTarget.toInt(),
                                carbsCurrent = carbsCurrent,
                                carbsTarget = carbsTarget.toInt(),
                                fatCurrent = fatCurrent,
                                fatTarget = fatTarget.toInt(),
                                weightLost = calculateWeightLoss(profile.id),
                                streak = calculateStreak(profile.id),
                                waterAvg = waterAvg,
                                sleepAvg = calculateSleepAverage(profile.id)
                            )
                        }
                    }
                }
            }
        }
    }
    
    private suspend fun calculateWeightLoss(userId: Long): Double {
        // Get user's current weight
        val user = userDao.getCurrentUser().firstOrNull() ?: return 0.0
        val currentWeight = user.weightKg
        
        // Get all BMR records to find the earliest one (starting weight)
        val allBMRRecords = bmrDao.getUserBMRRecords(userId).first()
        val startWeight = if (allBMRRecords.isNotEmpty()) {
            // Use the weight from the oldest BMR record as start weight
            // The user's weight at the time of first BMR calculation
            val oldestBMR = allBMRRecords.last() // Records are ordered DESC, so last is oldest
            // Get the user's weight when this BMR was calculated
            // Since we don't store historical weights, use current weight if it's > 0
            // Otherwise, we'll track it from the user profile which gets updated when BMR is saved
            currentWeight // This will be updated when BMR is calculated
        } else {
            currentWeight
        }
        
        // Calculate actual weight loss (start - current)
        // For now, if no weight history exists, return 0
        // Weight loss tracking requires storing historical weight data
        if (startWeight > 0 && currentWeight > 0 && startWeight > currentWeight) {
            return startWeight - currentWeight
        }
        
        return 0.0
    }
    
    private suspend fun calculateStreak(userId: Long): Int {
        // Count consecutive days with logged food entries
        var streak = 0
        val calendar = Calendar.getInstance()
        
        for (day in 0..30) { // Check up to 30 days back
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -day)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            val dayStart = calendar.timeInMillis
            val calories = foodDao.getTotalCaloriesForDate(userId, dayStart) ?: 0.0
            
            if (calories > 0) {
                streak++
            } else {
                break // Streak broken
            }
        }
        
        return streak
    }
    
    fun selectPeriod(period: TimePeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        // Reload data with new period
        loadProgressData()
    }
    
    private suspend fun calculateSleepAverage(userId: Long): Int {
        val sevenDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -6)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        
        // First try to get today's sleep if available (most recent)
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val todayRecord = sleepDao.getSleepRecordByDate(userId, today)
        if (todayRecord != null && todayRecord.sleepHours > 0) {
            // For weekly view, prefer showing today's hours
            return todayRecord.sleepHours.toInt()
        }
        
        // Otherwise calculate average
        val avgSleep = sleepDao.getAverageSleepHours(userId, sevenDaysAgo, todayEnd)
        return avgSleep?.toInt() ?: 0
    }
    
    private suspend fun calculateWaterAverage(userId: Long, startDate: Long, endDate: Long, days: Int): Int {
        try {
            // First try to get today's water intake (most recent)
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val todayRecord = waterDao.getWaterIntakeByDate(userId, today)
            if (todayRecord != null && todayRecord.glasses > 0) {
                // For WEEK period, show today's glasses directly
                // For other periods, calculate average
                if (days == 7) {
                    return todayRecord.glasses
                }
            }
            
            // Get water records for the date range
            val waterRecords = waterDao.getWaterIntakeByDateRange(userId, startDate, endDate).first()
            
            if (waterRecords.isEmpty()) {
                // If no records in range, try today as fallback
                if (todayRecord != null) {
                    android.util.Log.d("ProgressViewModel", "Using today's water: ${todayRecord.glasses} glasses")
                    return todayRecord.glasses
                }
                android.util.Log.d("ProgressViewModel", "No water records found")
                return 0
            }
            
            // Calculate total glasses and average
            val totalGlasses = waterRecords.sumOf { it.glasses }
            val avgGlasses = if (days > 0 && days <= 7) {
                // For weekly view, prefer showing today if available
                if (todayRecord != null && todayRecord.glasses > 0) {
                    todayRecord.glasses
                } else {
                    (totalGlasses.toDouble() / days).toInt()
                }
            } else if (days > 0) {
                // For monthly/yearly, calculate average
                (totalGlasses.toDouble() / days).toInt()
            } else {
                // If no days specified, return total glasses
                totalGlasses
            }
            
            android.util.Log.d("ProgressViewModel", "Water calculated: $avgGlasses glasses from ${waterRecords.size} records over $days days")
            return avgGlasses
        } catch (e: Exception) {
            android.util.Log.e("ProgressViewModel", "Error calculating water average", e)
            // Try to get today's water as last resort
            try {
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val todayRecord = waterDao.getWaterIntakeByDate(userId, today)
                return todayRecord?.glasses ?: 0
            } catch (ex: Exception) {
                return 0
            }
        }
    }
    
    fun loadUserWeight() {
        viewModelScope.launch {
            userDao.getCurrentUser().collect { user ->
                user?.let { profile ->
                    val currentWeight = profile.weightKg
                    
                    // Get start weight from the first BMR calculation
                    // When BMR is first calculated, we store that weight as the starting point
                    val allBMRRecords = bmrDao.getUserBMRRecords(profile.id).first()
                    val startWeight = if (allBMRRecords.isNotEmpty() && _uiState.value.startWeight == 0.0) {
                        // Use current weight as start weight if this is the first time loading
                        // The start weight should be set when the first BMR is calculated
                        currentWeight
                    } else if (_uiState.value.startWeight > 0) {
                        // Keep existing start weight if already set
                        _uiState.value.startWeight
                    } else {
                        // If no start weight is set, use current weight
                        currentWeight
                    }
                    
                    // Calculate weight loss (start - current, positive means weight lost)
                    val weightLost = if (startWeight > 0 && currentWeight > 0 && startWeight > currentWeight) {
                        startWeight - currentWeight
                    } else {
                        0.0
                    }
                    
                    _uiState.update { state ->
                        state.copy(
                            currentWeight = currentWeight,
                            startWeight = startWeight,
                            weightLost = weightLost
                        )
                    }
                }
            }
        }
    }
    
    fun updateWeight(newWeight: Double) {
        viewModelScope.launch {
            val user = userDao.getCurrentUser().firstOrNull()
            user?.let { profile ->
                val updatedUser = profile.copy(
                    weightKg = newWeight,
                    updatedAt = System.currentTimeMillis()
                )
                userDao.updateUser(updatedUser)
                
                // Reload weight data
                loadUserWeight()
            }
        }
    }
    
    fun toggleWeightDialog(show: Boolean) {
        _uiState.update { it.copy(showWeightDialog = show) }
    }
}

data class ProgressUiState(
    val selectedPeriod: TimePeriod = TimePeriod.WEEK,
    val avgCalories: Int = 0,
    val todayCalories: Int = 0, // Today's consumed calories
    val targetCalories: Int = 0,
    val progress: Float = 0f,
    val deficit: Int = 0,
    val burned: Int = 2200,
    val active: Int = 325,
    val weightLost: Double = 0.0,
    val currentWeight: Double = 0.0, // Current weight from user profile
    val startWeight: Double = 0.0, // Starting weight (from BMR record or first weight)
    val streak: Int = 0,
    val waterAvg: Int = 0,
    val sleepAvg: Int = 0,
    val proteinCurrent: Int = 0,
    val proteinTarget: Int = 0,
    val carbsCurrent: Int = 0,
    val carbsTarget: Int = 0,
    val fatCurrent: Int = 0,
    val fatTarget: Int = 0,
    val showWeightDialog: Boolean = false
)

enum class TimePeriod(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year")
}
