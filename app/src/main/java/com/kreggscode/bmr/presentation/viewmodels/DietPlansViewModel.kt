package com.kreggscode.bmr.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreggscode.bmr.data.api.PollinationsAIService
import com.kreggscode.bmr.data.local.dao.BMRDao
import com.kreggscode.bmr.data.local.dao.UserDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DietPlansViewModel @Inject constructor(
    private val userDao: UserDao,
    private val bmrDao: BMRDao,
    private val aiService: PollinationsAIService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DietPlansUiState())
    val uiState: StateFlow<DietPlansUiState> = _uiState.asStateFlow()
    
    init {
        loadUserData()
        // Initialize shopping list with default plan
        _uiState.update { it.copy(shoppingList = getShoppingListForPlan(DietPlanType.WEIGHT_LOSS)) }
    }
    
    private fun loadUserData() {
        viewModelScope.launch {
            userDao.getCurrentUser().collect { user ->
                user?.let { profile ->
                    bmrDao.getLatestBMRRecord(profile.id).collect { bmrRecord ->
                        _uiState.update { state ->
                            state.copy(
                                bmr = bmrRecord?.bmrValue ?: 0.0,
                                targetCalories = bmrRecord?.targetCalories ?: 0.0,
                                proteinGrams = bmrRecord?.proteinGrams ?: 0.0,
                                carbsGrams = bmrRecord?.carbsGrams ?: 0.0,
                                fatGrams = bmrRecord?.fatGrams ?: 0.0,
                                goal = profile.goalType
                            )
                        }
                    }
                }
            }
        }
    }
    
    fun selectPlanType(planType: DietPlanType) {
        _uiState.update { 
            it.copy(
                selectedPlanType = planType,
                shoppingList = getShoppingListForPlan(planType)
            ) 
        }
    }
    
    private fun getShoppingListForPlan(planType: DietPlanType): Map<String, List<String>> {
        return when (planType) {
            DietPlanType.WEIGHT_LOSS -> mapOf(
                "Proteins" to listOf("Chicken breast", "Turkey", "Fish (salmon, tuna)", "Egg whites", "Greek yogurt"),
                "Vegetables" to listOf("Broccoli", "Spinach", "Kale", "Bell peppers", "Cauliflower", "Zucchini"),
                "Fruits" to listOf("Berries", "Apples", "Grapefruit", "Oranges"),
                "Grains" to listOf("Oats", "Brown rice", "Quinoa"),
                "Healthy Fats" to listOf("Almonds", "Olive oil", "Avocado")
            )
            DietPlanType.MUSCLE_GAIN -> mapOf(
                "Proteins" to listOf("Chicken breast", "Lean beef", "Salmon", "Eggs", "Protein powder", "Greek yogurt"),
                "Carbs" to listOf("Sweet potatoes", "Brown rice", "Oats", "Whole grain bread", "Quinoa"),
                "Vegetables" to listOf("Broccoli", "Spinach", "Mixed vegetables"),
                "Fruits" to listOf("Bananas", "Berries", "Apples"),
                "Healthy Fats" to listOf("Peanut butter", "Almonds", "Avocado", "Olive oil")
            )
            DietPlanType.MAINTENANCE -> mapOf(
                "Proteins" to listOf("Chicken", "Fish", "Eggs", "Greek yogurt", "Lean beef"),
                "Vegetables" to listOf("Mixed salad greens", "Tomatoes", "Cucumbers", "Carrots", "Broccoli"),
                "Fruits" to listOf("Apples", "Bananas", "Berries", "Oranges"),
                "Grains" to listOf("Whole grain bread", "Brown rice", "Oats", "Quinoa"),
                "Dairy" to listOf("Milk", "Cheese", "Yogurt")
            )
            DietPlanType.KETO -> mapOf(
                "Proteins" to listOf("Ribeye steak", "Salmon", "Bacon", "Eggs", "Cheese"),
                "Low-Carb Vegetables" to listOf("Spinach", "Kale", "Asparagus", "Cauliflower", "Broccoli"),
                "Healthy Fats" to listOf("Butter", "Coconut oil", "Olive oil", "Avocado", "Macadamia nuts"),
                "Dairy" to listOf("Heavy cream", "Cream cheese", "Full-fat cheese"),
                "Keto Snacks" to listOf("Pork rinds", "Cheese crisps", "Keto fat bombs")
            )
            DietPlanType.VEGETARIAN -> mapOf(
                "Proteins" to listOf("Tofu", "Tempeh", "Lentils", "Chickpeas", "Greek yogurt", "Eggs", "Cheese"),
                "Vegetables" to listOf("Spinach", "Broccoli", "Carrots", "Bell peppers", "Tomatoes"),
                "Fruits" to listOf("Apples", "Bananas", "Berries", "Oranges"),
                "Grains" to listOf("Brown rice", "Quinoa", "Whole grain bread", "Oats"),
                "Legumes" to listOf("Black beans", "Kidney beans", "Lentils", "Chickpeas")
            )
            DietPlanType.VEGAN -> mapOf(
                "Proteins" to listOf("Tofu", "Tempeh", "Lentils", "Chickpeas", "Black beans", "Edamame"),
                "Vegetables" to listOf("Kale", "Spinach", "Broccoli", "Sweet potatoes", "Bell peppers"),
                "Fruits" to listOf("Berries", "Bananas", "Apples", "Oranges"),
                "Grains" to listOf("Quinoa", "Brown rice", "Oats", "Whole grain bread"),
                "Plant Milks" to listOf("Almond milk", "Soy milk", "Oat milk"),
                "Nuts & Seeds" to listOf("Almonds", "Chia seeds", "Flax seeds", "Walnuts")
            )
        }
    }
    
    fun generateDietPlan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            
            try {
                val state = _uiState.value
                val dietTypeDescription = when (state.selectedPlanType) {
                    DietPlanType.WEIGHT_LOSS -> "weight loss with calorie deficit"
                    DietPlanType.MUSCLE_GAIN -> "muscle gain with calorie surplus and high protein"
                    DietPlanType.MAINTENANCE -> "weight maintenance with balanced nutrition"
                    DietPlanType.KETO -> "ketogenic diet (low carb, high fat, under 50g carbs daily)"
                    DietPlanType.VEGETARIAN -> "vegetarian diet (no meat, includes dairy and eggs)"
                    DietPlanType.VEGAN -> "vegan diet (plant-based, no animal products)"
                }
                
                val result = aiService.generateDietPlan(
                    bmr = state.bmr,
                    goal = state.goal,
                    dietType = dietTypeDescription
                )
                
                result.onSuccess { plan ->
                    _uiState.update { 
                        it.copy(
                            generatedPlan = plan,
                            isGenerating = false,
                            showPlanDialog = false
                        )
                    }
                }.onFailure {
                    _uiState.update { 
                        it.copy(
                            generatedPlan = getDefaultPlan(state.selectedPlanType),
                            isGenerating = false,
                            showPlanDialog = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        generatedPlan = getDefaultPlan(_uiState.value.selectedPlanType),
                        isGenerating = false,
                        showPlanDialog = false
                    )
                }
            }
        }
    }
    
    fun togglePlanDialog(show: Boolean) {
        _uiState.update { it.copy(showPlanDialog = show) }
    }
    
    fun closePlan() {
        _uiState.update { it.copy(generatedPlan = "") }
    }
    
    private fun getDefaultPlan(planType: DietPlanType): String {
        return when (planType) {
            DietPlanType.WEIGHT_LOSS -> """
                **Weight Loss Plan**
                Daily Target: ${_uiState.value.targetCalories.toInt()} kcal
                
                **Breakfast (350 kcal)**
                - Oatmeal with berries and almonds
                - Greek yogurt
                - Green tea
                
                **Snack (150 kcal)**
                - Apple with almond butter
                
                **Lunch (450 kcal)**
                - Grilled chicken salad
                - Quinoa
                - Olive oil dressing
                
                **Snack (150 kcal)**
                - Protein shake
                
                **Dinner (400 kcal)**
                - Baked salmon
                - Steamed vegetables
                - Brown rice
                
                **Tips:**
                - Drink 8+ glasses of water
                - Avoid processed foods
                - Track your calories daily
            """.trimIndent()
            
            DietPlanType.MUSCLE_GAIN -> """
                **Muscle Gain Plan**
                Daily Target: ${_uiState.value.targetCalories.toInt()} kcal
                
                **Breakfast (500 kcal)**
                - Scrambled eggs (4 eggs)
                - Whole grain toast
                - Avocado
                - Protein shake
                
                **Snack (250 kcal)**
                - Greek yogurt with granola
                
                **Lunch (600 kcal)**
                - Grilled chicken breast
                - Sweet potato
                - Mixed vegetables
                
                **Pre-Workout (200 kcal)**
                - Banana with peanut butter
                
                **Dinner (550 kcal)**
                - Lean beef steak
                - Quinoa
                - Broccoli
                
                **Post-Workout (300 kcal)**
                - Protein shake
                - Rice cakes
                
                **Tips:**
                - Eat every 3-4 hours
                - Prioritize protein (${_uiState.value.proteinGrams.toInt()}g daily)
                - Stay hydrated
            """.trimIndent()
            
            DietPlanType.MAINTENANCE -> """
                **Maintenance Plan**
                Daily Target: ${_uiState.value.targetCalories.toInt()} kcal
                
                **Breakfast (400 kcal)**
                - Whole grain cereal
                - Milk
                - Banana
                
                **Snack (150 kcal)**
                - Mixed nuts
                
                **Lunch (500 kcal)**
                - Turkey sandwich
                - Side salad
                - Fruit
                
                **Snack (200 kcal)**
                - Hummus with vegetables
                
                **Dinner (450 kcal)**
                - Grilled fish
                - Roasted vegetables
                - Quinoa
                
                **Tips:**
                - Balanced macros
                - Regular meal times
                - Moderate portions
            """.trimIndent()
            
            DietPlanType.KETO -> """
                **Keto Diet Plan**
                Daily Target: ${_uiState.value.targetCalories.toInt()} kcal
                Carbs: <50g | Fat: 70% | Protein: 25%
                
                **Breakfast (400 kcal)**
                - Scrambled eggs with cheese
                - Bacon
                - Avocado
                
                **Snack (150 kcal)**
                - Cheese cubes
                - Macadamia nuts
                
                **Lunch (500 kcal)**
                - Grilled salmon
                - Cauliflower rice
                - Butter sauce
                
                **Snack (200 kcal)**
                - Keto fat bombs
                
                **Dinner (450 kcal)**
                - Ribeye steak
                - Asparagus with butter
                - Caesar salad (no croutons)
                
                **Tips:**
                - Stay under 50g carbs
                - High fat, moderate protein
                - Track ketones
            """.trimIndent()
            
            DietPlanType.VEGETARIAN -> """
                **Vegetarian Plan**
                Daily Target: ${_uiState.value.targetCalories.toInt()} kcal
                
                **Breakfast (400 kcal)**
                - Tofu scramble
                - Whole grain toast
                - Fresh fruit
                
                **Snack (150 kcal)**
                - Hummus with carrots
                
                **Lunch (500 kcal)**
                - Lentil curry
                - Brown rice
                - Naan bread
                
                **Snack (200 kcal)**
                - Greek yogurt with honey
                
                **Dinner (450 kcal)**
                - Chickpea Buddha bowl
                - Quinoa
                - Tahini dressing
                
                **Tips:**
                - Combine protein sources
                - Include B12 supplement
                - Eat variety of vegetables
            """.trimIndent()
            
            DietPlanType.VEGAN -> """
                **Vegan Diet Plan**
                Daily Target: ${_uiState.value.targetCalories.toInt()} kcal
                
                **Breakfast (400 kcal)**
                - Oatmeal with plant milk
                - Chia seeds
                - Berries and nuts
                
                **Snack (150 kcal)**
                - Apple with almond butter
                
                **Lunch (500 kcal)**
                - Tempeh stir-fry
                - Brown rice
                - Mixed vegetables
                
                **Snack (200 kcal)**
                - Trail mix
                
                **Dinner (450 kcal)**
                - Black bean burger
                - Sweet potato fries
                - Side salad
                
                **Tips:**
                - Supplement B12 and D3
                - Combine legumes and grains
                - Eat colorful vegetables
            """.trimIndent()
        }
    }
}

data class DietPlansUiState(
    val selectedPlanType: DietPlanType = DietPlanType.WEIGHT_LOSS,
    val bmr: Double = 0.0,
    val targetCalories: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val goal: String = "maintain",
    val generatedPlan: String = "",
    val isGenerating: Boolean = false,
    val showPlanDialog: Boolean = false,
    val shoppingList: Map<String, List<String>> = emptyMap()
)

enum class DietPlanType(val displayName: String) {
    WEIGHT_LOSS("Weight Loss"),
    MUSCLE_GAIN("Muscle Gain"),
    MAINTENANCE("Maintenance"),
    KETO("Keto"),
    VEGETARIAN("Vegetarian"),
    VEGAN("Vegan")
}
