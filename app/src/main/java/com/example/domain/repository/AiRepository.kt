package com.example.domain.repository

import kotlinx.serialization.Serializable

@Serializable
data class ClipboardAnalysis(
    val category: String,
    val tags: List<String>
)

interface AiRepository {
    suspend fun summarize(text: String): Result<String>
    suspend fun extractKeywords(text: String): Result<List<String>>
    suspend fun analyzeClipboardContent(text: String): Result<ClipboardAnalysis>
}
