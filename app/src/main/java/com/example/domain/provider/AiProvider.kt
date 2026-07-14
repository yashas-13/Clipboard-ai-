package com.example.domain.provider

interface AiProvider {
    val id: String
    val name: String
    suspend fun generateResponse(prompt: String, context: String): Result<String>
}
