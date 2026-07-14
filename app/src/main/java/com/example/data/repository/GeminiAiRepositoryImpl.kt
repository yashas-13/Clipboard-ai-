package com.example.data.repository

import com.example.domain.repository.AiRepository
import com.example.domain.repository.ClipboardAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

// Data classes for REST API
@Serializable
data class GenerateContentRequest(val contents: List<Content>, val systemInstruction: Content? = null)

@Serializable
data class Content(val parts: List<Part>)

@Serializable
data class Part(val text: String)

@Serializable
data class GenerateContentResponse(val candidates: List<Candidate>)

@Serializable
data class Candidate(val content: Content)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

class GeminiAiRepositoryImpl : AiRepository {

    private val apiService: GeminiApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
            
        val json = Json { ignoreUnknownKeys = true }
        
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }

    override suspend fun summarize(text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(listOf(Part("Summarize the following text:\n\n$text")))),
                systemInstruction = Content(listOf(Part("You are a helpful AI assistant that provides concise summaries.")))
            )
            val response = apiService.generateContent(BuildConfig.GEMINI_API_KEY, request)
            val resultText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (resultText != null) {
                Result.success(resultText)
            } else {
                Result.failure(Exception("No summary generated"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun extractKeywords(text: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(listOf(Part("Extract a comma-separated list of keywords from the following text:\n\n$text"))))
            )
            val response = apiService.generateContent(BuildConfig.GEMINI_API_KEY, request)
            val resultText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (resultText != null) {
                val keywords = resultText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                Result.success(keywords)
            } else {
                Result.failure(Exception("No keywords extracted"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun analyzeClipboardContent(text: String): Result<ClipboardAnalysis> = withContext(Dispatchers.IO) {
        try {
            val systemInstructionText = """
                You are an advanced clipboard classification and tagging system.
                Your task is to analyze the copied text and output a JSON object containing:
                1. "category": One of the following categories: "URL", "Email", "Phone", "Password", "OTP", "Code", "SQL", "JSON", "JWT", "API Key", "Markdown", "Address", "Shopping", "Banking", "Prompt", "Notes", "Cryptocurrency", "Credit Card", "Social", "Text", "General", "Sensitive".
                2. "tags": An array of relevant hashtags and keywords (such as #api, #token, #password, #github, #secret, #sql, #auth, #link, #shopping, #note). If the text contains any secret keywords, tokens, password keys, or api credentials, extract relevant hashtags like #api, #token, #password, #secret, #key, #credentials. Do not extract the actual secrets as plain tags, only category-level hashtags.
                
                You MUST return ONLY valid JSON. No Markdown formatting, no code block backticks, no other text.
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(listOf(Part("Analyze this text:\n\n$text")))),
                systemInstruction = Content(listOf(Part(systemInstructionText)))
            )
            val response = apiService.generateContent(BuildConfig.GEMINI_API_KEY, request)
            var resultText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (resultText != null) {
                // Remove potential markdown code blocks (e.g. ```json ... ```)
                resultText = resultText.trim()
                if (resultText.startsWith("```")) {
                    resultText = resultText.substringAfter("{").substringBeforeLast("}")
                    resultText = "{$resultText}"
                }
                
                val json = Json { ignoreUnknownKeys = true }
                val analysis = json.decodeFromString<ClipboardAnalysis>(resultText)
                Result.success(analysis)
            } else {
                Result.failure(Exception("No analysis generated"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
