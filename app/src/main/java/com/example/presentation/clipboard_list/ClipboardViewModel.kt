package com.example.presentation.clipboard_list

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.data.local.ClipboardClassifier
import com.example.data.local.ClipboardGroupEntity
import com.example.data.local.ClipboardItemEntity
import com.example.data.local.GroupItemCrossRefEntity
import com.example.data.local.SmartRuleEntity
import com.example.domain.ai.OfflineAiSummarizer
import com.example.domain.ai.SummaryResult
import com.example.domain.rules.SmartFolderEngine
import com.example.domain.repository.AiRepository
import com.example.domain.repository.ClipboardRepository
import com.example.domain.repository.FormField
import com.example.domain.repository.FormStructure
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class AutoSuggestOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val textToPaste: String,
    val type: SuggestionType,
    val badgeLabel: String
)

enum class SuggestionType {
    SYSTEM_CLIPBOARD,
    OTP_CODE,
    URL_LINK,
    EMAIL,
    RELEVANT_MATCH,
    SNIPPET
}

data class CategoryCount(
    val category: String,
    val count: Int
)

data class DailyCount(
    val dateLabel: String,
    val count: Int
)

data class DashboardStats(
    val totalItems: Int,
    val averageLength: Int,
    val totalWords: Int,
    val totalChars: Int,
    val copyVelocityPerDay: Float = 0f,
    val favoriteItemsCount: Int = 0,
    val pinnedItemsCount: Int = 0,
    val peakHourLabel: String = "12:00 - 13:00",
    val mostCopiedSnippet: ClipboardItemEntity? = null,
    val categoryStats: List<CategoryCount> = emptyList(),
    val dailyStats: List<DailyCount> = emptyList(),
    val hourlyStats: List<Int> = List(24) { 0 },
    val topSourceApps: List<Pair<String, Int>> = emptyList(),
    val topTags: List<Pair<String, Int>> = emptyList()
)

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

class ClipboardViewModel(
    private val repository: ClipboardRepository,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Privacy mask state
    private val _isPrivacyModeEnabled = MutableStateFlow(false)
    val isPrivacyModeEnabled: StateFlow<Boolean> = _isPrivacyModeEnabled

    fun togglePrivacyMode() {
        _isPrivacyModeEnabled.value = !_isPrivacyModeEnabled.value
    }

    // Selected items for bulk actions
    private val _selectedItemIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedItemIds: StateFlow<Set<Int>> = _selectedItemIds

    fun toggleSelection(id: Int) {
        val current = _selectedItemIds.value
        _selectedItemIds.value = if (current.contains(id)) {
            current - id
        } else {
            current + id
        }
    }

    fun selectAll(allIds: List<Int>) {
        _selectedItemIds.value = allIds.toSet()
    }

    fun clearSelection() {
        _selectedItemIds.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val selected = _selectedItemIds.value
            selected.forEach { id ->
                val item = repository.getAllItems().first().find { it.id == id }
                item?.let { repository.deleteItem(it) }
            }
            clearSelection()
        }
    }

    fun categorizeSelected(category: String) {
        viewModelScope.launch {
            val selected = _selectedItemIds.value
            val allItems = repository.getAllItems().first()
            selected.forEach { id ->
                val item = allItems.find { it.id == id }
                item?.let {
                    repository.updateItem(it.copy(category = category))
                }
            }
            clearSelection()
        }
    }

    // Tag filter state
    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter: StateFlow<String?> = _selectedTagFilter.asStateFlow()

    fun selectTagFilter(tag: String?) {
        _selectedTagFilter.value = if (_selectedTagFilter.value == tag) null else tag
    }

    // Dynamic list of available tags and categories with count
    val availableTags: StateFlow<List<Pair<String, Int>>> = repository.getAllItems()
        .map { list ->
            val tagMap = mutableMapOf<String, Int>()
            list.forEach { item ->
                // Category as tag
                if (item.category.isNotBlank()) {
                    val catTag = item.category
                    tagMap[catTag] = (tagMap[catTag] ?: 0) + 1
                }
                // Custom tags
                if (item.tags.isNotBlank()) {
                    item.tags.split(",").forEach { rawTag ->
                        val t = rawTag.trim()
                        if (t.isNotBlank()) {
                            val formattedTag = if (t.startsWith("#")) t else "#$t"
                            tagMap[formattedTag] = (tagMap[formattedTag] ?: 0) + 1
                        }
                    }
                }
            }
            tagMap.entries
                .sortedByDescending { it.value }
                .map { Pair(it.key, it.value) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<ClipboardItemEntity>> = combine(
        _searchQuery,
        _selectedTagFilter,
        repository.getAllItems()
    ) { query, selectedTag, list ->
        list.filter { item ->
            val matchesQuery = query.isBlank() || (
                item.text.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true) ||
                item.tags.contains(query, ignoreCase = true) ||
                item.sourceApp.contains(query, ignoreCase = true) ||
                item.preview.contains(query, ignoreCase = true)
            )

            val matchesTag = selectedTag.isNullOrBlank() || (
                item.category.equals(selectedTag, ignoreCase = true) ||
                item.tags.contains(selectedTag, ignoreCase = true) ||
                item.tags.split(",").any { it.trim().equals(selectedTag, ignoreCase = true) || "#${it.trim()}".equals(selectedTag, ignoreCase = true) }
            )

            matchesQuery && matchesTag
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _aiResult = MutableStateFlow<String?>(null)
    val aiResult: StateFlow<String?> = _aiResult

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val categoryStats: StateFlow<List<CategoryCount>> = repository.getAllItems()
        .map { list ->
            list.groupBy { it.category }
                .map { (cat, items) -> CategoryCount(cat, items.size) }
                .sortedByDescending { it.count }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val dailyStats: StateFlow<List<DailyCount>> = repository.getAllItems()
        .map { list ->
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            val dateKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            
            val last7DaysKeys = mutableListOf<String>()
            val last7DaysLabels = mutableListOf<String>()
            
            for (i in 6 downTo 0) {
                val tempCal = Calendar.getInstance()
                tempCal.add(Calendar.DAY_OF_YEAR, -i)
                val key = dateKeyFormat.format(tempCal.time)
                val label = dateFormat.format(tempCal.time)
                last7DaysKeys.add(key)
                last7DaysLabels.add(label)
            }
            
            val groupedByDay = list.groupBy {
                dateKeyFormat.format(Date(it.timestamp))
            }
            
            last7DaysKeys.mapIndexed { index, key ->
                val count = groupedByDay[key]?.size ?: 0
                DailyCount(last7DaysLabels[index], count)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Dashboard Stats Flows ---
    val stats: StateFlow<DashboardStats> = combine(
        repository.getAllItems(),
        categoryStats,
        dailyStats
    ) { list, catStats, dStats ->
        val total = list.size
        val avgLen = if (total > 0) list.map { it.text.length }.average().toInt() else 0
        val totalWords = list.sumOf { it.wordCount }
        val totalChars = list.sumOf { it.charCount }
        val favorites = list.count { it.isFavorite }
        val pinned = list.count { it.isPinned }

        val oldestTime = list.minOfOrNull { it.timestamp } ?: System.currentTimeMillis()
        val daysDiff = maxOf(1, ((System.currentTimeMillis() - oldestTime) / (1000 * 60 * 60 * 24)).toInt() + 1)
        val velocity = if (total > 0) total.toFloat() / daysDiff else 0f

        val hoursMap = list.groupBy {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.HOUR_OF_DAY)
        }
        val hourlyCounts = List(24) { hour -> hoursMap[hour]?.size ?: 0 }
        val peakHourIndex = if (hourlyCounts.any { it > 0 }) hourlyCounts.indices.maxByOrNull { hourlyCounts[it] } ?: 12 else 12
        val peakHourStr = String.format("%02d:00 - %02d:00", peakHourIndex, (peakHourIndex + 1) % 24)

        val topCopied = list.maxByOrNull { it.copyCount }

        val topApps = list.groupBy { it.sourceApp.ifBlank { "System" } }
            .map { (app, items) -> Pair(app, items.size) }
            .sortedByDescending { it.second }
            .take(5)

        val tagsMap = mutableMapOf<String, Int>()
        list.forEach { item ->
            if (item.tags.isNotBlank()) {
                item.tags.split(",").forEach { tag ->
                    val t = tag.trim()
                    if (t.isNotEmpty()) {
                        tagsMap[t] = (tagsMap[t] ?: 0) + 1
                    }
                }
            }
        }
        val topTagsList = tagsMap.entries.sortedByDescending { it.value }.take(5).map { Pair(it.key, it.value) }

        DashboardStats(
            totalItems = total,
            averageLength = avgLen,
            totalWords = totalWords,
            totalChars = totalChars,
            copyVelocityPerDay = velocity,
            favoriteItemsCount = favorites,
            pinnedItemsCount = pinned,
            peakHourLabel = peakHourStr,
            mostCopiedSnippet = topCopied,
            categoryStats = catStats,
            dailyStats = dStats,
            hourlyStats = hourlyCounts,
            topSourceApps = topApps,
            topTags = topTagsList
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardStats(0, 0, 0, 0)
    )

    // --- Smart Rules & Auto-Tagging States & Operations ---
    val smartRules: StateFlow<List<SmartRuleEntity>> = repository.getAllSmartRules()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Pre-populate default smart rules and pre-built items if empty
        viewModelScope.launch {
            val existingRules = repository.getAllSmartRules().first()
            if (existingRules.isEmpty()) {
                repository.insertSmartRule(
                    SmartRuleEntity(
                        ruleName = "Chrome Shopping",
                        targetFolderName = "Shopping",
                        sourceAppFilter = "Chrome, Browser",
                        keywordFilter = "buy, price, cart, order, shop, amazon, ebay, checkout, total",
                        tagsToApply = "#shopping, #web"
                    )
                )
                repository.insertSmartRule(
                    SmartRuleEntity(
                        ruleName = "Developer Code Snippets",
                        targetFolderName = "Development",
                        sourceAppFilter = "",
                        keywordFilter = "function, class, val, import, def, const, select",
                        categoryFilter = "Code",
                        tagsToApply = "#dev, #code"
                    )
                )
                repository.insertSmartRule(
                    SmartRuleEntity(
                        ruleName = "Verification & Security Codes",
                        targetFolderName = "Security & OTP",
                        sourceAppFilter = "",
                        categoryFilter = "OTP",
                        tagsToApply = "#auth, #security"
                    )
                )
            }

            val existingItems = repository.getAllItems().first()
            if (existingItems.isEmpty()) {
                seedPrebuiltItemsAndRules()
            }
        }
    }

    fun seedPrebuiltItemsAndRules() {
        viewModelScope.launch {
            val existing = repository.getAllItems().first()
            if (existing.isNotEmpty()) return@launch

            val prebuiltClips = listOf(
                ClipboardItemEntity(
                    text = "fun calculateTokenCount(text: String): Int = text.split(\" \").size * 2",
                    category = "Code",
                    preview = "fun calculateTokenCount(text: String): Int",
                    sourceApp = "Android Studio",
                    wordCount = 9,
                    charCount = 68,
                    tags = "#kotlin, #dev"
                ),
                ClipboardItemEntity(
                    text = "sk-proj-a89f920192039102938401928301",
                    category = "Sensitive",
                    preview = "sk-proj-a89f92019203...",
                    sourceApp = "Developer Console",
                    wordCount = 1,
                    charCount = 36,
                    tags = "#secret, #apikey",
                    isPinned = true
                ),
                ClipboardItemEntity(
                    text = "https://developer.android.com/jetpack/compose",
                    category = "URL",
                    preview = "https://developer.android.com/jetpack/compose",
                    sourceApp = "Chrome",
                    wordCount = 1,
                    charCount = 45,
                    tags = "#docs, #android"
                ),
                ClipboardItemEntity(
                    text = "Your verification code is 849201. Do not share with anyone.",
                    category = "OTP",
                    preview = "OTP Code: 849201",
                    sourceApp = "Messages",
                    wordCount = 10,
                    charCount = 58,
                    tags = "#security, #verification"
                ),
                ClipboardItemEntity(
                    text = "Prompt Template: Act as an expert Android engineer and optimize this Jetpack Compose layout for smooth 120Hz scrolling.",
                    category = "Work",
                    preview = "Prompt Template: Act as an expert Android engineer...",
                    sourceApp = "AI Assistant",
                    wordCount = 18,
                    charCount = 120,
                    tags = "#prompt, #ai",
                    isPinned = true
                ),
                ClipboardItemEntity(
                    text = "John Doe, 123 Innovation Way, Suite 400, San Francisco, CA 94105",
                    category = "Form Data",
                    preview = "John Doe, 123 Innovation Way...",
                    sourceApp = "Browser",
                    wordCount = 11,
                    charCount = 65,
                    tags = "#address, #contact"
                )
            )

            prebuiltClips.forEach { item ->
                repository.insertItem(item)
            }

            // Seed prebuilt smart groups
            val existingGroups = repository.getAllGroups().first()
            if (existingGroups.isEmpty()) {
                repository.createGroup("Dev & Code", "Code snippets and developer shortcuts")
                repository.createGroup("AI Prompts", "Templates and saved LLM prompts")
                repository.createGroup("Security", "Masked tokens and credentials")
            }
        }
    }

    // --- Offline AI Summarizer States & Operations ---
    private val _activeSummaryItem = MutableStateFlow<ClipboardItemEntity?>(null)
    val activeSummaryItem: StateFlow<ClipboardItemEntity?> = _activeSummaryItem

    private val _activeSummaryResult = MutableStateFlow<SummaryResult?>(null)
    val activeSummaryResult: StateFlow<SummaryResult?> = _activeSummaryResult

    fun requestOfflineSummary(item: ClipboardItemEntity) {
        _activeSummaryItem.value = item
        val result = OfflineAiSummarizer.summarize(
            text = item.text,
            category = item.category,
            sourceApp = item.sourceApp
        )
        _activeSummaryResult.value = result
    }

    fun dismissOfflineSummary() {
        _activeSummaryItem.value = null
        _activeSummaryResult.value = null
    }

    fun createSmartRule(
        ruleName: String,
        targetFolder: String,
        sourceApp: String = "",
        keywords: String = "",
        category: String = "",
        tags: String = ""
    ) {
        if (ruleName.isBlank() || targetFolder.isBlank()) return
        viewModelScope.launch {
            repository.insertSmartRule(
                SmartRuleEntity(
                    ruleName = ruleName,
                    targetFolderName = targetFolder,
                    sourceAppFilter = sourceApp,
                    keywordFilter = keywords,
                    categoryFilter = category,
                    tagsToApply = tags
                )
            )
        }
    }

    fun toggleSmartRule(rule: SmartRuleEntity) {
        viewModelScope.launch {
            repository.updateSmartRule(rule.copy(isEnabled = !rule.isEnabled))
        }
    }

    fun deleteSmartRule(rule: SmartRuleEntity) {
        viewModelScope.launch {
            repository.deleteSmartRule(rule)
        }
    }

    private suspend fun processSmartRulesAndAutoTags(insertedId: Int, item: ClipboardItemEntity) {
        // 1. Generate Content Auto-Tags
        val autoTagList = SmartFolderEngine.generateAutoTags(item)
        var updatedItem = item.copy(
            id = insertedId,
            tags = autoTagList.joinToString(", ")
        )

        // 2. Evaluate Smart Rules
        val activeRules = repository.getAllSmartRules().first()
        val allGroups = repository.getAllGroups().first()

        for (rule in activeRules) {
            if (SmartFolderEngine.matchesRule(updatedItem, rule)) {
                // Find or create target folder
                val group = allGroups.find { it.name.equals(rule.targetFolderName, ignoreCase = true) }
                val groupId = if (group != null) {
                    group.id
                } else {
                    repository.createGroup(rule.targetFolderName, "Smart Folder created by rule '${rule.ruleName}'").toInt()
                }

                // Add item to group
                repository.addItemToGroup(groupId, insertedId)

                // Apply rule tags if present
                if (rule.tagsToApply.isNotBlank()) {
                    val currentTagSet = updatedItem.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
                    rule.tagsToApply.split(",").forEach {
                        val t = it.trim()
                        if (t.isNotEmpty()) currentTagSet.add(if (t.startsWith("#")) t else "#$t")
                    }
                    updatedItem = updatedItem.copy(tags = currentTagSet.joinToString(", "))
                }
            }
        }

        // Save updated tags
        repository.updateItem(updatedItem)
    }

    fun addMockItem() {
        val mocks = listOf(
            "https://github.com/android/architecture-samples" to "Browser",
            "sk-proj-49102930192039102" to "IDE",
            "SELECT * FROM users WHERE active = 1;" to "Database Tool",
            "Check out the new Compose guidelines at developer.android.com" to "Slack",
            "user@company.com" to "Email Client",
            "Added 2 items to your Amazon cart! Total amount: $89.99 USD. Click to checkout now." to "Chrome"
        )
        val selected = mocks.random()
        viewModelScope.launch {
            val category = ClipboardClassifier.classify(selected.first)
            val preview = ClipboardClassifier.getPreview(selected.first)
            val entity = ClipboardItemEntity(
                text = selected.first,
                category = category,
                preview = preview,
                sourceApp = selected.second,
                wordCount = selected.first.split(Regex("\\s+")).filter { it.isNotBlank() }.size,
                charCount = selected.first.length
            )
            val insertedId = repository.insertItem(entity)
            processSmartRulesAndAutoTags(insertedId.toInt(), entity)
        }
    }

    fun addManualItem(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val category = ClipboardClassifier.classify(text)
            val preview = ClipboardClassifier.getPreview(text)
            val wordCount = text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            val entity = ClipboardItemEntity(
                text = text,
                category = category,
                preview = preview,
                wordCount = wordCount,
                charCount = text.length,
                sourceApp = "Manual Input"
            )
            val insertedId = repository.insertItem(entity)
            processSmartRulesAndAutoTags(insertedId.toInt(), entity)
        }
    }

    fun summarizeItem(item: ClipboardItemEntity, prompt: String = "") {
        viewModelScope.launch {
            _aiResult.value = "Processing with AI..."
            val result = if (prompt.isNotBlank()) {
                aiRepository.chatWithContext(prompt, item.text)
            } else {
                aiRepository.summarize(item.text)
            }
            _aiResult.value = result.getOrNull() ?: "Failed to process item."
        }
    }

    fun clearAiResult() {
        _aiResult.value = null
    }

    fun deleteItem(item: ClipboardItemEntity) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun togglePin(item: ClipboardItemEntity) {
        viewModelScope.launch {
            repository.updateItem(item.copy(isPinned = !item.isPinned))
        }
    }

    fun exportBackup(contentResolver: android.content.ContentResolver, uri: android.net.Uri, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            try {
                val itemsList = repository.getAllItems().first()
                val jsonString = json.encodeToString(itemsList)
                
                if (uri.scheme == "file") {
                    val file = File(uri.path ?: throw Exception("Invalid file path"))
                    file.outputStream().use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(jsonString)
                        }
                    }
                } else {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(jsonString)
                        }
                    } ?: throw Exception("Failed to open output stream")
                }
                onSuccess()
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    fun importBackup(contentResolver: android.content.ContentResolver, uri: android.net.Uri, onSuccess: (count: Int) -> Unit, onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonString = if (uri.scheme == "file") {
                    val file = File(uri.path ?: throw Exception("Invalid file path"))
                    file.readText()
                } else {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        InputStreamReader(inputStream).use { reader ->
                            reader.readText()
                        }
                    } ?: throw Exception("Failed to open input stream")
                }
                
                val itemsList = Json.decodeFromString<List<ClipboardItemEntity>>(jsonString)
                var importedCount = 0
                itemsList.forEach { item ->
                    repository.insertItem(item.copy(id = 0))
                    importedCount++
                }
                onSuccess(importedCount)
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    // --- Grouping states & operations ---
    val groups: StateFlow<List<ClipboardGroupEntity>> = repository.getAllGroups()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val crossRefs: StateFlow<List<GroupItemCrossRefEntity>> = repository.getAllCrossRefs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedGroupId = MutableStateFlow<Int?>(null)
    val selectedGroupId: StateFlow<Int?> = _selectedGroupId

    fun selectGroup(groupId: Int?) {
        _selectedGroupId.value = groupId
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val groupItems: StateFlow<List<ClipboardItemEntity>> = _selectedGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) {
                flowOf(emptyList())
            } else {
                repository.getItemsForGroup(groupId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createGroup(name: String, description: String = "") {
        viewModelScope.launch {
            repository.createGroup(name, description)
        }
    }

    fun deleteGroup(group: ClipboardGroupEntity) {
        viewModelScope.launch {
            if (_selectedGroupId.value == group.id) {
                _selectedGroupId.value = null
            }
            repository.deleteGroup(group)
        }
    }

    fun addSelectedToGroup(groupId: Int) {
        viewModelScope.launch {
            val selected = _selectedItemIds.value
            selected.forEach { itemId ->
                repository.addItemToGroup(groupId, itemId)
            }
            clearSelection()
        }
    }

    fun createGroupAndAddSelected(name: String, description: String = "") {
        viewModelScope.launch {
            val groupId = repository.createGroup(name, description)
            val selected = _selectedItemIds.value
            selected.forEach { itemId ->
                repository.addItemToGroup(groupId.toInt(), itemId)
            }
            clearSelection()
        }
    }

    fun addSingleItemToGroup(groupId: Int, itemId: Int) {
        viewModelScope.launch {
            repository.addItemToGroup(groupId, itemId)
        }
    }

    fun removeItemFromGroup(groupId: Int, itemId: Int) {
        viewModelScope.launch {
            repository.removeItemFromGroup(groupId, itemId)
        }
    }

    // --- RAG Chat states & operations ---
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(
            text = "Welcome to **Clipboard RAG Studio**! 🧠\n\nAsk me anything about your copied clipboards or specific groups. For example:\n- *'Summarize my travel notes'*\n- *'Find the api credentials'*\n- *'Generate an email reply from my clips'*",
            isUser = false
        )
    ))
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading

    fun clearChat() {
        _chatHistory.value = listOf(
            ChatMessage(
                text = "Chat cleared. Ask me anything about your clipboard content!",
                isUser = false
            )
        )
    }

    fun sendChatQuery(query: String, selectedGroupId: Int? = null) {
        if (query.isBlank()) return
        
        val userMsg = ChatMessage(text = query, isUser = true)
        _chatHistory.value = _chatHistory.value + userMsg
        _isChatLoading.value = true
        viewModelScope.launch {
            try {
                val relevantItems = if (selectedGroupId != null) {
                    repository.getItemsForGroup(selectedGroupId).first()
                } else {
                    repository.getAllItems().first()
                }
                
                if (relevantItems.isEmpty()) {
                    _chatHistory.value = _chatHistory.value + ChatMessage(
                        text = "Your clipboard history is empty. Please copy some content first!",
                        isUser = false
                    )
                    _isChatLoading.value = false
                    return@launch
                }
                
                val contextText = relevantItems.joinToString("\n---\n") { item ->
                    "Category: ${item.category}\nDate: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(item.timestamp))}\nContent: ${item.text}"
                }
                
                val result = aiRepository.chatWithContext(query, contextText)
                val replyText = result.getOrNull() ?: "Sorry, I couldn't process your request. Please check your Gemini API key configuration."
                _chatHistory.value = _chatHistory.value + ChatMessage(text = replyText, isUser = false)
            } catch (e: Exception) {
                _chatHistory.value = _chatHistory.value + ChatMessage(text = "Error: ${e.localizedMessage ?: "Unknown error occurred"}", isUser = false)
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    // --- Form Analysis & Smart Paste states & operations ---
    private val _formInputText = MutableStateFlow("")
    val formInputText: StateFlow<String> = _formInputText

    private val _formStructure = MutableStateFlow<FormStructure?>(null)
    val formStructure: StateFlow<FormStructure?> = _formStructure

    private val _isFormAnalyzing = MutableStateFlow(false)
    val isFormAnalyzing: StateFlow<Boolean> = _isFormAnalyzing

    private val _fieldMatches = MutableStateFlow<Map<String, ClipboardItemEntity?>>(emptyMap())
    val fieldMatches: StateFlow<Map<String, ClipboardItemEntity?>> = _fieldMatches

    fun updateFormInputText(text: String) {
        _formInputText.value = text
    }

    fun analyzeFormText(text: String) {
        val inputText = text.ifBlank { _formInputText.value }
        if (inputText.isBlank()) return

        _isFormAnalyzing.value = true
        viewModelScope.launch {
            try {
                val result = aiRepository.analyzeForm(inputText)
                val structure = result.getOrNull() ?: FormStructure("Detected Form", listOf(FormField("f_0", "Input Field", "Text")))
                _formStructure.value = structure
                autoMatchFields(structure.fields)
            } catch (e: Exception) {
                val fallback = FormStructure("Detected Form", listOf(FormField("f_0", "Input Field", "Text")))
                _formStructure.value = fallback
                autoMatchFields(fallback.fields)
            } finally {
                _isFormAnalyzing.value = false
            }
        }
    }

    fun autoMatchFields(fields: List<FormField>) {
        val allItems = items.value
        val matches = mutableMapOf<String, ClipboardItemEntity?>()

        for (field in fields) {
            val bestMatch = findBestMatchForField(field, allItems)
            matches[field.fieldId] = bestMatch
        }
        _fieldMatches.value = matches
    }

    fun setFieldMatch(fieldId: String, item: ClipboardItemEntity?) {
        _fieldMatches.value = _fieldMatches.value.toMutableMap().apply {
            put(fieldId, item)
        }
    }

    fun getSuggestedItemsForField(field: FormField): List<ClipboardItemEntity> {
        val allItems = items.value
        val fieldCat: String = field.category
        val fieldLabel: String = field.label

        return allItems.sortedByDescending { item ->
            var score = 0
            if (item.category.equals(fieldCat, ignoreCase = true)) score += 100
            if (fieldCat == "API Key" && (item.category == "Credentials" || item.category == "Sensitive")) score += 80
            if (fieldCat == "Email" && item.text.contains("@")) score += 90
            if (fieldCat == "Phone" && item.text.any { it.isDigit() }) score += 60
            if (fieldCat == "URL" && (item.text.startsWith("http://") || item.text.startsWith("https://"))) score += 90
            if (item.tags.contains(fieldCat, ignoreCase = true) || item.tags.contains(fieldLabel, ignoreCase = true)) score += 50
            if (item.text.contains(fieldLabel, ignoreCase = true)) score += 40
            score
        }
    }

    private fun findBestMatchForField(field: FormField, allItems: List<ClipboardItemEntity>): ClipboardItemEntity? {
        if (allItems.isEmpty()) return null
        val candidates = getSuggestedItemsForField(field)
        return candidates.firstOrNull()
    }

    fun getAutoSuggestOptions(
        currentInput: String,
        systemClipboardText: String?
    ): List<AutoSuggestOption> {
        val suggestions = mutableListOf<AutoSuggestOption>()
        val allItems = items.value

        // 1. System Clipboard Option
        if (!systemClipboardText.isNullOrBlank() && systemClipboardText != currentInput) {
            val snippet = if (systemClipboardText.length > 32) systemClipboardText.take(32) + "..." else systemClipboardText
            suggestions.add(
                AutoSuggestOption(
                    id = "sys_clip",
                    title = "Paste System Clipboard",
                    subtitle = "“$snippet”",
                    textToPaste = systemClipboardText,
                    type = SuggestionType.SYSTEM_CLIPBOARD,
                    badgeLabel = "📋 System"
                )
            )
        }

        // 2. OTP Code Detection in recent clips / system clip
        val otpRegex = Regex("\\b\\d{4,8}\\b")
        val recentTexts = listOfNotNull(systemClipboardText) + allItems.take(15).map { it.text }
        val detectedOtp = recentTexts.firstNotNullOfOrNull { text ->
            if (text.contains("code", ignoreCase = true) || text.contains("otp", ignoreCase = true) || text.contains("verification", ignoreCase = true) || text.length in 4..8) {
                otpRegex.find(text)?.value
            } else null
        }
        if (detectedOtp != null && detectedOtp != currentInput) {
            suggestions.add(
                AutoSuggestOption(
                    id = "otp_$detectedOtp",
                    title = "Paste OTP Code ($detectedOtp)",
                    subtitle = "Quick Verification Code",
                    textToPaste = detectedOtp,
                    type = SuggestionType.OTP_CODE,
                    badgeLabel = "⚡ OTP"
                )
            )
        }

        // 3. Relevant URL Links
        val urlRegex = Regex("https?://[^\\s]+")
        val detectedUrl = recentTexts.firstNotNullOfOrNull { text -> urlRegex.find(text)?.value }
        if (detectedUrl != null && detectedUrl != currentInput) {
            val shortUrl = if (detectedUrl.length > 35) detectedUrl.take(35) + "..." else detectedUrl
            suggestions.add(
                AutoSuggestOption(
                    id = "url_$detectedUrl",
                    title = "Paste Link",
                    subtitle = shortUrl,
                    textToPaste = detectedUrl,
                    type = SuggestionType.URL_LINK,
                    badgeLabel = "🔗 Link"
                )
            )
        }

        // 4. Matching Clips based on currentInput typing
        if (currentInput.isNotBlank()) {
            val matches = allItems.filter { item ->
                item.text.contains(currentInput, ignoreCase = true) && item.text != currentInput
            }.take(4)

            matches.forEach { match ->
                val prev = if (match.text.length > 32) match.text.take(32) + "..." else match.text
                suggestions.add(
                    AutoSuggestOption(
                        id = "match_${match.id}",
                        title = "Paste Match: ${match.category}",
                        subtitle = prev,
                        textToPaste = match.text,
                        type = SuggestionType.RELEVANT_MATCH,
                        badgeLabel = "💡 ${match.category}"
                    )
                )
            }
        } else {
            // Top pinned / recent snippets
            allItems.filter { it.isPinned || it.isFavorite }.take(3).forEach { item ->
                val prev = if (item.text.length > 32) item.text.take(32) + "..." else item.text
                suggestions.add(
                    AutoSuggestOption(
                        id = "fav_${item.id}",
                        title = "Paste Saved Snippet",
                        subtitle = prev,
                        textToPaste = item.text,
                        type = SuggestionType.SNIPPET,
                        badgeLabel = if (item.isPinned) "📌 Pinned" else "⭐ Favorite"
                    )
                )
            }
        }

        return suggestions.distinctBy { it.textToPaste }
    }

    fun copyFilledFormToClipboard(context: Context, clipboardManager: androidx.compose.ui.platform.ClipboardManager) {
        val structure = _formStructure.value ?: return
        val matches = _fieldMatches.value

        val builder = StringBuilder()
        builder.appendLine("=== ${structure.formTitle} ===")

        for (field in structure.fields) {
            val matchedItem = matches[field.fieldId]
            val value = matchedItem?.text ?: field.placeholder
            builder.appendLine("${field.label}: $value")
        }

        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(builder.toString().trim()))
        Toast.makeText(context, "Filled Form copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun provideFactory(repository: ClipboardRepository, aiRepository: AiRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    return ClipboardViewModel(repository, aiRepository) as T
                }
            }
    }
}
