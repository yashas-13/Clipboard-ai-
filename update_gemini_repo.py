with open('app/src/main/java/com/example/data/repository/GeminiAiRepositoryImpl.kt', 'r') as f:
    content = f.read()

# Add FormField and FormStructure import if needed
if 'import com.example.domain.repository.FormStructure' not in content:
    content = content.replace('import com.example.domain.repository.ClipboardAnalysis', 'import com.example.domain.repository.ClipboardAnalysis\nimport com.example.domain.repository.FormField\nimport com.example.domain.repository.FormStructure')

analyze_form_code = '''
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
                contents = listOf(Content(listOf(Part("Analyze and extract form fields from this text:\\n\\n$formText")))),
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
            val cleanLine = line.replace(Regex("[:\\\\*\\\\-\\\\_]"), " ").trim()
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
}'''

content = content[:-1].rstrip() + '\n' + analyze_form_code

with open('app/src/main/java/com/example/data/repository/GeminiAiRepositoryImpl.kt', 'w') as f:
    f.write(content)
