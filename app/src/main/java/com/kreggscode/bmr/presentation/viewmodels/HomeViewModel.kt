package com.kreggscode.bmr.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreggscode.bmr.data.local.dao.BMRDao
import com.kreggscode.bmr.data.local.dao.FoodDao
import com.kreggscode.bmr.data.local.dao.UserDao
import com.kreggscode.bmr.data.local.dao.WaterDao
import com.kreggscode.bmr.data.local.entities.WaterIntake
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
    private val foodDao: FoodDao,
    private val waterDao: WaterDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadUserData()
        loadTodayStats()
        loadRecentMeals()
        loadTodayWater()
        refreshMotivation()
    }
    
    private fun loadTodayWater() {
        viewModelScope.launch {
            userDao.getCurrentUser().collect { user ->
                user?.let { profile ->
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    
                    // Use Flow to auto-update when water changes
                    waterDao.getWaterIntakeByDateFlow(profile.id, today).collect { todayRecord ->
                        _uiState.update { state ->
                            state.copy(
                                waterIntake = todayRecord?.glasses ?: 0,
                                totalWaterMl = todayRecord?.totalMl ?: 0
                            )
                        }
                    }
                }
            }
        }
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
    
    fun refreshTodayStats() {
        viewModelScope.launch {
            val user = userDao.getCurrentUser().firstOrNull()
            user?.let { profile ->
                val record = bmrDao.getLatestBMRRecord(profile.id).first()
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
    
    private fun loadTodayStats() {
        viewModelScope.launch {
            userDao.getCurrentUser().collect { user ->
                user?.let { profile ->
                    bmrDao.getLatestBMRRecord(profile.id).collect { record ->
                        val bmr = record?.bmrValue ?: 0.0
                        val targetCalories = record?.targetCalories ?: bmr
                        
                        // Get today's consumed calories - use Flow for real-time updates
                        val today = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        
                        // Use Flow to get real-time updates
                        foodDao.getMealsByDate(profile.id, today).collect { meals ->
                            val consumed = meals.sumOf { it.caloriesCalculated }
                            
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
        viewModelScope.launch {
            val user = userDao.getCurrentUser().firstOrNull() ?: return@launch
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val existing = waterDao.getWaterIntakeByDate(user.id, today)
            
            if (existing != null) {
                val updated = existing.copy(
                    totalMl = existing.totalMl + glassSize,
                    glasses = existing.glasses + 1,
                    lastUpdated = System.currentTimeMillis()
                )
                waterDao.updateWaterIntake(updated)
            } else {
                val newRecord = WaterIntake(
                    userId = user.id,
                    date = today,
                    totalMl = glassSize,
                    glasses = 1,
                    lastUpdated = System.currentTimeMillis()
                )
                waterDao.insertWaterIntake(newRecord)
            }
            
            // Reload water data
            loadTodayWater()
            
            _uiState.update { it.copy(showWaterDialog = false) }
        }
    }
    
    fun resetWater() {
        viewModelScope.launch {
            val user = userDao.getCurrentUser().firstOrNull() ?: return@launch
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            waterDao.deleteWaterIntakeByDate(user.id, today)
            loadTodayWater()
        }
    }
    
    fun removeWater(glassSize: Int) {
        viewModelScope.launch {
            val user = userDao.getCurrentUser().firstOrNull() ?: return@launch
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val existing = waterDao.getWaterIntakeByDate(user.id, today)
            existing?.let { record ->
                val newMl = (record.totalMl - glassSize).coerceAtLeast(0)
                val newGlasses = (record.glasses - 1).coerceAtLeast(0)
                
                if (newMl == 0 && newGlasses == 0) {
                    waterDao.deleteWaterIntakeByDate(user.id, today)
                } else {
                    val updated = record.copy(
                        totalMl = newMl,
                        glasses = newGlasses,
                        lastUpdated = System.currentTimeMillis()
                    )
                    waterDao.updateWaterIntake(updated)
                }
            }
            
            loadTodayWater()
        }
    }
    
    fun saveWaterIntake() {
        // Save current water intake when navigating away
        // This ensures water is saved when user presses back
        // TODO: Implement water persistence
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
