package com.kreggscode.bmr.data.remote.api

import com.kreggscode.bmr.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface PollinationsApi {
    
    @Multipart
    @POST("vision/food-recognize")
    suspend fun recognizeFood(
        @Part image: MultipartBody.Part,
        @Part("user_context") context: RequestBody
    ): Response<FoodRecognitionResponse>
    
    @POST("https://text.pollinations.ai/openai")
    suspend fun generateDietPlan(
        @Body request: DietPlanRequest
    ): Response<DietPlanResponse>
    
    @POST("https://text.pollinations.ai/openai")
    suspend fun chatWithNutritionist(
        @Body request: ChatRequest
    ): Response<ChatResponse>
    
    @POST("https://text.pollinations.ai/openai")
    suspend fun analyzeNutrition(
        @Body request: NutritionAnalysisRequest
    ): Response<NutritionAnalysisResponse>
    
    companion object {
        const val BASE_URL = "https://text.pollinations.ai/"
        const val TEMPERATURE = 1.0
    }
}
