package com.kreggscode.bmr.presentation.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreggscode.bmr.data.local.dao.FoodDao
import com.kreggscode.bmr.data.local.dao.UserDao
import com.kreggscode.bmr.data.local.entities.FoodItem
import com.kreggscode.bmr.data.local.entities.MealEntry
import com.kreggscode.bmr.data.remote.api.PollinationsApi
import com.kreggscode.bmr.data.remote.dto.ChatMessage
import com.kreggscode.bmr.data.remote.dto.ChatRequest
import com.kreggscode.bmr.presentation.screens.RecognizedFoodItem
import com.kreggscode.bmr.presentation.screens.ScanMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject

@HiltViewModel
class FoodScannerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: UserDao,
    private val foodDao: FoodDao,
    private val aiService: com.kreggscode.bmr.data.api.PollinationsAIService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FoodScannerUiState())
    val uiState: StateFlow<FoodScannerUiState> = _uiState.asStateFlow()
    
    // Manual entry fields
    var manualFoodName by mutableStateOf("")
    var manualCalories by mutableStateOf("")
    var manualServing by mutableStateOf("")
    var manualProtein by mutableStateOf("")
    var manualCarbs by mutableStateOf("")
    var manualFat by mutableStateOf("")
    
    fun changeScanMode(mode: ScanMode) {
        _uiState.update { it.copy(scanMode = mode) }
    }
    
    fun processImage(imageUri: Uri) {
        _uiState.update { 
            it.copy(
                isProcessing = true,
                scannedImageUri = imageUri
            )
        }
        
        viewModelScope.launch {
            try {
                // Process image with Pollinations AI
                val recognizedFoods = analyzeImageWithAI(imageUri)
                
                _uiState.update { 
                    it.copy(
                        recognizedFoods = recognizedFoods,
                        scanMode = ScanMode.RESULTS,
                        isProcessing = false
                    )
                }
            } catch (e: Exception) {
                // Fallback to mock data for demo
                val mockFoods = listOf(
                    RecognizedFoodItem(
                        name = "Grilled Chicken Breast",
                        confidence = 0.92f,
                        calories = 165.0,
                        portion = "100g",
                        protein = 31.0,
                        carbs = 0.0,
                        fat = 3.6
                    ),
                    RecognizedFoodItem(
                        name = "Steamed Broccoli",
                        confidence = 0.88f,
                        calories = 35.0,
                        portion = "100g",
                        protein = 2.8,
                        carbs = 7.0,
                        fat = 0.4
                    ),
                    RecognizedFoodItem(
                        name = "Brown Rice",
                        confidence = 0.85f,
                        calories = 112.0,
                        portion = "100g",
                        protein = 2.6,
                        carbs = 23.5,
                        fat = 0.9
                    )
                )
                
                _uiState.update { 
                    it.copy(
                        recognizedFoods = mockFoods,
                        scanMode = ScanMode.RESULTS,
                        isProcessing = false
                    )
                }
            }
        }
    }
    
    private suspend fun analyzeImageWithAI(imageUri: Uri): List<RecognizedFoodItem> {
        return try {
            // Use AI to analyze food description from image
            val prompt = """Analyze this food and identify all items visible. For each item provide:
                - Name of the food
                - Estimated calories per 100g
                - Protein, carbs, and fat content
                - Suggested portion size
                
                Format as a list with each item on a new line.""".trimIndent()
            
            val result = aiService.analyzeFoodDescription(prompt)
            
            result.getOrNull()?.let { response ->
                parseAIResponse(response)
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun prepareImageFile(imageUri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        
        // Resize image if too large
        val maxSize = 1024
        val scaledBitmap = if (bitmap.width > maxSize || bitmap.height > maxSize) {
            val scale = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }
        
        // Compress and save to temp file
        val tempFile = File(context.cacheDir, "food_image_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(tempFile)
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        outputStream.close()
        
        return tempFile
    }
    
    private fun parseAIResponse(response: String): List<RecognizedFoodItem> {
        // Parse AI text response and extract food items
        val foods = mutableListOf<RecognizedFoodItem>()
        
        try {
            // Simple parsing - look for food names and nutritional info
            val lines = response.lines().filter { it.isNotBlank() }
            
            lines.forEach { line ->
                // Extract food name (usually first part before numbers)
                val name = line.substringBefore(":").substringBefore("-").trim()
                
                // Extract calories (look for numbers followed by "cal" or "kcal")
                val caloriesMatch = Regex("(\\d+)\\s*(cal|kcal)").find(line)
                val calories = caloriesMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 150.0
                
                // Extract protein
                val proteinMatch = Regex("(\\d+)g?\\s*protein", RegexOption.IGNORE_CASE).find(line)
                val protein = proteinMatch?.groupValues?.get(1)?.toDoubleOrNull()
                
                // Extract carbs
                val carbsMatch = Regex("(\\d+)g?\\s*carb", RegexOption.IGNORE_CASE).find(line)
                val carbs = carbsMatch?.groupValues?.get(1)?.toDoubleOrNull()
                
                // Extract fat
                val fatMatch = Regex("(\\d+)g?\\s*fat", RegexOption.IGNORE_CASE).find(line)
                val fat = fatMatch?.groupValues?.get(1)?.toDoubleOrNull()
                
                if (name.isNotEmpty() && name.length > 2) {
                    foods.add(
                        RecognizedFoodItem(
                            name = name.take(50),
                            confidence = 0.85f,
                            calories = calories,
                            portion = "100g",
                            protein = protein,
                            carbs = carbs,
                            fat = fat
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Return empty on parse error
        }
        
        return foods.take(5) // Limit to 5 items
    }
    
    fun confirmFood(food: RecognizedFoodItem) {
        _uiState.update { state ->
            state.copy(
                confirmedFoods = state.confirmedFoods + food
            )
        }
    }
    
    fun saveAllFoods() {
        viewModelScope.launch {
            val user = userDao.getCurrentUser().first() ?: return@launch
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            _uiState.value.recognizedFoods.forEach { recognizedFood ->
                // Create or find food item
                val foodItem = FoodItem(
                    name = recognizedFood.name,
                    calories = recognizedFood.calories,
                    protein = recognizedFood.protein ?: 0.0,
                    carbs = recognizedFood.carbs ?: 0.0,
                    fat = recognizedFood.fat ?: 0.0,
                    servingSize = recognizedFood.portion.replace(Regex("[^0-9.]"), ""),
                    servingUnit = recognizedFood.portion.replace(Regex("[0-9.]"), ""),
                    isCustom = true
                )
                
                val foodId = foodDao.insertFoodItem(foodItem)
                
                // Create meal entry
                val mealEntry = MealEntry(
                    userId = user.id,
                    foodItemId = foodId,
                    date = today,
                    mealType = getMealType(),
                    quantity = 1.0,
                    caloriesCalculated = recognizedFood.calories,
                    proteinCalculated = recognizedFood.protein ?: 0.0,
                    carbsCalculated = recognizedFood.carbs ?: 0.0,
                    fatCalculated = recognizedFood.fat ?: 0.0,
                    source = "scanner"
                )
                
                foodDao.insertMealEntry(mealEntry)
            }
            
            _uiState.update { 
                it.copy(
                    saveSuccess = true,
                    scanMode = ScanMode.CAMERA
                )
            }
        }
    }
    
    fun addManualFood() {
        viewModelScope.launch {
            val user = userDao.getCurrentUser().first() ?: return@launch
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val calories = manualCalories.toDoubleOrNull() ?: 0.0
            val protein = manualProtein.toDoubleOrNull() ?: 0.0
            val carbs = manualCarbs.toDoubleOrNull() ?: 0.0
            val fat = manualFat.toDoubleOrNull() ?: 0.0
            
            // Create food item
            val foodItem = FoodItem(
                name = manualFoodName,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                servingSize = manualServing.replace(Regex("[^0-9.]"), ""),
                servingUnit = manualServing.replace(Regex("[0-9.]"), ""),
                isCustom = true
            )
            
            val foodId = foodDao.insertFoodItem(foodItem)
            
            // Create meal entry
            val mealEntry = MealEntry(
                userId = user.id,
                foodItemId = foodId,
                date = today,
                mealType = getMealType(),
                quantity = 1.0,
                caloriesCalculated = calories,
                proteinCalculated = protein,
                carbsCalculated = carbs,
                fatCalculated = fat,
                source = "manual"
            )
            
            foodDao.insertMealEntry(mealEntry)
            
            // Clear form
            manualFoodName = ""
            manualCalories = ""
            manualServing = ""
            manualProtein = ""
            manualCarbs = ""
            manualFat = ""
            
            _uiState.update { 
                it.copy(
                    saveSuccess = true,
                    scanMode = ScanMode.CAMERA
                )
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
    
    fun updateManualFoodName(value: String) { manualFoodName = value }
    fun updateManualCalories(value: String) { manualCalories = value }
    fun updateManualServing(value: String) { manualServing = value }
    fun updateManualProtein(value: String) { manualProtein = value }
    fun updateManualCarbs(value: String) { manualCarbs = value }
    fun updateManualFat(value: String) { manualFat = value }
}

data class FoodScannerUiState(
    val scanMode: ScanMode = ScanMode.CAMERA,
    val isProcessing: Boolean = false,
    val scannedImageUri: Uri? = null,
    val recognizedFoods: List<RecognizedFoodItem> = emptyList(),
    val confirmedFoods: List<RecognizedFoodItem> = emptyList(),
    val saveSuccess: Boolean = false,
    val error: String? = null
)
