package com.example.campuslife.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class NotePayload(val text: String)
data class EmbeddingResponse(val embedding: List<Float>)
data class AgentRequest(val query: String, val context: String)
data class AgentResponse(val answer: String)

interface CampusLifeApiService {
    // Removed trailing slash in POST annotation to let Retrofit handle the relative path
    @POST("get_embedding")
    suspend fun getNoteEmbedding(@Body payload: NotePayload): EmbeddingResponse

    @POST("ask")
    suspend fun askAgent(@Body request: AgentRequest): AgentResponse
}

object RetrofitClient {
    // Ensure this IP is exactly your machine's local Wi-Fi IP
    private const val BASE_URL = "http://192.168.1.15:8000/"

    val apiService: CampusLifeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CampusLifeApiService::class.java)
    }
}