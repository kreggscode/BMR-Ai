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
    private val pollinationsApi: PollinationsApi
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
                            goal = profile.goalType
                        )
                    }
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
                
                bmrDao.insertBMRRecord(bmrRecord)
                
                // Update user profile
                val updatedUser = user.copy(
                    heightCm = state.height.toDoubleOrNull() ?: user.heightCm,
                    weightKg = state.weight.toDoubleOrNull() ?: user.weightKg,
                    activityLevel = state.activityLevel,
                    goalType = state.goal,
                    updatedAt = System.currentTimeMillis()
                )
                
                userDao.updateUser(updatedUser)
                
                _uiState.update { it.copy(saveSuccess = true) }
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
                val systemMessage = """You are a professional nutritionist providing personalized BMR analysis.
                    Be concise and actionable. Maximum 3 sentences.""".trimIndent()
                
                val userMessage = """Analyze my BMR results:
                    BMR: ${state.calculatedBMR.toInt()} kcal/day
                    TDEE: ${state.calculatedTDEE.toInt()} kcal/day
                    Goal: ${state.goal}
                    Target Calories: ${state.targetCalories.toInt()} kcal/day
                    Macros: Protein ${state.proteinGrams.toInt()}g, Carbs ${state.carbsGrams.toInt()}g, Fat ${state.fatGrams.toInt()}g
                    
                    Provide 2 practical tips for reaching my goal.""".trimIndent()
                
                val request = NutritionAnalysisRequest(
                    messages = listOf(
                        ChatMessage("system", systemMessage),
                        ChatMessage("user", userMessage)
                    ),
                    temperature = 1.0
                )
                
                val response = pollinationsApi.analyzeNutrition(request)
                
                if (response.isSuccessful) {
                    val analysis = response.body()?.content ?: "Unable to generate analysis"
                    _uiState.update { 
                        it.copy(
                            aiAnalysis = analysis,
                            isLoadingAI = false
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            aiAnalysis = "Your BMR of ${state.calculatedBMR.toInt()} kcal/day is your baseline. With your ${state.goal} goal, aim for ${state.targetCalories.toInt()} calories daily. Focus on hitting your protein target of ${state.proteinGrams.toInt()}g to preserve muscle mass.",
                            isLoadingAI = false
                        )
                    }
                }
            } catch (e: Exception) {
                // Fallback to offline analysis
                val state = _uiState.value
                _uiState.update { 
                    it.copy(
                        aiAnalysis = "Your BMR of ${state.calculatedBMR.toInt()} kcal/day is your baseline. With your ${state.goal} goal, aim for ${state.targetCalories.toInt()} calories daily. Focus on hitting your protein target of ${state.proteinGrams.toInt()}g to preserve muscle mass.",
                        isLoadingAI = false
                    )
                }
            }
        }
    }
    
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
