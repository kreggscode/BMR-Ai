package com.kreggscode.bmr.data.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PollinationsAIService @Inject constructor() {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val baseUrl = "https://text.pollinations.ai"
    
    /**
     * Generate text using Pollinations AI
     * @param prompt The user's message/question
     * @param systemPrompt Optional system instructions for AI behavior
     * @param temperature Creativity level (0.0-3.0), default 1.0
     * @return AI response text
     */
    suspend fun generateText(
        prompt: String,
        systemPrompt: String? = null,
        temperature: Float = 1.0f
    ): Result<String> {
        return try {
            val messages = JSONArray().apply {
                if (systemPrompt != null) {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                }
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            
            val requestBody = JSONObject().apply {
                put("model", "openai")
                put("messages", messages)
                put("temperature", temperature)
                put("max_tokens", 1000)
            }
            
            val request = Request.Builder()
                .url("$baseUrl/openai")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val jsonResponse = JSONObject(responseBody)
                val content = jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                
                Result.success(content)
            } else {
                Result.failure(Exception("API Error: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate diet plan using AI
     * @param bmr User's BMR value
     * @param goal User's fitness goal (weight loss, muscle gain, etc.)
     * @param dietType Type of diet plan (balanced, keto, vegetarian, etc.)
     * @return Formatted diet plan
     */
    suspend fun generateDietPlan(
        bmr: Double,
        goal: String,
        dietType: String = "balanced"
    ): Result<String> {
        val prompt = """
            Create a detailed daily diet plan for someone with:
            - BMR: $bmr calories
            - Goal: $goal
            - Diet Type: $dietType
            
            Provide:
            1. Daily calorie target
            2. Macronutrient breakdown (protein, carbs, fats)
            3. Sample meal plan for one day (breakfast, lunch, dinner, snacks)
            4. Hydration recommendations
            5. Key tips for success
            
            Format the response in a clear, structured way.
        """.trimIndent()
        
        return generateText(
            prompt = prompt,
            systemPrompt = "You are a professional nutritionist and diet planner. Provide practical, healthy, and achievable diet plans.",
            temperature = 0.8f
        )
    }
    
    /**
     * Analyze food from description
     * @param foodDescription Description of the food
     * @return Nutritional information
     */
    suspend fun analyzeFoodDescription(
        foodDescription: String
    ): Result<String> {
        val prompt = """
            Analyze this food and provide nutritional information:
            "$foodDescription"
            
            Provide:
            1. Estimated calories
            2. Protein, carbs, and fat content
            3. Key vitamins and minerals
            4. Health benefits or concerns
            5. Portion size recommendations
        """.trimIndent()
        
        return generateText(
            prompt = prompt,
            systemPrompt = "You are a nutrition expert. Provide accurate nutritional analysis of foods.",
            temperature = 0.7f
        )
    }
    
    /**
     * Get nutrition advice
     * @param question User's nutrition question
     * @param context Optional context (user's BMR, goals, etc.)
     * @return AI nutrition advice
     */
    suspend fun getNutritionAdvice(
        question: String,
        context: String? = null
    ): Result<String> {
        val fullPrompt = if (context != null) {
            "Context: $context\n\nQuestion: $question"
        } else {
            question
        }
        
        return generateText(
            prompt = fullPrompt,
            systemPrompt = """
                You are an AI Nutritionist assistant. Provide helpful, accurate, and friendly nutrition advice.
                - Be conversational and supportive
                - Give practical, actionable tips
                - Cite scientific facts when relevant
                - Encourage healthy habits
                - If asked about medical conditions, recommend consulting a healthcare professional
            """.trimIndent(),
            temperature = 1.0f  // Set to 1.0 as requested
        )
    }
    
    /**
     * Generate image URL for food visualization
     * @param foodName Name of the food
     * @return Image URL
     */
    fun generateFoodImageUrl(foodName: String): String {
        val encodedPrompt = java.net.URLEncoder.encode(
            "professional food photography of $foodName, high quality, appetizing",
            "UTF-8"
        )
        return "https://image.pollinations.ai/prompt/$encodedPrompt?width=512&height=512&model=flux"
    }
    
    /**
     * Analyze BMR results and provide insights
     * @param bmr Calculated BMR
     * @param age User's age
     * @param sex User's sex
     * @param activityLevel User's activity level
     * @return AI analysis and recommendations
     */
    suspend fun analyzeBMRResults(
        bmr: Double,
        age: Int,
        sex: String,
        activityLevel: String
    ): Result<String> {
        val prompt = """
            Analyze this BMR calculation and provide insights:
            - BMR: $bmr calories/day
            - Age: $age years
            - Sex: $sex
            - Activity Level: $activityLevel
            
            Provide:
            1. What this BMR means for the user
            2. Recommended daily calorie intake based on activity
            3. Tips to optimize metabolism
            4. Lifestyle recommendations
            5. Common mistakes to avoid
            
            Keep it encouraging and practical.
        """.trimIndent()
        
        return generateText(
            prompt = prompt,
            systemPrompt = "You are a fitness and nutrition expert. Provide insightful, motivating analysis of BMR results.",
            temperature = 0.9f
        )
    }
}
