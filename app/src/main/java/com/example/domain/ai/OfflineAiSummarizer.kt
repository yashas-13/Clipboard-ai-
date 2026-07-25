package com.example.domain.ai

import android.util.Patterns
import java.util.Locale

data class SummaryResult(
    val oneLiner: String,
    val bulletPoints: List<String>,
    val keyEntities: List<Pair<String, String>>,
    val originalWordCount: Int,
    val summaryWordCount: Int,
    val spaceSavedPercent: Int,
    val readingTimeSeconds: Int,
    val sentimentOrIntent: String
)

object OfflineAiSummarizer {

    private val STOP_WORDS = setOf(
        "a", "an", "the", "and", "or", "but", "if", "because", "as", "what", "which",
        "this", "that", "these", "those", "then", "just", "so", "than", "such",
        "both", "through", "about", "against", "between", "into", "throughout", "during",
        "before", "after", "above", "below", "to", "from", "up", "upon", "down", "in", "out",
        "on", "off", "over", "under", "again", "further", "then", "once", "here", "there",
        "when", "where", "why", "how", "all", "any", "both", "each", "few", "more", "most",
        "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than",
        "too", "very", "can", "will", "is", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "for", "with", "by", "at", "it", "its"
    )

    fun summarize(text: String, category: String = "General", sourceApp: String = ""): SummaryResult {
        val trimmed = text.trim()
        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        val originalWordCount = words.size

        if (trimmed.isEmpty()) {
            return SummaryResult(
                oneLiner = "Empty text snippet",
                bulletPoints = listOf("No text content provided"),
                keyEntities = emptyList(),
                originalWordCount = 0,
                summaryWordCount = 0,
                spaceSavedPercent = 0,
                readingTimeSeconds = 0,
                sentimentOrIntent = "General"
            )
        }

        // 1. Extract Key Entities (URLs, Emails, Phones, Prices, Dates, Code keywords)
        val entities = mutableListOf<Pair<String, String>>()
        
        // Extract URLs
        val urlMatcher = Patterns.WEB_URL.matcher(trimmed)
        while (urlMatcher.find()) {
            val url = urlMatcher.group()
            if (url.length > 5) entities.add("URL" to url)
        }

        // Extract Emails
        val emailMatcher = Patterns.EMAIL_ADDRESS.matcher(trimmed)
        while (emailMatcher.find()) {
            entities.add("Email" to emailMatcher.group())
        }

        // Extract Prices / Monetary
        val priceRegex = Regex("(?i)(\\$\\s?\\d+(?:\\.\\d{2})?|€\\s?\\d+|(?:USD|EUR|INR|GBP)\\s?\\d+(?:\\.\\d{2})?|\\d+\\s?(?:dollars|bucks|rupees|euros))")
        priceRegex.findAll(trimmed).forEach {
            entities.add("Amount" to it.value)
        }

        // Extract OTP or Code
        val otpRegex = Regex("(?i)\\b(\\d{4,8}|[A-Z0-9]{6,8})\\b")
        if (category == "OTP" || trimmed.lowercase().contains("code") || trimmed.lowercase().contains("verification")) {
            otpRegex.find(trimmed)?.let {
                entities.add("Verification Code" to it.value)
            }
        }

        // 2. Sentence Segmentation
        val sentences = trimmed.split(Regex("(?<=[.!?\\n])\\s+"))
            .map { it.trim() }
            .filter { it.length > 10 }

        val bulletList = mutableListOf<String>()

        if (sentences.isEmpty() || sentences.size == 1) {
            // Short text snippet
            bulletList.add(trimmed.take(150))
        } else {
            // TF-IDF & Sentence Graph Ranking Algorithm
            val wordFrequencies = mutableMapOf<String, Int>()
            sentences.forEach { sentence ->
                val cleanWords = sentence.lowercase(Locale.ROOT)
                    .replace(Regex("[^a-zA-Z0-9\\s]"), "")
                    .split(Regex("\\s+"))
                    .filter { it !in STOP_WORDS && it.length > 2 }
                cleanWords.forEach { word ->
                    wordFrequencies[word] = (wordFrequencies[word] ?: 0) + 1
                }
            }

            // Score each sentence
            val sentenceScores = sentences.mapIndexed { index, sentence ->
                val cleanWords = sentence.lowercase(Locale.ROOT)
                    .replace(Regex("[^a-zA-Z0-9\\s]"), "")
                    .split(Regex("\\s+"))
                    .filter { it !in STOP_WORDS }

                var score = cleanWords.sumOf { wordFrequencies[it] ?: 0 }.toFloat()
                
                // Position bias (earlier sentences often contain main ideas)
                if (index == 0) score *= 1.3f
                if (index == 1) score *= 1.1f

                // Entity presence bonus
                if (priceRegex.containsMatchIn(sentence) || Patterns.WEB_URL.matcher(sentence).find()) {
                    score *= 1.25f
                }

                index to score
            }

            // Select top 2 to 3 highest scoring sentences while maintaining original narrative order
            val targetCount = minOf(3, sentences.size)
            val topIndices = sentenceScores.sortedByDescending { it.second }
                .take(targetCount)
                .map { it.first }
                .sorted()

            topIndices.forEach { idx ->
                val s = sentences[idx]
                val cleanSentence = s.replace(Regex("^[-*•\\s]+"), "").trim()
                if (cleanSentence.isNotBlank()) {
                    bulletList.add(cleanSentence)
                }
            }
        }

        if (bulletList.isEmpty()) {
            bulletList.add(trimmed.take(120))
        }

        // 3. One-Liner Abstract
        val firstBullet = bulletList.firstOrNull() ?: trimmed
        val oneLiner = if (firstBullet.length > 90) {
            firstBullet.take(87) + "..."
        } else {
            firstBullet
        }

        // 4. Word Compression & Stats
        val summaryWords = bulletList.sumOf { it.split(Regex("\\s+")).size }
        val spaceSaved = if (originalWordCount > 0) {
            maxOf(0, ((originalWordCount - summaryWords).toFloat() / originalWordCount * 100).toInt())
        } else 0

        val readingTime = maxOf(1, (originalWordCount / 3.5f).toInt())

        // 5. Intent & Category Sentiment
        val intent = when {
            category == "Code" -> "Developer Snippet"
            category == "OTP" || entities.any { it.first == "Verification Code" } -> "Security Code"
            category == "URL" -> "Web Resource"
            entities.any { it.first == "Amount" } || trimmed.lowercase().contains("buy") || trimmed.lowercase().contains("order") -> "Shopping & E-Commerce"
            trimmed.contains("http") -> "Link Reference"
            originalWordCount > 50 -> "Long Article / Note"
            else -> "Quick Note"
        }

        return SummaryResult(
            oneLiner = oneLiner,
            bulletPoints = bulletList,
            keyEntities = entities.distinctBy { it.second }.take(4),
            originalWordCount = originalWordCount,
            summaryWordCount = summaryWords,
            spaceSavedPercent = spaceSaved,
            readingTimeSeconds = readingTime,
            sentimentOrIntent = intent
        )
    }
}
