package com.kreggscode.bmr.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
     * @param dietType Detailed diet type instructions from ViewModel
     * @return Formatted diet plan
     */
    suspend fun generateDietPlan(
        bmr: Double,
        goal: String,
        dietType: String = "balanced"
    ): Result<String> {
        // Use the detailed dietType instructions directly from ViewModel
        val prompt = """
            $dietType
            
            User's BMR: $bmr calories
            User's Goal: $goal
            
            Provide a complete daily meal plan with:
            1. Specific meal times and food items
            2. Exact portion sizes and calorie counts
            3. Macronutrient breakdown for each meal
            4. Hydration schedule
            5. Pre/post workout meals if applicable
            6. Shopping list for the day
            
            Make it practical, specific, and achievable. Include actual food names and quantities.
        """.trimIndent()
        
        return generateText(
            prompt = prompt,
            systemPrompt = "You are a professional nutritionist. Create SPECIFIC, DETAILED meal plans with exact foods and portions. Make each plan UNIQUE based on the diet type requirements.",
            temperature = 0.9f
        )
    }
    
    /**
     * Analyze food from description or image
     * @param foodDescription Description of the food or image analysis prompt
     * @return Nutritional information in structured format
     */
    suspend fun analyzeFoodDescription(
        foodDescription: String
    ): Result<String> {
        val prompt = """
            $foodDescription
            
            For EACH food item visible, provide this EXACT format:
            Food: [name] | Calories: [number]kcal | Protein: [number]g | Carbs: [number]g | Fat: [number]g | Portion: [size]
            
            Example:
            Food: Grilled Chicken Breast | Calories: 165kcal | Protein: 31g | Carbs: 0g | Fat: 3.6g | Portion: 100g
            Food: Brown Rice | Calories: 112kcal | Protein: 2.6g | Carbs: 24g | Fat: 0.9g | Portion: 100g
            
            Be specific and accurate. List all visible food items.
        """.trimIndent()
        
        return generateText(
            prompt = prompt,
            systemPrompt = """You are a professional nutritionist and food recognition expert.
                Analyze food images and descriptions with precision.
                Always provide nutritional data in the EXACT format requested.
                Be thorough - identify ALL food items you can see.
                Use realistic portion sizes and accurate calorie counts.""".trimIndent(),
            temperature = 0.7f
        )
    }
    
    /**
     * Analyze food from image using vision AI
     * @param prompt Text prompt describing what to analyze
     * @param imageUri URI of the image to analyze
     * @param context Android context for accessing the image
     * @return Nutritional information in structured format
     */
    suspend fun analyzeFoodImage(
        prompt: String,
        imageUri: android.net.Uri,
        context: android.content.Context
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("PollinationsAI", "Starting food image analysis...")
            android.util.Log.d("PollinationsAI", "Image URI: $imageUri")
            
            // Convert image to base64
            val base64Image = convertImageToBase64(imageUri, context)
            android.util.Log.d("PollinationsAI", "Image converted to base64, length: ${base64Image.length}")
            
            val contentArray = JSONArray().apply {
                // Text content
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", prompt)
                })
                
                // Image content - using proper format for Pollinations.AI vision API
                put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:image/jpeg;base64,$base64Image")
                    })
                })
            }
            
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", contentArray)
                })
            }
            
            val requestBody = JSONObject().apply {
                put("model", "openai")  // Using openai model for vision support
                put("messages", messages)
                put("temperature", 1.0)  // Set to 1.0 as per Pollinations.AI recommendation
                put("max_tokens", 1500)  // Increased for detailed food analysis
            }
            
            android.util.Log.d("PollinationsAI", "Request body prepared, sending to API...")
            
            val request = Request.Builder()
                .url("$baseUrl/openai")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            
            android.util.Log.d("PollinationsAI", "Response received: ${response.code}")
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                android.util.Log.d("PollinationsAI", "Response body: ${responseBody.take(500)}...")
                
                val jsonResponse = JSONObject(responseBody)
                val content = jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                
                android.util.Log.d("PollinationsAI", "Analysis successful: ${content.take(200)}...")
                Result.success(content)
            } else {
                val errorBody = response.body?.string() ?: "No error details"
                android.util.Log.e("PollinationsAI", "Vision API Error: ${response.code} - ${response.message}")
                android.util.Log.e("PollinationsAI", "Error body: $errorBody")
                Result.failure(Exception("Vision API Error: ${response.code} - ${response.message}\n$errorBody"))
            }
        } catch (e: Exception) {
            android.util.Log.e("PollinationsAI", "Exception in analyzeFoodImage: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private fun convertImageToBase64(imageUri: android.net.Uri, context: android.content.Context): String {
        return try {
            android.util.Log.d("PollinationsAI", "Converting image to base64...")
            
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: throw Exception("Cannot open image stream")
            
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                ?: throw Exception("Cannot decode image")
            
            android.util.Log.d("PollinationsAI", "Original image size: ${bitmap.width}x${bitmap.height}")
            
            // Resize if too large (max 1024x1024 for better quality while staying within limits)
            val maxSize = 1024
            val scaledBitmap = if (bitmap.width > maxSize || bitmap.height > maxSize) {
                val scale = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()
                
                android.util.Log.d("PollinationsAI", "Resizing to: ${newWidth}x${newHeight}")
                
                android.graphics.Bitmap.createScaledBitmap(
                    bitmap,
                    newWidth,
                    newHeight,
                    true
                )
            } else {
                bitmap
            }
            
            val outputStream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
            val byteArray = outputStream.toByteArray()
            
            android.util.Log.d("PollinationsAI", "Compressed image size: ${byteArray.size} bytes")
            
            // Clean up
            inputStream.close()
            outputStream.close()
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            bitmap.recycle()
            
            android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            android.util.Log.e("PollinationsAI", "Error converting image to base64: ${e.message}", e)
            throw Exception("Failed to convert image: ${e.message}", e)
        }
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
