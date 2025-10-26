package com.kreggscode.bmr.data.remote.dto

import com.google.gson.annotations.SerializedName

// Food Recognition DTOs
data class FoodRecognitionResponse(
    @SerializedName("items") val items: List<RecognizedFood>,
    @SerializedName("notes") val notes: String? = null
)

data class RecognizedFood(
    @SerializedName("name") val name: String,
    @SerializedName("confidence") val confidence: Float,
    @SerializedName("kcal") val calories: Double,
    @SerializedName("protein") val protein: Double? = null,
    @SerializedName("carbs") val carbs: Double? = null,
    @SerializedName("fat") val fat: Double? = null,
    @SerializedName("portion") val portion: String
)

// Diet Plan DTOs
data class DietPlanRequest(
    @SerializedName("messages") val messages: List<ChatMessage>,
    @SerializedName("model") val model: String = "openai",
    @SerializedName("temperature") val temperature: Double = 1.0,
    @SerializedName("seed") val seed: Int? = null
)

data class DietPlanResponse(
    @SerializedName("content") val content: String
)

data class DietPlanContext(
    @SerializedName("bmr") val bmr: Double,
    @SerializedName("tdee") val tdee: Double,
    @SerializedName("calorie_target") val calorieTarget: Double,
    @SerializedName("dietary_preferences") val dietaryPreferences: List<String>? = null,
    @SerializedName("allergies") val allergies: List<String>? = null,
    @SerializedName("cuisine_preference") val cuisinePreference: List<String>? = null
)

// Chat DTOs
data class ChatRequest(
    @SerializedName("messages") val messages: List<ChatMessage>,
    @SerializedName("model") val model: String = "openai",
    @SerializedName("temperature") val temperature: Double = 1.0
)

data class ChatMessage(
    @SerializedName("role") val role: String, // "system", "user", "assistant"
    @SerializedName("content") val content: String
)

data class ChatResponse(
    @SerializedName("content") val content: String
)

// Nutrition Analysis DTOs
data class NutritionAnalysisRequest(
    @SerializedName("messages") val messages: List<ChatMessage>,
    @SerializedName("model") val model: String = "openai",
    @SerializedName("temperature") val temperature: Double = 1.0
)

data class NutritionAnalysisResponse(
    @SerializedName("content") val content: String
)
