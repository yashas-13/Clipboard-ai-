package com.example.data.local

import android.util.Patterns
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONException

object ClipboardClassifier {
    fun classify(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return "Text"

        // Image Check (URIs that point to image media or image file paths)
        if (trimmed.startsWith("content://") && (trimmed.contains("image") || trimmed.contains("photo") || trimmed.contains("media") || trimmed.contains("mms"))) {
            return "Image"
        }
        val lowerTrimmed = trimmed.lowercase()
        if (trimmed.startsWith("file://") && (lowerTrimmed.endsWith(".png") || lowerTrimmed.endsWith(".jpg") || lowerTrimmed.endsWith(".jpeg") || lowerTrimmed.endsWith(".webp") || lowerTrimmed.endsWith(".gif"))) {
            return "Image"
        }

        // Sensitive information check (API Keys, Tokens, Passwords, etc.)
        val isSensitive = trimmed.matches(Regex(".*[A-Za-z0-9_-]{32,}.*")) || 
                          trimmed.contains("sk-") || 
                          trimmed.contains("API_KEY") || 
                          trimmed.startsWith("eyJ")
        if (isSensitive) return "Sensitive"

        // URL Check
        val urlPattern = Patterns.WEB_URL
        if (urlPattern.matcher(trimmed).matches() || trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return "URL"
        }

        // Email Check
        val emailPattern = Patterns.EMAIL_ADDRESS
        if (emailPattern.matcher(trimmed).matches()) {
            return "Email"
        }

        // Phone Check
        val phonePattern = Patterns.PHONE
        if (phonePattern.matcher(trimmed).matches() || trimmed.matches(Regex("^\\+?[0-9]{10,13}$"))) {
            return "Phone"
        }

        // OTP Check (4-8 digit number)
        if (trimmed.matches(Regex("^\\d{4,8}$"))) {
            return "OTP"
        }
        val lowerText = trimmed.lowercase()
        if ((lowerText.contains("otp") || lowerText.contains("code") || lowerText.contains("verification") || lowerText.contains("verify")) && 
            trimmed.contains(Regex("\\b\\d{4,8}\\b"))) {
            return "OTP"
        }

        // JSON Check
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            try {
                JSONObject(trimmed)
                return "JSON"
            } catch (e: JSONException) {
                try {
                    JSONArray(trimmed)
                    return "JSON"
                } catch (e2: JSONException) {
                    // Not valid JSON
                }
            }
        }

        // Code Check
        val codeKeywords = listOf(
            "import ", "package ", "public class ", "private val ", "fun ", "def ", "class ", 
            "const ", "let ", "var ", "function ", "void ", "int ", "String", "printf", "println",
            "<?php", "<html>", "body {", "SELECT * FROM", "INSERT INTO"
        )
        if (codeKeywords.any { trimmed.contains(it) } || trimmed.contains("{") && trimmed.contains("}") && trimmed.contains(";")) {
            return "Code"
        }

        // Rich Text / HTML Check
        if (trimmed.startsWith("<html") || trimmed.contains("<html>") || (trimmed.contains("<p>") && trimmed.contains("</p>"))) {
            return "Rich Text"
        }

        // Markdown Check
        val markdownRegexes = listOf(
            Regex("^#+ .*$", RegexOption.MULTILINE),       // Headers
            Regex("\\*\\*.*?\\*\\*"),                      // Bold
            Regex("\\* .*$", RegexOption.MULTILINE),       // Bullet list
            Regex("- .*$", RegexOption.MULTILINE),         // Bullet list
            Regex("\\[.*?\\]\\(.*?\\)"),                   // Links
            Regex("`{3}[\\s\\S]*?`{3}")                    // Code blocks
        )
        if (markdownRegexes.any { it.containsMatchIn(trimmed) }) {
            return "Markdown"
        }

        return "Text"
    }

    fun getPreview(text: String): String {
        val trimmed = text.trim()
        return if (trimmed.length > 100) {
            trimmed.take(100) + "..."
        } else {
            trimmed
        }
    }
}
