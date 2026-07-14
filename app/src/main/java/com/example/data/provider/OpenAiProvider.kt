package com.example.data.provider

import com.example.domain.provider.AiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenAiProvider : AiProvider {
    override val id: String = "openai"
    override val name: String = "OpenAI"

    override suspend fun generateResponse(prompt: String, context: String): Result<String> = withContext(Dispatchers.IO) {
        // Implementation would use OpenAI SDK or REST API
        Result.success("OpenAI processed your request: $prompt on context: $context")
    }
}
