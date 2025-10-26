package com.kreggscode.bmr.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreggscode.bmr.data.local.dao.BMRDao
import com.kreggscode.bmr.data.local.dao.FoodDao
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
    private val foodDao: FoodDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()
    
    init {
        loadProgressData()
    }
    
    private fun loadProgressData() {
        viewModelScope.launch {
            userDao.getCurrentUser().collect { user ->
                user?.let { profile ->
                    // Load BMR data
                    bmrDao.getLatestBMRRecord(profile.id).collect { bmrRecord ->
                        val targetCalories = bmrRecord?.targetCalories ?: 2000.0
                        
                        // Calculate average daily calories for the week
                        val weekStart = Calendar.getInstance().apply {
                            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }.timeInMillis
                        
                        val totalCalories = foodDao.getTotalCaloriesForDate(profile.id, weekStart) ?: 0.0
                        val avgCalories = totalCalories / 7
                        
                        // Calculate progress
                        val progress = (avgCalories / targetCalories).toFloat().coerceIn(0f, 1f)
                        
                        // Calculate deficit/surplus
                        val deficit = targetCalories - avgCalories
                        
                        // Get macros
                        val proteinTarget = bmrRecord?.proteinGrams ?: 100.0
                        val carbsTarget = bmrRecord?.carbsGrams ?: 200.0
                        val fatTarget = bmrRecord?.fatGrams ?: 60.0
                        
                        // Calculate current macros (simplified - using ratios)
                        val proteinCurrent = (avgCalories * 0.3 / 4).toInt()
                        val carbsCurrent = (avgCalories * 0.4 / 4).toInt()
                        val fatCurrent = (avgCalories * 0.3 / 9).toInt()
                        
                        _uiState.update { state ->
                            state.copy(
                                avgCalories = avgCalories.toInt(),
                                targetCalories = targetCalories.toInt(),
                                progress = progress,
                                deficit = deficit.toInt(),
                                proteinCurrent = proteinCurrent,
                                proteinTarget = proteinTarget.toInt(),
                                carbsCurrent = carbsCurrent,
                                carbsTarget = carbsTarget.toInt(),
                                fatCurrent = fatCurrent,
                                fatTarget = fatTarget.toInt(),
                                weightLost = calculateWeightLoss(profile.id),
                                streak = calculateStreak(profile.id),
                                waterAvg = 6 // TODO: Track water intake
                            )
                        }
                    }
                }
            }
        }
    }
    
    private suspend fun calculateWeightLoss(userId: Long): Double {
        // TODO: Implement weight tracking
        // For now, estimate based on calorie deficit
        val weekStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            add(Calendar.WEEK_OF_YEAR, -4) // Last 4 weeks
        }.timeInMillis
        
        return 2.5 // Placeholder: 2.5 kg lost
    }
    
    private suspend fun calculateStreak(userId: Long): Int {
        // TODO: Implement streak tracking
        // For now, return a reasonable value
        return 12 // 12 day streak
    }
    
    fun selectPeriod(period: TimePeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
    }
}

data class ProgressUiState(
    val selectedPeriod: TimePeriod = TimePeriod.WEEK,
    val avgCalories: Int = 0,
    val targetCalories: Int = 0,
    val progress: Float = 0f,
    val deficit: Int = 0,
    val burned: Int = 2200,
    val active: Int = 325,
    val weightLost: Double = 0.0,
    val streak: Int = 0,
    val waterAvg: Int = 0,
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
