package com.example.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.domain.repository.AiRepository
import com.example.domain.repository.ClipboardAnalysis
import com.example.domain.repository.FormField
import com.example.domain.repository.FormStructure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

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

class GeminiAiRepositoryImpl(private val context: Context) : AiRepository {
    private fun getApiKey(): String {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val userKey = prefs.getString("api_key", "")
        return if (!userKey.isNullOrEmpty()) userKey else BuildConfig.GEMINI_API_KEY
    }

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
            val response = apiService.generateContent(getApiKey(), request)
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
            val response = apiService.generateContent(getApiKey(), request)
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
                1. "category": One of the following categories: "URL", "Email", "Phone", "Password", "OTP", "Code", "SQL", "JSON", "JWT", "API Key", "Credentials", "Markdown", "Address", "Shopping", "Banking", "Prompt", "Notes", "Cryptocurrency", "Credit Card", "Social", "Text", "General", "Sensitive".
                2. "tags": An array of relevant hashtags and keywords (such as #api, #token, #password, #github, #secret, #sql, #auth, #link, #shopping, #note). If the text contains any secret keywords, tokens, password keys, or api credentials, extract relevant hashtags like #api, #token, #password, #secret, #key, #credentials. Do not extract the actual secrets as plain tags, only category-level hashtags.

                You MUST return ONLY valid JSON. No Markdown formatting, no code block backticks, no other text.
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(listOf(Part("Analyze this text:\n\n$text")))),
                systemInstruction = Content(listOf(Part(systemInstructionText)))
            )
            val response = apiService.generateContent(getApiKey(), request)
            var resultText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (resultText != null) {
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

    override suspend fun chatWithContext(query: String, contextText: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemInstructionText = """
                You are Clipboard AI's advanced RAG (Retrieval-Augmented Generation) assistant.
                You are given a user query and a context containing multiple copied clipboard items.
                Your task is to answer the user's query accurately using the information in the clipboard context.
                Be clear, concise, and helpful. Use markdown formatting where appropriate (e.g., bullet points, bold text, code blocks).
                If the context does not contain the answer, politely say so but offer to help based on general knowledge if possible.
            """.trimIndent()

            val prompt = """
                User Query: $query
                Clipboard Context:
                $contextText
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(listOf(Part(prompt)))),
                systemInstruction = Content(listOf(Part(systemInstructionText)))
            )
            val response = apiService.generateContent(getApiKey(), request)
            val resultText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (resultText != null) {
                Result.success(resultText)
            } else {
                Result.failure(Exception("No answer generated"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun analyzeForm(formText: String): Result<FormStructure> = withContext(Dispatchers.IO) {
        try {
            val systemInstructionText = """
                You are a form layout and structure detection AI.
                Your task is to analyze raw text, HTML form snippets, input labels, or unstructured form descriptions, and identify all form fields.
                Output a JSON object with:
                1. "formTitle": A short title for the form (e.g., "User Registration", "Checkout Form", "Developer Credentials", "Contact Information").
                2. "fields": An array of objects with:
                   - "fieldId": Snake case string identifier (e.g. "email_address", "api_key", "phone_number").
                   - "label": Human readable label (e.g. "Email Address", "API Key", "Mobile Phone").
                   - "category": One of standard categories: "Email", "Phone", "Password", "API Key", "Credentials", "Address", "OTP", "URL", "Banking", "Notes", "Name", "Text".
                   - "placeholder": Optional string hint.

                You MUST return ONLY valid JSON. No Markdown formatting, no code block backticks.
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(listOf(Part("Analyze and extract form fields from this text:\n\n$formText")))),
                systemInstruction = Content(listOf(Part(systemInstructionText)))
            )
            val response = apiService.generateContent(getApiKey(), request)
            var resultText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (resultText != null) {
                resultText = resultText.trim()
                if (resultText.startsWith("```")) {
                    resultText = resultText.substringAfter("{").substringBeforeLast("}")
                    resultText = "{$resultText}"
                }
                val json = Json { ignoreUnknownKeys = true }
                val structure = json.decodeFromString<FormStructure>(resultText)
                if (structure.fields.isNotEmpty()) {
                    return@withContext Result.success(structure)
                }
            }
            Result.success(parseFormHeuristically(formText))
        } catch (e: Exception) {
            Result.success(parseFormHeuristically(formText))
        }
    }

    private fun parseFormHeuristically(text: String): FormStructure {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val fields = mutableListOf<FormField>()
        val defaultTitle = "Detected Form"

        for ((index, line) in lines.withIndex()) {
            val cleanLine = line.replace(Regex("[:\\*\\-\\_]"), " ").trim()
            if (cleanLine.isBlank()) continue

            val category = when {
                cleanLine.contains("email", ignoreCase = true) -> "Email"
                cleanLine.contains("phone", ignoreCase = true) || cleanLine.contains("mobile", ignoreCase = true) || cleanLine.contains("tel", ignoreCase = true) -> "Phone"
                cleanLine.contains("api key", ignoreCase = true) || cleanLine.contains("secret", ignoreCase = true) || cleanLine.contains("token", ignoreCase = true) -> "API Key"
                cleanLine.contains("password", ignoreCase = true) || cleanLine.contains("passcode", ignoreCase = true) -> "Password"
                cleanLine.contains("username", ignoreCase = true) || cleanLine.contains("credentials", ignoreCase = true) || cleanLine.contains("login", ignoreCase = true) -> "Credentials"
                cleanLine.contains("address", ignoreCase = true) || cleanLine.contains("street", ignoreCase = true) || cleanLine.contains("city", ignoreCase = true) || cleanLine.contains("zip", ignoreCase = true) -> "Address"
                cleanLine.contains("otp", ignoreCase = true) || cleanLine.contains("code", ignoreCase = true) || cleanLine.contains("pin", ignoreCase = true) -> "OTP"
                cleanLine.contains("url", ignoreCase = true) || cleanLine.contains("website", ignoreCase = true) || cleanLine.contains("link", ignoreCase = true) -> "URL"
                cleanLine.contains("name", ignoreCase = true) -> "Name"
                cleanLine.contains("card", ignoreCase = true) || cleanLine.contains("bank", ignoreCase = true) -> "Banking"
                else -> "Text"
            }

            fields.add(
                FormField(
                    fieldId = "field_$index",
                    label = cleanLine.take(30),
                    category = category,
                    placeholder = "Enter $cleanLine"
                )
            )
        }

        if (fields.isEmpty()) {
            fields.add(FormField("field_0", "Input Value", "Text", "Enter value"))
        }

        return FormStructure(formTitle = defaultTitle, fields = fields)
    }
}
