package com.example.domain.repository

import kotlinx.serialization.Serializable

@Serializable
data class ClipboardAnalysis(
    val category: String,
    val tags: List<String>
)

@Serializable
data class FormField(
    val fieldId: String,
    val label: String,
    val category: String,
    val placeholder: String = ""
)

@Serializable
data class FormStructure(
    val formTitle: String = "Analyzed Form",
    val fields: List<FormField> = emptyList()
)

interface AiRepository {
    suspend fun summarize(text: String): Result<String>
    suspend fun extractKeywords(text: String): Result<List<String>>
    suspend fun analyzeClipboardContent(text: String): Result<ClipboardAnalysis>
    suspend fun chatWithContext(query: String, contextText: String): Result<String>
    suspend fun analyzeForm(formText: String): Result<FormStructure>
}
