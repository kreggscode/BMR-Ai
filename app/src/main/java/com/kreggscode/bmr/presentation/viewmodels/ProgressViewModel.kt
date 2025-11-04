package com.kreggscode.bmr.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreggscode.bmr.data.local.dao.BMRDao
import com.kreggscode.bmr.data.local.dao.FoodDao
import com.kreggscode.bmr.data.local.dao.SleepDao
import com.kreggscode.bmr.data.local.dao.UserDao
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
    private val sleepDao: SleepDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()
    
    init {
        loadProgressData()
        loadUserWeight()
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
                        
                        // Calculate progress based on period - use today for week, average for month/year
                        val caloriesForProgress = when (period) {
                            TimePeriod.WEEK -> todayCalories
                            else -> avgCalories
                        }
                        val progress = (caloriesForProgress / targetCalories).toFloat().coerceIn(0f, 1f)
                        
                        // Calculate deficit/surplus (for today when week is selected)
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
                                waterAvg = 0, // Water intake tracked separately in WaterTrackingScreen
                                sleepAvg = calculateSleepAverage(profile.id)
                            )
                        }
                    }
                }
            }
        }
    }
    
    private suspend fun calculateWeightLoss(userId: Long): Double {
        // Get user's current weight and start weight from state
        val user = userDao.getCurrentUser().firstOrNull() ?: return 0.0
        val currentWeight = user.weightKg
        
        // Get start weight from first BMR record or use current if no start weight
        val firstBMR = bmrDao.getLatestBMRRecord(userId).firstOrNull()
        val startWeight = firstBMR?.let {
            // Try to get initial weight from when BMR was calculated
            // For now, we'll use the current weight as start if no history
            currentWeight
        } ?: currentWeight
        
        // Calculate actual weight loss (start - current)
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
        
        val avgSleep = sleepDao.getAverageSleepHours(userId, sevenDaysAgo, todayEnd)
        return avgSleep?.toInt() ?: 0
    }
    
    fun loadUserWeight() {
        viewModelScope.launch {
            userDao.getCurrentUser().collect { user ->
                user?.let { profile ->
                    val currentWeight = profile.weightKg
                    
                    // Get start weight - use first weight entry or current weight
                    // For now, we'll track start weight from first BMR calculation
                    // In a real app, you'd have a weight history table
                    val startWeight = if (_uiState.value.startWeight == 0.0) {
                        currentWeight // Use current as start if no start weight set
                    } else {
                        _uiState.value.startWeight
                    }
                    
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
    val fatTarget: Int = 0
)

enum class TimePeriod(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year")
}
