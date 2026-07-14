package com.example.data.provider

import com.example.domain.provider.AiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiProvider : AiProvider {
    override val id: String = "gemini"
    override val name: String = "Google Gemini"

    override suspend fun generateResponse(prompt: String, context: String): Result<String> = withContext(Dispatchers.IO) {
        // Implementation would use Gemini SDK or REST API
        Result.success("Gemini processed your request: $prompt on context: $context")
    }
}
