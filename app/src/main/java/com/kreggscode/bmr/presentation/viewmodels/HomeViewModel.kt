package com.kreggscode.bmr.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreggscode.bmr.data.local.dao.BMRDao
import com.kreggscode.bmr.data.local.dao.FoodDao
import com.kreggscode.bmr.data.local.dao.UserDao
import com.kreggscode.bmr.presentation.screens.RecentMeal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userDao: UserDao,
    private val bmrDao: BMRDao,
    private val foodDao: FoodDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadUserData()
        loadTodayStats()
        loadRecentMeals()
        refreshMotivation()
    }
    
    private fun loadUserData() {
        viewModelScope.launch {
            userDao.getCurrentUser().collect { user ->
                _uiState.update { state ->
                    state.copy(userName = user?.name ?: "Guest")
                }
            }
        }
    }
    
    private fun loadTodayStats() {
        viewModelScope.launch {
            userDao.getCurrentUser().collect { user ->
                user?.let { profile ->
                    bmrDao.getLatestBMRRecord(profile.id).collect { record ->
                        val bmr = record?.bmrValue ?: 0.0
                        val targetCalories = record?.targetCalories ?: bmr
                        
                        // Get today's consumed calories
                        val today = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        
                        val consumed = foodDao.getTotalCaloriesForDate(profile.id, today) ?: 0.0
                        
                        _uiState.update { state ->
                            state.copy(
                                bmr = bmr,
                                targetCalories = targetCalories,
                                caloriesConsumed = consumed,
                                caloriesRemaining = (targetCalories - consumed).coerceAtLeast(0.0)
                            )
                        }
                    }
                }
            }
        }
    }
    
    private fun loadRecentMeals() {
        viewModelScope.launch {
            userDao.getCurrentUser().collect { user ->
                user?.let { profile ->
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    
                    foodDao.getMealsByDate(profile.id, today).collect { meals ->
                        val recentMeals = meals.take(5).map { entry ->
                            val foodItem = foodDao.getFoodItemById(entry.foodItemId)
                            RecentMeal(
                                type = entry.mealType.capitalize(),
                                name = foodItem?.name ?: "Unknown",
                                calories = entry.caloriesCalculated.toInt(),
                                time = formatTime(entry.timestamp)
                            )
                        }
                        
                        _uiState.update { state ->
                            state.copy(recentMeals = recentMeals)
                        }
                    }
                }
            }
        }
    }
    
    fun refreshMotivation() {
        val quotes = listOf(
            "Every healthy choice is a step towards a better you!",
            "Your body is your temple. Keep it pure and clean for the soul to reside in.",
            "Take care of your body. It's the only place you have to live.",
            "A healthy outside starts from the inside.",
            "The groundwork for all happiness is good health.",
            "Health is not about the weight you lose, but about the life you gain.",
            "Your health is an investment, not an expense.",
            "Today is a good day to start living healthy!",
            "Small steps daily lead to big changes yearly.",
            "You are what you eat, so don't be fast, cheap, easy, or fake."
        )
        
        _uiState.update { state ->
            state.copy(motivationalQuote = quotes.random())
        }
    }
    
    fun incrementWater() {
        _uiState.update { state ->
            state.copy(waterIntake = state.waterIntake + 1)
        }
    }
    
    fun showWaterDialog(show: Boolean) {
        _uiState.update { it.copy(showWaterDialog = show) }
    }
    
    fun addWater(glassSize: Int) {
        _uiState.update { state ->
            state.copy(
                waterIntake = state.waterIntake + 1,
                totalWaterMl = state.totalWaterMl + glassSize,
                showWaterDialog = false
            )
        }
    }
    
    fun resetWater() {
        _uiState.update { state ->
            state.copy(
                waterIntake = 0,
                totalWaterMl = 0
            )
        }
    }
    
    private fun formatTime(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return String.format("%02d:%02d", 
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
    }
}

data class HomeUiState(
    val userName: String = "Guest",
    val bmr: Double = 0.0,
    val targetCalories: Double = 0.0,
    val caloriesConsumed: Double = 0.0,
    val caloriesRemaining: Double = 0.0,
    val waterIntake: Int = 0,
    val totalWaterMl: Int = 0,
    val showWaterDialog: Boolean = false,
    val motivationalQuote: String = "",
    val recentMeals: List<RecentMeal> = emptyList(),
    val isLoading: Boolean = false
)
