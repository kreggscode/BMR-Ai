package com.kreggscode.bmr.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreggscode.bmr.data.local.dao.BMRDao
import com.kreggscode.bmr.data.local.dao.FoodDao
import com.kreggscode.bmr.data.local.dao.UserDao
import com.kreggscode.bmr.data.api.PollinationsAIService
import com.kreggscode.bmr.presentation.screens.MealLogItem
import com.kreggscode.bmr.presentation.screens.FavoriteFoodItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class FoodLogsViewModel @Inject constructor(
    private val userDao: UserDao,
    private val foodDao: FoodDao,
    private val bmrDao: BMRDao,
    private val aiService: PollinationsAIService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FoodLogsUiState())
    val uiState: StateFlow<FoodLogsUiState> = _uiState.asStateFlow()
    
    init {
        loadFoodLogs()
    }
    
    private fun loadFoodLogs() {
        viewModelScope.launch {
            userDao.getCurrentUser().collect { user ->
                user?.let { profile ->
                    // Load today's meals
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    
                    // Calculate weekly date range
                    val sevenDaysAgo = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -6)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    
                    val todayEnd = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    
                    // Load BMR record, today's meals, weekly meals, and all foods together
                    combine(
                        bmrDao.getLatestBMRRecord(profile.id),
                        foodDao.getMealsByDate(profile.id, today),
                        foodDao.getMealsByDateRange(profile.id, sevenDaysAgo, todayEnd),
                        foodDao.getAllFoodItems()
                    ) { bmrRecord, todayMeals, weeklyMeals, allFoods ->
                        val targetCalories = bmrRecord?.targetCalories ?: 2000.0
                        val foodMap = allFoods.associateBy { it.id }
                        
                        val mealItems = todayMeals.map { entry ->
                            val foodItem = foodMap[entry.foodItemId]
                            MealLogItem(
                                id = entry.id,
                                foodName = foodItem?.name ?: "Unknown",
                                mealType = entry.mealType,
                                time = formatTime(entry.timestamp),
                                calories = entry.caloriesCalculated,
                                protein = entry.proteinCalculated,
                                carbs = entry.carbsCalculated,
                                fat = entry.fatCalculated,
                                date = entry.date,
                                timestamp = entry.timestamp,
                                source = entry.source,
                                isFavorite = favoriteMealIds.contains(entry.id)
                            )
                        }
                        
                        // Calculate today's totals
                        val totalCalories = todayMeals.sumOf { it.caloriesCalculated }
                        val totalProtein = todayMeals.sumOf { it.proteinCalculated }
                        val totalCarbs = todayMeals.sumOf { it.carbsCalculated }
                        val totalFat = todayMeals.sumOf { it.fatCalculated }
                        val remainingCalories = (targetCalories - totalCalories).coerceAtLeast(0.0)
                        
                        // Calculate weekly calorie data for tracker graph
                        val weeklyData = (0..6).map { dayOffset ->
                            val calendar = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, -dayOffset)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val dayDate = calendar.timeInMillis
                            
                            // Filter meals for this specific day (date field matches dayDate)
                            val dayMeals = weeklyMeals.filter { 
                                it.date == dayDate
                            }
                            val dayCalories = dayMeals.sumOf { it.caloriesCalculated }
                            
                            val dayLabel = when (dayOffset) {
                                0 -> "Today"
                                1 -> "Yesterday"
                                else -> {
                                    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                                    dateFormat.format(calendar.time)
                                }
                            }
                            
                            DailyCalorieData(
                                date = dayDate,
                                calories = dayCalories,
                                dayLabel = dayLabel
                            )
                        }.reversed() // Oldest to newest (left to right)
                        
                        _uiState.update { state ->
                            state.copy(
                                todayMeals = mealItems,
                                todayTotalCalories = totalCalories,
                                todayTotalProtein = totalProtein,
                                todayTotalCarbs = totalCarbs,
                                todayTotalFat = totalFat,
                                targetCalories = targetCalories,
                                caloriesRemaining = remainingCalories,
                                weeklyCalorieData = weeklyData
                            )
                        }
                    }.collect { }
                    
                    // Load history (last 30 days, excluding today)
                    val thirtyDaysAgo = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -30)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    
                    // Exclude today - use yesterday as end date
                    val yesterdayEnd = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -1)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    
                    foodDao.getMealsByDateRange(profile.id, thirtyDaysAgo, yesterdayEnd).collect { meals ->
                        val mealItems = meals.map { entry ->
                            val foodItem = foodDao.getFoodItemById(entry.foodItemId)
                            MealLogItem(
                                id = entry.id,
                                foodName = foodItem?.name ?: "Unknown",
                                mealType = entry.mealType,
                                time = formatTime(entry.timestamp),
                                calories = entry.caloriesCalculated,
                                protein = entry.proteinCalculated,
                                carbs = entry.carbsCalculated,
                                fat = entry.fatCalculated,
                                date = entry.date,
                                timestamp = entry.timestamp,
                                source = entry.source,
                                isFavorite = favoriteMealIds.contains(entry.id)
                            )
                        }.sortedByDescending { it.date } // Sort by date descending (newest first)
                        
                        _uiState.update { state ->
                            state.copy(
                                historyMeals = mealItems
                            )
                        }
                    }
                    
                    // Load favorites (foods logged more than 3 times)
                    foodDao.getAllFoodItems().collect { foods ->
                        val favorites = foods
                            .filter { it.isCustom }
                            .map { food ->
                                FavoriteFoodItem(
                                    id = food.id,
                                    name = food.name,
                                    calories = food.calories,
                                    protein = food.protein,
                                    carbs = food.carbs,
                                    fat = food.fat
                                )
                            }
                            .take(20) // Limit to 20 favorites
                        
                        _uiState.update { state ->
                            state.copy(favoriteFoods = favorites)
                        }
                    }
                }
            }
        }
    }
    
    private val _navigateToAI = MutableStateFlow<String?>(null)
    val navigateToAI: StateFlow<String?> = _navigateToAI.asStateFlow()
    
    fun askAIAboutMeal(meal: MealLogItem) {
        val prompt = """
            Analyze this meal:
            Food: ${meal.foodName}
            Calories: ${meal.calories.toInt()} kcal
            Protein: ${meal.protein.toInt()}g
            Carbs: ${meal.carbs.toInt()}g
            Fat: ${meal.fat.toInt()}g
            Meal Type: ${meal.mealType}
            
            Provide nutritional insights, suggestions for improvement, and how this meal fits into a balanced diet.
        """.trimIndent()
        
        _navigateToAI.value = prompt
    }
    
    fun clearNavigation() {
        _navigateToAI.value = null
    }
    
    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            try {
                val mealEntry = foodDao.getMealEntryById(mealId)
                mealEntry?.let {
                    foodDao.deleteMealEntry(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("FoodLogs", "Error deleting meal: ${e.message}")
            }
        }
    }
    
    fun addFavoriteToToday(food: FavoriteFoodItem) {
        viewModelScope.launch {
            val user = userDao.getCurrentUser().firstOrNull() ?: return@launch
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            // Create meal entry from favorite
            val mealEntry = com.kreggscode.bmr.data.local.entities.MealEntry(
                userId = user.id,
                foodItemId = food.id,
                date = today,
                mealType = getMealType(),
                quantity = 1.0,
                caloriesCalculated = food.calories,
                proteinCalculated = food.protein,
                carbsCalculated = food.carbs,
                fatCalculated = food.fat,
                source = "favorite"
            )
            
            foodDao.insertMealEntry(mealEntry)
        }
    }
    
    fun removeFavorite(foodId: Long) {
        viewModelScope.launch {
            // TODO: Implement favorite removal logic
            android.util.Log.d("FoodLogs", "Remove favorite: $foodId")
        }
    }
    
    // Store favorite meal IDs in memory (could be persisted to SharedPreferences or DB)
    private val favoriteMealIds = mutableSetOf<Long>()
    
    fun toggleFavorite(mealId: Long) {
        viewModelScope.launch {
            if (favoriteMealIds.contains(mealId)) {
                favoriteMealIds.remove(mealId)
            } else {
                favoriteMealIds.add(mealId)
            }
            
            _uiState.update { state ->
                val updatedState = state.copy(
                    todayMeals = state.todayMeals.map { 
                        if (it.id == mealId) it.copy(isFavorite = favoriteMealIds.contains(mealId)) else it 
                    },
                    historyMeals = state.historyMeals.map { 
                        if (it.id == mealId) it.copy(isFavorite = favoriteMealIds.contains(mealId)) else it 
                    }
                )
                
                // Update favorite foods list from both today and history
                val allFavoriteMeals = (updatedState.todayMeals + updatedState.historyMeals)
                    .filter { favoriteMealIds.contains(it.id) }
                    .distinctBy { it.foodName }
                
                val favoriteFoods = allFavoriteMeals.map { meal ->
                    FavoriteFoodItem(
                        id = meal.id,
                        name = meal.foodName,
                        calories = meal.calories,
                        protein = meal.protein,
                        carbs = meal.carbs,
                        fat = meal.fat
                    )
                }
                
                updatedState.copy(favoriteFoods = favoriteFoods)
            }
        }
    }
    
    private fun getMealType(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> "breakfast"
            in 11..14 -> "lunch"
            in 15..17 -> "snack"
            in 18..22 -> "dinner"
            else -> "snack"
        }
    }
    
    private fun formatTime(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
    }
}

data class DailyCalorieData(
    val date: Long,
    val calories: Double,
    val dayLabel: String
)

data class FoodLogsUiState(
    val historyMeals: List<MealLogItem> = emptyList(),
    val todayMeals: List<MealLogItem> = emptyList(),
    val favoriteFoods: List<FavoriteFoodItem> = emptyList(),
    val todayTotalCalories: Double = 0.0,
    val todayTotalProtein: Double = 0.0,
    val todayTotalCarbs: Double = 0.0,
    val todayTotalFat: Double = 0.0,
    val targetCalories: Double = 2000.0,
    val caloriesRemaining: Double = 2000.0,
    val weeklyCalorieData: List<DailyCalorieData> = emptyList()
)


