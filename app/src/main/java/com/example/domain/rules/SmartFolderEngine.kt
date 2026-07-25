package com.example.domain.rules

import com.example.data.local.ClipboardItemEntity
import com.example.data.local.SmartRuleEntity
import java.util.Locale

object SmartFolderEngine {

    /**
     * Generate offline smart tags based on text content, source application, and category.
     */
    fun generateAutoTags(item: ClipboardItemEntity): List<String> {
        val autoTags = mutableSetOf<String>()
        val textLower = item.text.lowercase(Locale.ROOT)
        val appLower = item.sourceApp.lowercase(Locale.ROOT)

        // Existing user tags if any
        if (item.tags.isNotBlank()) {
            item.tags.split(",").forEach {
                val t = it.trim().removePrefix("#")
                if (t.isNotEmpty()) autoTags.add(t)
            }
        }

        // Source App Tags
        when {
            appLower.contains("chrome") || appLower.contains("browser") || appLower.contains("firefox") || appLower.contains("safari") -> autoTags.add("web")
            appLower.contains("slack") || appLower.contains("teams") || appLower.contains("whatsapp") || appLower.contains("telegram") || appLower.contains("discord") -> autoTags.add("chat")
            appLower.contains("code") || appLower.contains("studio") || appLower.contains("github") || appLower.contains("terminal") -> autoTags.add("dev")
            appLower.contains("amazon") || appLower.contains("ebay") || appLower.contains("shopping") || appLower.contains("flipkart") -> autoTags.add("shopping")
            appLower.contains("gmail") || appLower.contains("outlook") || appLower.contains("mail") -> autoTags.add("email")
            appLower.contains("twitter") || appLower.contains("x.com") || appLower.contains("instagram") || appLower.contains("linkedin") -> autoTags.add("social")
        }

        // Content / Keyword Based Tags
        if (textLower.contains("http://") || textLower.contains("https://")) {
            autoTags.add("link")
        }

        if (textLower.contains("price") || textLower.contains("cart") || textLower.contains("buy") ||
            textLower.contains("order") || textLower.contains("checkout") || textLower.contains("receipt") ||
            textLower.contains("$") || textLower.contains("€") || textLower.contains("₹") || textLower.contains("total:")) {
            autoTags.add("shopping")
        }

        if (textLower.contains("function ") || textLower.contains("class ") || textLower.contains("import ") ||
            textLower.contains("def ") || textLower.contains("val ") || textLower.contains("const ") || textLower.contains("select *")) {
            autoTags.add("code")
        }

        if (item.category == "OTP" || textLower.contains("otp") || textLower.contains("verification code") || textLower.contains("passcode")) {
            autoTags.add("auth")
            autoTags.add("security")
        }

        if (textLower.contains("meeting") || textLower.contains("zoom") || textLower.contains("google meet") || textLower.contains("agenda")) {
            autoTags.add("work")
        }

        return autoTags.map { "#$it" }
    }

    /**
     * Check if a ClipboardItem matches a given SmartRule.
     */
    fun matchesRule(item: ClipboardItemEntity, rule: SmartRuleEntity): Boolean {
        if (!rule.isEnabled) return false

        val textLower = item.text.lowercase(Locale.ROOT)
        val appLower = item.sourceApp.lowercase(Locale.ROOT)

        // 1. Source App Filter Check
        if (rule.sourceAppFilter.isNotBlank()) {
            val appFilterLower = rule.sourceAppFilter.lowercase(Locale.ROOT)
            val appMatches = appFilterLower.split(",").any { filter ->
                appLower.contains(filter.trim())
            }
            if (!appMatches) return false
        }

        // 2. Keyword Filter Check
        if (rule.keywordFilter.isNotBlank()) {
            val keywords = rule.keywordFilter.lowercase(Locale.ROOT).split(",")
            val keywordMatches = keywords.any { kw ->
                textLower.contains(kw.trim())
            }
            if (!keywordMatches) return false
        }

        // 3. Category Filter Check
        if (rule.categoryFilter.isNotBlank()) {
            if (!item.category.equals(rule.categoryFilter.trim(), ignoreCase = true)) {
                return false
            }
        }

        return true
    }

    data class RuleResult(
        val matchedFolderName: String?,
        val autoTags: List<String>
    )

    /**
     * Evaluates a text snippet and metadata against active smart rules and auto-tags.
     */
    fun evaluateRules(text: String, sourceApp: String, category: String, rules: List<SmartRuleEntity>): RuleResult {
        val tempItem = ClipboardItemEntity(
            text = text,
            category = category,
            preview = "",
            wordCount = 0,
            charCount = text.length,
            sourceApp = sourceApp
        )
        val autoTags = generateAutoTags(tempItem).toMutableList()

        var matchedFolder: String? = null
        for (rule in rules) {
            if (matchesRule(tempItem, rule)) {
                matchedFolder = rule.targetFolderName
                if (rule.tagsToApply.isNotBlank()) {
                    rule.tagsToApply.split(",").forEach {
                        val t = it.trim().let { tag -> if (tag.startsWith("#")) tag else "#$tag" }
                        if (t.isNotBlank() && !autoTags.contains(t)) {
                            autoTags.add(t)
                        }
                    }
                }
                break
            }
        }
        return RuleResult(matchedFolder, autoTags)
    }
}
