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
                // Analyze image with vision AI - provide detailed prompt
                val prompt = """Analyze this food image and identify ALL visible food items with high precision.

                REQUIREMENTS:
                - Identify every single food item you can see in the image
                - Be extremely specific about food names (e.g., "Grilled Chicken Breast" not just "Chicken")
                - Provide accurate nutritional estimates per 100g serving
                - If no food is visible, clearly state "No food items detected"

                OUTPUT FORMAT - Use this EXACT format for each food item:
                FOOD_ITEM: [Specific Food Name]
                CALORIES: [number] kcal per 100g
                PROTEIN: [number]g per 100g
                CARBS: [number]g per 100g
                FAT: [number]g per 100g
                PORTION: [estimated portion size, e.g., 200g, 1 cup, 1 slice]
                ---

                List ALL food items separately. Do not combine items. Be thorough and accurate.""".trimIndent()
                
                val result = aiService.analyzeFoodImage(prompt, imageUri, context)
                
                result.onSuccess { response ->
                    val recognizedFoods = parseAIResponse(response)
                    
                    if (recognizedFoods.isNotEmpty()) {
                        _uiState.update { 
                            it.copy(
                                recognizedFoods = recognizedFoods,
                                scanMode = ScanMode.RESULTS,
                                isProcessing = false
                            )
                        }
                    } else {
                        // No foods recognized
                        _uiState.update { 
                            it.copy(
                                recognizedFoods = listOf(
                                    RecognizedFoodItem(
                                        name = "No food detected - Try again with better lighting",
                                        confidence = 0.0f,
                                        calories = 0.0,
                                        portion = "N/A",
                                        protein = 0.0,
                                        carbs = 0.0,
                                        fat = 0.0
                                    )
                                ),
                                scanMode = ScanMode.RESULTS,
                                isProcessing = false,
                                error = "Could not identify any food items"
                            )
                        }
                    }
                }.onFailure { e ->
                    _uiState.update { 
                        it.copy(
                            recognizedFoods = listOf(
                                RecognizedFoodItem(
                                    name = "Analysis failed - ${e.message}",
                                    confidence = 0.0f,
                                    calories = 0.0,
                                    portion = "N/A",
                                    protein = 0.0,
                                    carbs = 0.0,
                                    fat = 0.0
                                )
                            ),
                            scanMode = ScanMode.RESULTS,
                            isProcessing = false,
                            error = e.message
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        recognizedFoods = listOf(
                            RecognizedFoodItem(
                                name = "Error: ${e.message ?: "Unknown error"}",
                                confidence = 0.0f,
                                calories = 0.0,
                                portion = "N/A",
                                protein = 0.0,
                                carbs = 0.0,
                                fat = 0.0
                            )
                        ),
                        scanMode = ScanMode.RESULTS,
                        isProcessing = false,
                        error = e.message
                    )
                }
            }
        }
    }
    
    fun processBarcode(imageUri: Uri) {
        _uiState.update { 
            it.copy(
                isProcessing = true,
                scannedImageUri = imageUri
            )
        }
        
        viewModelScope.launch {
            try {
                // Use AI to extract barcode information
                val prompt = """Analyze this barcode image and provide the product information.
                    If you can identify the product, provide:
                    - Product name
                    - Nutritional information per serving (calories, protein, carbs, fat)
                    - Serving size
                    
                    Format the response clearly.""".trimIndent()
                
                val result = aiService.analyzeFoodDescription(prompt)
                
                result.getOrNull()?.let { response ->
                    val foods = parseAIResponse(response)
                    _uiState.update { 
                        it.copy(
                            recognizedFoods = foods,
                            scanMode = ScanMode.RESULTS,
                            isProcessing = false
                        )
                    }
                } ?: run {
                    // Fallback
                    _uiState.update { 
                        it.copy(
                            recognizedFoods = listOf(
                                RecognizedFoodItem(
                                    name = "Product (Scan Again)",
                                    confidence = 0.5f,
                                    calories = 0.0,
                                    portion = "1 serving",
                                    protein = 0.0,
                                    carbs = 0.0,
                                    fat = 0.0
                                )
                            ),
                            scanMode = ScanMode.RESULTS,
                            isProcessing = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        recognizedFoods = listOf(
                            RecognizedFoodItem(
                                name = "Barcode scan failed - Try manual entry",
                                confidence = 0.0f,
                                calories = 0.0,
                                portion = "1 serving",
                                protein = 0.0,
                                carbs = 0.0,
                                fat = 0.0
                            )
                        ),
                        scanMode = ScanMode.RESULTS,
                        isProcessing = false
                    )
                }
            }
        }
    }
    
    // Removed - no longer needed, logic moved to processImage()
    
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
        val foods = mutableListOf<RecognizedFoodItem>()
        
        try {
            // Split by the separator "---" to get individual food items
            val foodBlocks = response.split("---").filter { it.trim().isNotEmpty() }
            
            foodBlocks.forEach { block ->
                try {
                    // Extract food name
                    val foodItemMatch = Regex("FOOD_ITEM:\\s*(.+)", RegexOption.IGNORE_CASE).find(block)
                    val name = foodItemMatch?.groupValues?.get(1)?.trim() ?: ""
                    
                    // Extract calories
                    val caloriesMatch = Regex("CALORIES:\\s*(\\d+\\.?\\d*)", RegexOption.IGNORE_CASE).find(block)
                    val calories = caloriesMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                    
                    // Extract protein
                    val proteinMatch = Regex("PROTEIN:\\s*(\\d+\\.?\\d*)", RegexOption.IGNORE_CASE).find(block)
                    val protein = proteinMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                    
                    // Extract carbs
                    val carbsMatch = Regex("CARBS:\\s*(\\d+\\.?\\d*)", RegexOption.IGNORE_CASE).find(block)
                    val carbs = carbsMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                    
                    // Extract fat
                    val fatMatch = Regex("FAT:\\s*(\\d+\\.?\\d*)", RegexOption.IGNORE_CASE).find(block)
                    val fat = fatMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                    
                    // Extract portion
                    val portionMatch = Regex("PORTION:\\s*(.+)", RegexOption.IGNORE_CASE).find(block)
                    val portion = portionMatch?.groupValues?.get(1)?.trim() ?: "100g"
                    
                    if (name.isNotEmpty() && name.length > 2 && calories > 0 && !name.contains("No food", ignoreCase = true)) {
                        foods.add(
                            RecognizedFoodItem(
                                name = name.take(50),
                                confidence = 0.90f,
                                calories = calories,
                                portion = portion,
                                protein = protein,
                                carbs = carbs,
                                fat = fat
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Skip malformed blocks
                }
            }
            
            // Fallback: If no structured data found, try the old parsing format
            if (foods.isEmpty()) {
                val lines = response.lines().filter { it.contains("Food:") || it.contains("FOOD_ITEM:") || it.contains("|") }
                
                lines.forEach { line ->
                    try {
                        // Try new format first
                        var name = ""
                        var calories = 0.0
                        var protein = 0.0
                        var carbs = 0.0
                        var fat = 0.0
                        var portion = "100g"
                        
                        if (line.contains("FOOD_ITEM:")) {
                            val foodItemMatch = Regex("FOOD_ITEM:\\s*(.+)", RegexOption.IGNORE_CASE).find(line)
                            name = foodItemMatch?.groupValues?.get(1)?.trim() ?: ""
                        } else if (line.contains("Food:")) {
                            val nameMatch = Regex("Food:\\s*([^|]+)").find(line)
                            name = nameMatch?.groupValues?.get(1)?.trim() ?: ""
                        }
                        
                        // Extract numbers from the line
                        val caloriesMatch = Regex("Calories:\\s*(\\d+\\.?\\d*)", RegexOption.IGNORE_CASE).find(line)
                        calories = caloriesMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                        
                        val proteinMatch = Regex("Protein:\\s*(\\d+\\.?\\d*)", RegexOption.IGNORE_CASE).find(line)
                        protein = proteinMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                        
                        val carbsMatch = Regex("(?:Carbs|Carbohydrate):\\s*(\\d+\\.?\\d*)", RegexOption.IGNORE_CASE).find(line)
                        carbs = carbsMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                        
                        val fatMatch = Regex("Fat:\\s*(\\d+\\.?\\d*)", RegexOption.IGNORE_CASE).find(line)
                        fat = fatMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                        
                        val portionMatch = Regex("Portion:\\s*([^|]+)", RegexOption.IGNORE_CASE).find(line)
                        portion = portionMatch?.groupValues?.get(1)?.trim() ?: "100g"
                        
                        if (name.isNotEmpty() && name.length > 3 && calories > 0) {
                            foods.add(
                                RecognizedFoodItem(
                                    name = name.take(50),
                                    confidence = 0.80f,
                                    calories = calories,
                                    portion = portion,
                                    protein = protein,
                                    carbs = carbs,
                                    fat = fat
                                )
                            )
                        }
                    } catch (e: Exception) {
                        // Skip malformed lines
                    }
                }
            }
            
            // Final fallback: Simple parsing if all else fails
            if (foods.isEmpty()) {
                val simpleLines = response.lines().filter { it.isNotBlank() && it.length > 10 && !it.contains("No food", ignoreCase = true) }
                simpleLines.take(3).forEach { line ->
                    val name = line.substringBefore(":").substringBefore("-").substringBefore("(").trim()
                    val caloriesMatch = Regex("(\\d+)\\s*(cal|kcal)", RegexOption.IGNORE_CASE).find(line)
                    val calories = caloriesMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 150.0
                    
                    if (name.length > 3) {
                        foods.add(
                            RecognizedFoodItem(
                                name = name.take(50),
                                confidence = 0.70f,
                                calories = calories,
                                portion = "100g",
                                protein = null,
                                carbs = null,
                                fat = null
                            )
                        )
                    }
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
            // Prevent duplicate saves
            if (_uiState.value.saveSuccess) {
                return@launch
            }
            
            val user = userDao.getCurrentUser().first() ?: return@launch
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val today = calendar.timeInMillis
            
            val foodsToSave = _uiState.value.recognizedFoods.toList() // Create copy to prevent concurrent modification
            
            foodsToSave.forEach { recognizedFood ->
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
                
                // Create meal entry with current timestamp
                val now = System.currentTimeMillis()
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
                    source = "scanner",
                    timestamp = now
                )
                
                foodDao.insertMealEntry(mealEntry)
            }
            
            _uiState.update { 
                it.copy(
                    saveSuccess = true,
                    scanMode = ScanMode.CAMERA,
                    recognizedFoods = emptyList() // Clear after saving
                )
            }
        }
    }
    
    fun addManualFood() {
        viewModelScope.launch {
            val user = userDao.getCurrentUser().first() ?: return@launch
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val today = calendar.timeInMillis
            
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
            
            // Create meal entry with current timestamp
            val now = System.currentTimeMillis()
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
                source = "manual",
                timestamp = now
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
