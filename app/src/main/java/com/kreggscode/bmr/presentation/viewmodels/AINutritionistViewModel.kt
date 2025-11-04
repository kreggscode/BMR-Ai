package com.kreggscode.bmr.presentation.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kreggscode.bmr.data.local.dao.BMRDao
import com.kreggscode.bmr.data.local.dao.UserDao
import com.kreggscode.bmr.data.remote.api.PollinationsApi
import com.kreggscode.bmr.data.remote.dto.ChatRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AINutritionistViewModel @Inject constructor(
    private val userDao: UserDao,
    private val bmrDao: BMRDao,
    private val aiService: com.kreggscode.bmr.data.api.PollinationsAIService,
    @ApplicationContext private val context: android.content.Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    init {
        loadUserContext()
    }
    
    private fun loadUserContext() {
        viewModelScope.launch {
            userDao.getCurrentUser().collect { user ->
                user?.let { profile ->
                    bmrDao.getLatestBMRRecord(profile.id).collect { bmrRecord ->
                        _uiState.update { state ->
                            state.copy(
                                userContext = UserContext(
                                    name = profile.name,
                                    bmr = bmrRecord?.bmrValue,
                                    tdee = bmrRecord?.tdeeValue,
                                    targetCalories = bmrRecord?.targetCalories,
                                    goal = profile.goalType
                                )
                            )
                        }
                    }
                }
            }
        }
    }
    
    fun sendMessage(message: String) {
        if (message.isBlank()) return
        
        // Add user message
        val userMessage = ChatMessage(
            content = message,
            isUser = true,
            timestamp = getCurrentTimestamp()
        )
        
        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                isTyping = true
            )
        }
        
        // Send to AI
        viewModelScope.launch {
            try {
                val aiResponse = getAIResponse(message)
                
                val aiMessage = ChatMessage(
                    content = aiResponse,
                    isUser = false,
                    timestamp = getCurrentTimestamp()
                )
                
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + aiMessage,
                        isTyping = false
                    )
                }
                
                // Save to history
                saveToHistory(userMessage, aiMessage)
                
            } catch (e: Exception) {
                // Fallback response
                val fallbackMessage = ChatMessage(
                    content = getFallbackResponse(message),
                    isUser = false,
                    timestamp = getCurrentTimestamp()
                )
                
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + fallbackMessage,
                        isTyping = false
                    )
                }
            }
        }
    }
    
    private suspend fun getAIResponse(userMessage: String): String = withContext(Dispatchers.IO) {
        val context = _uiState.value.userContext
        
        val contextInfo = if (context != null) {
            """
            User Profile:
            - Name: ${context.name}
            - BMR: ${context.bmr?.toInt() ?: "Not calculated"} kcal/day
            - TDEE: ${context.tdee?.toInt() ?: "Not calculated"} kcal/day
            - Goal: ${context.goal}
            - Target Calories: ${context.targetCalories?.toInt() ?: "Not set"} kcal/day
            """.trimIndent()
        } else {
            null
        }
        
        return@withContext try {
            android.util.Log.d("AINutritionist", "Calling Pollinations API with message: $userMessage")
            val result = aiService.getNutritionAdvice(
                question = userMessage,
                context = contextInfo
            )
            
            result.fold(
                onSuccess = { response ->
                    android.util.Log.d("AINutritionist", "API Success: $response")
                    response
                },
                onFailure = { error ->
                    android.util.Log.e("AINutritionist", "API Error: ${error.message}", error)
                    "⚠️ Connection Error: ${error.message}\n\nUsing offline response:\n\n${getFallbackResponse(userMessage)}"
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("AINutritionist", "Exception: ${e.message}", e)
            "⚠️ Error: ${e.message}\n\nUsing offline response:\n\n${getFallbackResponse(userMessage)}"
        }
    }
    
    private fun getFallbackResponse(message: String): String {
        val lowerMessage = message.lowercase()
        
        return when {
            lowerMessage.contains("protein") -> {
                "Great question about protein! Adults generally need 0.8-1.2g of protein per kg of body weight. Good sources include lean meats, fish, eggs, legumes, and dairy. For muscle building, aim for 1.6-2.2g per kg. Would you like specific meal suggestions?"
            }
            lowerMessage.contains("calorie") || lowerMessage.contains("calories") -> {
                "Based on your BMR and activity level, I can help you calculate your daily calorie needs. For weight loss, aim for a 300-500 calorie deficit. For muscle gain, a 200-300 calorie surplus works well. What's your current goal?"
            }
            lowerMessage.contains("meal plan") || lowerMessage.contains("diet plan") -> {
                "I can create a personalized meal plan based on your calorie target and preferences. A balanced approach includes 30% protein, 40% carbs, and 30% healthy fats. Would you like me to generate a specific plan for you?"
            }
            lowerMessage.contains("weight loss") || lowerMessage.contains("lose weight") -> {
                "For healthy weight loss, aim for 0.5-1kg per week through a moderate calorie deficit and regular exercise. Focus on whole foods, increase protein intake to preserve muscle, and stay hydrated. Consistency is key!"
            }
            lowerMessage.contains("muscle") || lowerMessage.contains("gain weight") -> {
                "To build muscle, ensure adequate protein intake (1.6-2.2g/kg), maintain a slight calorie surplus, and follow progressive resistance training. Recovery and sleep are equally important. Need a specific workout nutrition plan?"
            }
            lowerMessage.contains("vegetarian") || lowerMessage.contains("vegan") -> {
                "Plant-based diets can be very nutritious! Focus on combining different protein sources (legumes, nuts, seeds, whole grains) for complete amino acids. Consider B12 supplementation. Want specific meal ideas?"
            }
            lowerMessage.contains("snack") -> {
                "Healthy snacks can help maintain energy levels. Try Greek yogurt with berries, apple slices with almond butter, mixed nuts, or hummus with vegetables. Aim for 150-200 calories per snack."
            }
            lowerMessage.contains("water") || lowerMessage.contains("hydration") -> {
                "Aim for 2.7-3.7 liters of water daily (including fluids from food). Drink more during exercise or hot weather. A good indicator is pale yellow urine. Set reminders if you forget to drink water!"
            }
            lowerMessage.contains("breakfast") -> {
                "A balanced breakfast sets the tone for your day! Try oatmeal with fruits and nuts, eggs with whole grain toast, or Greek yogurt parfait. Aim for 300-400 calories with protein, fiber, and healthy fats."
            }
            lowerMessage.contains("supplement") -> {
                "Most nutrients should come from food, but some supplements may help. Common ones include Vitamin D, Omega-3, and B12 for vegetarians. Always consult a healthcare provider before starting new supplements."
            }
            else -> {
                "I'm here to help with your nutrition questions! I can assist with meal planning, calorie calculations, macro breakdowns, healthy recipes, and dietary advice. What would you like to know about?"
            }
        }
    }
    
    private fun saveToHistory(userMessage: ChatMessage, aiMessage: ChatMessage) {
        // TODO: Implement chat history persistence
    }
    
    private fun getCurrentTimestamp(): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date())
    }
    
    fun clearChat() {
        _uiState.update { state ->
            state.copy(messages = emptyList())
        }
    }
    
    fun sendMessageWithImage(message: String, imageUri: Uri?) {
        if (message.isBlank() && imageUri == null) return
        
        // Add user message
        val displayMessage = if (imageUri != null) {
            "$message 📷 [Image attached]"
        } else {
            message
        }
        
        val userMessage = ChatMessage(
            content = displayMessage,
            isUser = true,
            timestamp = getCurrentTimestamp()
        )
        
        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                isTyping = true
            )
        }
        
        // Send to AI with image analysis
        viewModelScope.launch {
            try {
                val aiResponse = if (imageUri != null) {
                    // Use vision API to analyze the image
                    val prompt = if (message.isNotBlank()) {
                        message
                    } else {
                        "Analyze this food image and provide detailed nutritional information. Identify all food items, estimate portion sizes, and provide calorie counts and macronutrient breakdown."
                    }
                    
                    val result = aiService.analyzeFoodImage(prompt, imageUri, context)
                    result.fold(
                        onSuccess = { analysis -> analysis },
                        onFailure = { error -> 
                            "I can see you've shared an image. I encountered an error analyzing it: ${error.message}. Please try again or describe what's in the image."
                        }
                    )
                } else {
                    // Regular text message
                    getAIResponse(message)
                }
                
                val aiMessage = ChatMessage(
                    content = aiResponse,
                    isUser = false,
                    timestamp = getCurrentTimestamp()
                )
                
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + aiMessage,
                        isTyping = false
                    )
                }
                
                saveToHistory(userMessage, aiMessage)
                
            } catch (e: Exception) {
                val fallbackMessage = ChatMessage(
                    content = "I encountered an error analyzing your image. Please try again or describe what's in the picture. Error: ${e.message}",
                    isUser = false,
                    timestamp = getCurrentTimestamp()
                )
                
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + fallbackMessage,
                        isTyping = false
                    )
                }
            }
        }
    }
    
    fun sendVoiceMessage(transcribedText: String) {
        if (transcribedText.isBlank()) return
        sendMessage(transcribedText)
    }
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val userContext: UserContext? = null,
    val error: String? = null,
    val isRecording: Boolean = false,
    val attachedImageUri: Uri? = null
)

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: String
)

data class UserContext(
    val name: String,
    val bmr: Double?,
    val tdee: Double?,
    val targetCalories: Double?,
    val goal: String
)
