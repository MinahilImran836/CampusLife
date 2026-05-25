package com.example.campuslife.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// Request payload matching our Python FastAPI Pydantic schema
data class NotePayload(val text: String)

// Response layout receiving our Float array mapping
data class EmbeddingResponse(val embedding: List<Float>)

// Request payload matching Python AgentRequest schema to include context strings
data class AgentRequest(
    val query: String,
    val context: String
)

// Response layout matching Python AgentResponse schema
data class AgentResponse(val answer: String)

interface CampusLifeApiService {
    @POST("get_embedding/") // Added trailing slash to prevent 307 redirects
    suspend fun getNoteEmbedding(@Body payload: NotePayload): EmbeddingResponse

    @POST("ask/") // Added trailing slash to prevent 307 redirects
    suspend fun askAgent(@Body request: AgentRequest): AgentResponse
}

object RetrofitClient {
    // Update this IP if your laptop's local IP changes on your Wi-Fi network
    private const val BASE_URL = "http://192.168.1.15:8000/"

    val apiService: CampusLifeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CampusLifeApiService::class.java)
    }
}