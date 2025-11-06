package com.kreggscode.bmr.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreggscode.bmr.data.local.dao.BMRDao
import com.kreggscode.bmr.data.local.dao.UserDao
import com.kreggscode.bmr.data.local.entities.BMRRecord
import com.kreggscode.bmr.data.local.entities.UserProfile
import com.kreggscode.bmr.data.remote.api.PollinationsApi
import com.kreggscode.bmr.data.remote.dto.ChatMessage
import com.kreggscode.bmr.data.remote.dto.NutritionAnalysisRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull

@HiltViewModel
class BMRCalculatorViewModel @Inject constructor(
    private val userDao: UserDao,
    private val bmrDao: BMRDao,
    private val aiService: com.kreggscode.bmr.data.api.PollinationsAIService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BMRCalculatorUiState())
    val uiState: StateFlow<BMRCalculatorUiState> = _uiState.asStateFlow()
    
    init {
        loadUserProfile()
    }
    
    private fun loadUserProfile() {
        viewModelScope.launch {
            userDao.getCurrentUser().collect { user ->
                user?.let { profile ->
                    _uiState.update { state ->
                        state.copy(
                            age = calculateAge(profile.dateOfBirth).toString(),
                            sex = profile.sex,
                            height = profile.heightCm.toString(),
                            weight = profile.weightKg.toString(),
                            activityLevel = profile.activityLevel,
                            goal = profile.goalType,
                            showResults = false // Don't auto-show results
                        )
                    }
                    // Don't auto-calculate - let user click Calculate button
                }
            }
        }
    }
    
    fun updateAge(age: String) {
        _uiState.update { it.copy(age = age) }
    }
    
    fun updateSex(sex: String) {
        _uiState.update { it.copy(sex = sex) }
    }
    
    fun updateHeight(height: String) {
        _uiState.update { it.copy(height = height) }
    }
    
    fun updateWeight(weight: String) {
        _uiState.update { it.copy(weight = weight) }
    }
    
    fun updateActivityLevel(level: String) {
        _uiState.update { it.copy(activityLevel = level) }
    }
    
    fun updateFormula(formula: String) {
        _uiState.update { it.copy(formula = formula) }
    }
    
    fun updateGoal(goal: String) {
        _uiState.update { it.copy(goal = goal) }
    }
    
    fun toggleUnitSystem(isMetric: Boolean) {
        val currentState = _uiState.value
        val currentHeight = currentState.height.toDoubleOrNull() ?: 0.0
        val currentWeight = currentState.weight.toDoubleOrNull() ?: 0.0
        
        if (isMetric && !currentState.isMetric) {
            // Convert from Imperial to Metric
            val heightCm = currentHeight * 2.54 // inches to cm
            val weightKg = currentWeight * 0.453592 // lbs to kg
            _uiState.update { 
                it.copy(
                    isMetric = true,
                    height = heightCm.toInt().toString(),
                    weight = String.format("%.1f", weightKg)
                )
            }
        } else if (!isMetric && currentState.isMetric) {
            // Convert from Metric to Imperial
            val heightIn = currentHeight / 2.54 // cm to inches
            val weightLbs = currentWeight / 0.453592 // kg to lbs
            _uiState.update { 
                it.copy(
                    isMetric = false,
                    height = heightIn.toInt().toString(),
                    weight = String.format("%.1f", weightLbs)
                )
            }
        } else {
            _uiState.update { it.copy(isMetric = isMetric) }
        }
    }
    
    fun calculateBMR() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isCalculating = true) }
                android.util.Log.d("BMRCalculator", "Starting calculation...")
                
                val state = _uiState.value
                val age = state.age.toIntOrNull()
                val height = state.height.toDoubleOrNull()
                val weight = state.weight.toDoubleOrNull()
                
                android.util.Log.d("BMRCalculator", "Age: $age, Height: $height, Weight: $weight")
                
                if (age == null || height == null || weight == null || age <= 0 || height <= 0 || weight <= 0) {
                    android.util.Log.e("BMRCalculator", "Invalid input values")
                    _uiState.update { it.copy(isCalculating = false) }
                    return@launch
                }
                
                var heightCm = height
                var weightKg = weight
                
                // Convert to metric if using imperial
                if (!state.isMetric) {
                    heightCm = height * 2.54 // inches to cm
                    weightKg = weight * 0.453592 // lbs to kg
                }
                
                android.util.Log.d("BMRCalculator", "Converted - Height: $heightCm cm, Weight: $weightKg kg")
                
                // Calculate BMR
                val bmr = if (state.formula == "mifflin") {
                    calculateMifflinStJeor(age, state.sex, heightCm, weightKg)
                } else {
                    calculateHarrisBenedict(age, state.sex, heightCm, weightKg)
                }
                
                android.util.Log.d("BMRCalculator", "BMR calculated: $bmr")
                
                // Calculate TDEE with activity multiplier
                val multiplier = getActivityMultiplier(state.activityLevel)
                val tdee = bmr * multiplier
                
                // Calculate target calories based on goal
                val targetCalories = when (state.goal) {
                    "lose" -> tdee - 500 // 500 calorie deficit for ~1 lb/week loss
                    "gain" -> tdee + 500 // 500 calorie surplus for ~1 lb/week gain
                    else -> tdee // maintain
                }
                
                // Calculate macros (balanced approach)
                val proteinCalories = targetCalories * 0.30 // 30% protein
                val carbCalories = targetCalories * 0.40 // 40% carbs
                val fatCalories = targetCalories * 0.30 // 30% fat
                
                val proteinGrams = proteinCalories / 4
                val carbsGrams = carbCalories / 4
                val fatGrams = fatCalories / 9
                
                android.util.Log.d("BMRCalculator", "Results - BMR: $bmr, TDEE: $tdee, Target: $targetCalories")
                
                _uiState.update { 
                    it.copy(
                        calculatedBMR = bmr,
                        calculatedTDEE = tdee,
                        targetCalories = targetCalories,
                        proteinGrams = proteinGrams,
                        carbsGrams = carbsGrams,
                        fatGrams = fatGrams,
                        showResults = true,
                        isCalculating = false
                    )
                }
                
                android.util.Log.d("BMRCalculator", "Calculation complete, showing results")
            } catch (e: Exception) {
                android.util.Log.e("BMRCalculator", "Calculation error: ${e.message}", e)
                _uiState.update { it.copy(isCalculating = false) }
            }
        }
    }
    
    fun saveBMRRecord() {
        viewModelScope.launch {
            val state = _uiState.value
            val user = userDao.getCurrentUser().firstOrNull()
            
            if (user != null) {
                val bmrRecord = BMRRecord(
                    userId = user.id,
                    formula = state.formula,
                    bmrValue = state.calculatedBMR,
                    tdeeValue = state.calculatedTDEE,
                    activityMultiplier = getActivityMultiplier(state.activityLevel),
                    targetCalories = state.targetCalories,
                    proteinGrams = state.proteinGrams,
                    carbsGrams = state.carbsGrams,
                    fatGrams = state.fatGrams
                )
                
                // Check if this is the first BMR calculation (no existing records)
                val existingRecords = bmrDao.getUserBMRRecords(user.id).firstOrNull() ?: emptyList()
                val isFirstBMR = existingRecords.isEmpty()
                
                bmrDao.insertBMRRecord(bmrRecord)
                
                // Update user profile
                val newWeight = state.weight.toDoubleOrNull() ?: user.weightKg
                val updatedUser = user.copy(
                    heightCm = state.height.toDoubleOrNull() ?: user.heightCm,
                    weightKg = newWeight,
                    activityLevel = state.activityLevel,
                    goalType = state.goal,
                    updatedAt = System.currentTimeMillis()
                )
                
                userDao.updateUser(updatedUser)
                
                // If this is the first BMR calculation, store this weight as the starting weight
                // This will be used for weight loss tracking
                if (isFirstBMR) {
                    android.util.Log.d("BMRCalculator", "First BMR calculation - starting weight: $newWeight kg")
                }
                
                _uiState.update { 
                    it.copy(
                        saveSuccess = true,
                        showResults = false // Reset to show input form after saving
                    )
                }
            } else {
                // Create new user if doesn't exist
                val newUser = UserProfile(
                    name = "User",
                    dateOfBirth = System.currentTimeMillis() - (state.age.toLongOrNull() ?: 25) * 365 * 24 * 60 * 60 * 1000,
                    sex = state.sex,
                    heightCm = state.height.toDoubleOrNull() ?: 170.0,
                    weightKg = state.weight.toDoubleOrNull() ?: 70.0,
                    activityLevel = state.activityLevel,
                    goalType = state.goal
                )
                
                val userId = userDao.insertUser(newUser)
                
                val bmrRecord = BMRRecord(
                    userId = userId,
                    formula = state.formula,
                    bmrValue = state.calculatedBMR,
                    tdeeValue = state.calculatedTDEE,
                    activityMultiplier = getActivityMultiplier(state.activityLevel),
                    targetCalories = state.targetCalories,
                    proteinGrams = state.proteinGrams,
                    carbsGrams = state.carbsGrams,
                    fatGrams = state.fatGrams
                )
                
                bmrDao.insertBMRRecord(bmrRecord)
                _uiState.update { it.copy(saveSuccess = true) }
            }
        }
    }
    
    fun resetCalculation() {
        _uiState.update { it.copy(showResults = false, aiAnalysis = null) }
    }
    
    fun requestAIAnalysis() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAI = true) }
            
            try {
                val state = _uiState.value
                val goalText = when(state.goal) {
                    "lose" -> "weight loss"
                    "gain" -> "muscle gain"
                    else -> "weight maintenance"
                }
                
                val prompt = """COMPREHENSIVE METABOLIC ANALYSIS REPORT
                    
                    PERSONAL METABOLIC PROFILE
                    
                    Basal Metabolic Rate (BMR): ${state.calculatedBMR.toInt()} calories per day
                    Total Daily Energy Expenditure (TDEE): ${state.calculatedTDEE.toInt()} calories per day
                    Fitness Goal: ${goalText.capitalize()}
                    Daily Calorie Target: ${state.targetCalories.toInt()} calories
                    Recommended Macronutrients: ${state.proteinGrams.toInt()}g protein, ${state.carbsGrams.toInt()}g carbohydrates, ${state.fatGrams.toInt()}g fat
                    
                    ANALYSIS SECTIONS
                    
                    1. UNDERSTANDING YOUR METABOLISM
                    Explain what your BMR and TDEE values mean in practical terms. How do these numbers relate to your daily energy requirements and metabolic efficiency?
                    
                    2. CALORIE MANAGEMENT STRATEGY
                    Provide a detailed breakdown of your ${state.targetCalories.toInt()} calorie daily target:
                    - How this supports your ${goalText} objective
                    - Optimal distribution of calories throughout the day
                    - Comparison to general population averages
                    
                    3. MACRONUTRIENT OPTIMIZATION
                    Explain the importance of your specific protein (${state.proteinGrams.toInt()}g), carbohydrate (${state.carbsGrams.toInt()}g), and fat (${state.fatGrams.toInt()}g) targets:
                    - Protein's role in muscle maintenance and growth
                    - Carbohydrates for energy production and performance
                    - Fats for hormonal balance and nutrient absorption
                    - Strategic timing of macronutrient consumption
                    
                    4. MEAL TIMING AND FREQUENCY PLAN
                    Recommend optimal meal timing and frequency for your ${state.targetCalories.toInt()} calorie target:
                    - Ideal number of meals per day
                    - Pre and post-workout nutrition timing
                    - Evening and nighttime eating guidelines
                    
                    5. METABOLIC ENHANCEMENT TECHNIQUES
                    Provide 5-7 evidence-based strategies to optimize metabolic function:
                    - Exercise recommendations for metabolic boost
                    - Lifestyle factors influencing metabolism
                    - Nutritional approaches for metabolic health
                    
                    6. COMMON METABOLIC MISTAKES TO AVOID
                    Identify critical errors that could undermine your metabolic results:
                    - Inappropriate calorie restriction patterns
                    - Macronutrient distribution problems
                    - Timing and frequency errors
                    
                    7. PROGRESS TIMELINE AND REALISTIC EXPECTATIONS
                    Outline achievable progress markers for your ${goalText} goal:
                    - Weekly progress indicators
                    - Monthly transformation milestones
                    - Comprehensive success measurement beyond weight
                    
                    8. RECOVERY AND HYDRATION PROTOCOL
                    Detail essential recovery strategies for optimal metabolic performance:
                    - Daily hydration requirements with specific calculations
                    - Sleep optimization guidelines
                    - Stress management and recovery protocols
                    
                    FORMATTING REQUIREMENTS
                    - Use clear, professional section headings
                    - Include specific numerical data and calculations
                    - Provide practical, actionable recommendations
                    - Maintain an encouraging and supportive tone
                    - Use numbered and bulleted lists for clarity
                    - Reference scientific principles where appropriate
                    
                    MEDICAL DISCLAIMER
                    This analysis is for informational purposes only and does not constitute medical advice. Always consult with qualified healthcare professionals before making significant changes to your diet, exercise regimen, or lifestyle. Individual results may vary based on personal health conditions, genetics, and adherence to recommendations.
                    
                    Create a comprehensive, professional metabolic assessment that empowers informed decision-making.""".trimIndent()
                
                val systemPrompt = """You are an expert nutritionist and metabolic specialist.
                    Provide DETAILED, COMPREHENSIVE analysis - not brief summaries.
                    Use clear formatting with sections, bullet points, and specific numbers.
                    Be encouraging but realistic. Give actionable advice.""".trimIndent()
                
                val result = aiService.generateText(
                    prompt = prompt,
                    systemPrompt = systemPrompt,
                    temperature = 1.0f
                )
                
                result.onSuccess { analysis ->
                    _uiState.update { 
                        it.copy(
                            aiAnalysis = analysis,
                            isLoadingAI = false
                        )
                    }
                }.onFailure { e ->
                    // Fallback to detailed offline analysis
                    _uiState.update { 
                        it.copy(
                            aiAnalysis = generateDetailedOfflineAnalysis(state),
                            isLoadingAI = false
                        )
                    }
                }
            } catch (e: Exception) {
                val state = _uiState.value
                _uiState.update { 
                    it.copy(
                        aiAnalysis = generateDetailedOfflineAnalysis(state),
                        isLoadingAI = false
                    )
                }
            }
        }
    }
    
    private fun generateDetailedOfflineAnalysis(state: BMRCalculatorUiState): String {
        val goalText = when(state.goal) {
            "lose" -> "weight loss"
            "gain" -> "muscle gain"
            else -> "maintenance"
        }
        
        return """
            COMPREHENSIVE METABOLIC ANALYSIS REPORT
            
            PERSONAL METABOLIC PROFILE
            
            Your Basal Metabolic Rate (BMR) of ${state.calculatedBMR.toInt()} calories per day represents the energy your body requires to maintain essential physiological functions while at complete rest. This includes organ function, temperature regulation, and cellular maintenance.
            
            Your Total Daily Energy Expenditure (TDEE) of ${state.calculatedTDEE.toInt()} calories per day represents your complete daily energy needs, factoring in your activity level and all daily movement.
            
            CALORIE MANAGEMENT STRATEGY FOR ${goalText.uppercase()}
            
            Daily Target: ${state.targetCalories.toInt()} calories
            ${when(state.goal) {
                "lose" -> """• Creates a sustainable 500-calorie deficit for approximately 1 pound of fat loss per week
• Preserves lean muscle mass and metabolic function
• Maintains above ${(state.calculatedBMR * 0.8).toInt()} calories to protect hormonal balance"""
                "gain" -> """• Provides a 500-calorie surplus for approximately 1 pound of muscle gain per week
• Supports strength training and recovery processes
• Focuses on nutrient-dense, quality calorie sources"""
                else -> """• Maintains current body weight and composition
• Supports body recomposition goals
• Allows for metabolic flexibility and adjustment"""
            }}
            
            MACRONUTRIENT OPTIMIZATION
            
            Protein: ${state.proteinGrams.toInt()}g (30% of total calories)
            - Essential for muscle tissue maintenance and growth
            - Provides highest thermic effect of food (25-30% of calories burned during digestion)
            - Promotes satiety and helps maintain lean body mass
            
            Carbohydrates: ${state.carbsGrams.toInt()}g (40% of total calories)
            - Primary fuel source for physical activity and brain function
            - Supports optimal workout performance and recovery
            - Best consumed around training sessions for maximum utilization
            
            Fat: ${state.fatGrams.toInt()}g (30% of total calories)
            - Critical for hormone production and regulation
            - Supports vitamin absorption and neurological function
            - Provides sustained energy and metabolic stability
            
            MEAL TIMING AND FREQUENCY PROTOCOL
            
            1. Breakfast: ${(state.targetCalories * 0.25).toInt()} calories - Initiates metabolic activity
            2. Lunch: ${(state.targetCalories * 0.35).toInt()} calories - Primary energy meal
            3. Afternoon Snack: ${(state.targetCalories * 0.10).toInt()} calories - Maintains energy levels
            4. Dinner: ${(state.targetCalories * 0.30).toInt()} calories - Recovery and repair
            
            METABOLIC ENHANCEMENT STRATEGIES
            
            • Strength training 3-4 times per week builds metabolically active muscle tissue
            • Protein consumption with each meal maximizes thermic effect
            • Adequate hydration: ${(state.weight.toDoubleOrNull() ?: 70.0) * 0.033} liters of water daily
            • Quality sleep (7-9 hours) optimizes hormonal balance and metabolic function
            • Regular meal timing prevents metabolic slowdown
            • Increased non-exercise activity (8,000+ daily steps) boosts calorie expenditure
            
            CRITICAL MISTAKES TO AVOID
            
            • Excessive calorie restriction below ${(state.calculatedBMR * 0.8).toInt()} calories
            • Insufficient protein intake compromising muscle maintenance
            • Irregular meal timing causing metabolic confusion
            • Failure to track progress and adjust accordingly
            • Unrealistic expectations leading to discouragement
            
            PROGRESS TIMELINE AND EXPECTATIONS
            
            Weeks 1-2: Initial physiological adaptation and water weight adjustments
            Weeks 3-4: Visible ${if(state.goal == "lose") "fat loss" else if(state.goal == "gain") "muscle gain" else "maintenance"} progress
            Months 2-3: Significant body composition transformation
            Months 4+: Establishment of sustainable lifestyle patterns
            
            RECOVERY AND HYDRATION PROTOCOL
            
            Water Intake: ${(state.weight.toDoubleOrNull() ?: 70.0) * 0.033} liters daily minimum
            Sleep: 7-9 hours of quality sleep for optimal hormonal function
            Stress Management: High cortisol levels impair metabolic efficiency
            Progress Monitoring: Regular measurements beyond scale weight
            
            MEDICAL DISCLAIMER
            
            This metabolic analysis is provided for informational purposes only and does not constitute medical advice, diagnosis, or treatment. The information presented is based on general nutritional and metabolic principles and may not be appropriate for all individuals. Always consult with qualified healthcare professionals, including physicians and registered dietitians, before implementing significant changes to your diet, exercise regimen, or lifestyle. Individual results may vary based on personal health conditions, medical history, genetics, and adherence to recommendations. If you have any medical conditions, are taking medications, or are pregnant/breastfeeding, professional medical supervision is essential.
            
            Sustainable health transformation requires consistency, patience, and professional guidance when needed.
        """.trimIndent()
    }
    
    private fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)
    
    private fun calculateMifflinStJeor(age: Int, sex: String, heightCm: Double, weightKg: Double): Double {
        val s = if (sex.lowercase() == "male") 5 else -161
        return (10 * weightKg) + (6.25 * heightCm) - (5 * age) + s
    }
    
    private fun calculateHarrisBenedict(age: Int, sex: String, heightCm: Double, weightKg: Double): Double {
        return if (sex.lowercase() == "male") {
            (13.397 * weightKg) + (4.799 * heightCm) - (5.677 * age) + 88.362
        } else {
            (9.247 * weightKg) + (3.098 * heightCm) - (4.330 * age) + 447.593
        }
    }
    
    private fun getActivityMultiplier(level: String): Double {
        return when (level) {
            "sedentary" -> 1.2
            "light" -> 1.375
            "moderate" -> 1.55
            "active" -> 1.725
            "very_active" -> 1.9
            else -> 1.2
        }
    }
    
    private fun calculateAge(dateOfBirth: Long): Int {
        val now = System.currentTimeMillis()
        val ageInMillis = now - dateOfBirth
        return (ageInMillis / (365.25 * 24 * 60 * 60 * 1000)).toInt()
    }
}

data class BMRCalculatorUiState(
    val age: String = "",
    val sex: String = "male",
    val height: String = "",
    val weight: String = "",
    val activityLevel: String = "moderate",
    val formula: String = "mifflin",
    val goal: String = "maintain",
    val isMetric: Boolean = true,
    val calculatedBMR: Double = 0.0,
    val calculatedTDEE: Double = 0.0,
    val targetCalories: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val showResults: Boolean = false,
    val isCalculating: Boolean = false,
    val aiAnalysis: String? = null,
    val isLoadingAI: Boolean = false,
    val saveSuccess: Boolean = false
)
