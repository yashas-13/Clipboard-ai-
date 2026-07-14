package com.example.presentation.clipboard_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ClipboardItemEntity
import com.example.data.local.ClipboardClassifier
import com.example.domain.repository.ClipboardRepository
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.SharingStarted
import com.example.domain.repository.AiRepository
import com.example.domain.provider.AiProvider
import com.example.data.provider.GeminiProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.content.ContentResolver
import android.net.Uri
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.File

data class DailyCount(val dateLabel: String, val count: Int)
data class CategoryCount(val category: String, val count: Int)

data class DashboardStats(
    val totalItems: Int = 0,
    val totalWords: Int = 0,
    val totalChars: Int = 0,
    val averageLength: Int = 0,
    val categoryStats: List<CategoryCount> = emptyList(),
    val dailyStats: List<DailyCount> = emptyList()
)

class ClipboardViewModel(
    private val repository: ClipboardRepository,
    private val aiRepository: AiRepository,
    private val activeAiProvider: AiProvider = GeminiProvider()
) : ViewModel() {

    private val _isPrivacyModeEnabled = MutableStateFlow(true)
    val isPrivacyModeEnabled: StateFlow<Boolean> = _isPrivacyModeEnabled

    fun togglePrivacyMode() {
        _isPrivacyModeEnabled.value = !_isPrivacyModeEnabled.value
    }

    private val _selectedItemIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedItemIds: StateFlow<Set<Int>> = _selectedItemIds

    fun toggleSelection(id: Int) {
        val current = _selectedItemIds.value
        if (current.contains(id)) {
            _selectedItemIds.value = current - id
        } else {
            _selectedItemIds.value = current + id
        }
    }

    fun selectAll(ids: List<Int>) {
        _selectedItemIds.value = ids.toSet()
    }

    fun clearSelection() {
        _selectedItemIds.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val selected = _selectedItemIds.value
            val allItems = repository.getAllItems().first()
            allItems.filter { selected.contains(it.id) }.forEach {
                repository.deleteItem(it)
            }
            clearSelection()
        }
    }

    fun exportSelected(contentResolver: ContentResolver, uri: Uri, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            try {
                val selected = _selectedItemIds.value
                val allItems = repository.getAllItems().first()
                val selectedItems = allItems.filter { selected.contains(it.id) }
                val jsonString = Json { prettyPrint = true }.encodeToString(selectedItems)
                
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
                clearSelection()
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    fun categorizeSelected(category: String) {
        viewModelScope.launch {
            val selected = _selectedItemIds.value
            val allItems = repository.getAllItems().first()
            allItems.filter { selected.contains(it.id) }.forEach { item ->
                repository.updateItem(item.copy(category = category))
            }
            clearSelection()
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val items: StateFlow<List<ClipboardItemEntity>> = repository.getAllItems()
        .combine(_searchQuery) { items, query ->
            if (query.isBlank()) {
                items
            } else {
                items.filter { 
                    it.text.contains(query, ignoreCase = true) || 
                    it.category.contains(query, ignoreCase = true) ||
                    it.tags.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    val stats: StateFlow<DashboardStats> = repository.getAllItems()
        .map { list -> calculateDashboardStats(list) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardStats()
        )

    private fun calculateDashboardStats(list: List<ClipboardItemEntity>): DashboardStats {
        if (list.isEmpty()) return DashboardStats()
        
        val totalItems = list.size
        val totalWords = list.sumOf { it.wordCount }
        val totalChars = list.sumOf { it.charCount }
        val averageLength = if (totalItems > 0) totalChars / totalItems else 0
        
        // Category distribution
        val categoryCounts = list.groupBy { it.category }
            .map { CategoryCount(it.key, it.value.size) }
            .sortedByDescending { it.count }
            
        // Daily copy counts for the last 7 days (including empty days)
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayLabelFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        
        val last7DaysKeys = mutableListOf<String>()
        val last7DaysLabels = mutableListOf<String>()
        val dayMap = mutableMapOf<String, Int>()
        
        for (i in 6 downTo 0) {
            val tempCal = Calendar.getInstance()
            tempCal.add(Calendar.DAY_OF_YEAR, -i)
            val key = dayKeyFormat.format(tempCal.time)
            val label = dayLabelFormat.format(tempCal.time)
            last7DaysKeys.add(key)
            last7DaysLabels.add(label)
            dayMap[key] = 0
        }
        
        list.forEach { item ->
            val key = dayKeyFormat.format(Date(item.timestamp))
            if (dayMap.containsKey(key)) {
                dayMap[key] = dayMap.getValue(key) + 1
            }
        }
        
        val dailyStats = last7DaysKeys.mapIndexed { index, key ->
            DailyCount(last7DaysLabels[index], dayMap[key] ?: 0)
        }
        
        return DashboardStats(
            totalItems = totalItems,
            totalWords = totalWords,
            totalChars = totalChars,
            averageLength = averageLength,
            categoryStats = categoryCounts,
            dailyStats = dailyStats
        )
    }
        
    private val _aiResult = MutableStateFlow<String?>(null)
    val aiResult: StateFlow<String?> = _aiResult

    fun summarizeItem(item: ClipboardItemEntity, prompt: String = "Summarize") {
        viewModelScope.launch {
            _aiResult.value = "Thinking..."
            val result = activeAiProvider.generateResponse(prompt, item.text)
            _aiResult.value = result.getOrNull() ?: "Failed to process request"
        }
    }
    
    fun dismissAiResult() {
        _aiResult.value = null
    }

    fun addMockItem() {
        viewModelScope.launch {
            val types = listOf(
                Pair("Code", "const observer = new IntersectionObserver(cb, opt);\nconsole.log('Observer initialized');"),
                Pair("URL", "https://github.com/openai/whisper/discussions/124"),
                Pair("AI Prompt", "Review the attached code for potential memory leaks in the coroutine scopes...")
            )
            val selected = types.random()
            val preview = ClipboardClassifier.getPreview(selected.second)
            
            repository.insertItem(
                ClipboardItemEntity(
                    text = selected.second,
                    category = selected.first,
                    preview = preview,
                    wordCount = selected.second.split(" ").filter { it.isNotBlank() }.size,
                    charCount = selected.second.length
                )
            )
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
            
            // Perform background AI enrichment
            launch {
                val result = aiRepository.analyzeClipboardContent(text)
                result.onSuccess { analysis ->
                    val updatedEntity = entity.copy(
                        id = insertedId.toInt(),
                        category = analysis.category,
                        tags = analysis.tags.joinToString(", ")
                    )
                    repository.updateItem(updatedEntity)
                }.onFailure {
                    it.printStackTrace()
                }
            }
        }
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

    fun exportBackup(contentResolver: ContentResolver, uri: Uri, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            try {
                val items = repository.getAllItems().first()
                val jsonString = Json { prettyPrint = true }.encodeToString(items)
                
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

    fun importBackup(contentResolver: ContentResolver, uri: Uri, onSuccess: (count: Int) -> Unit, onError: (Throwable) -> Unit) {
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
                
                val items = Json.decodeFromString<List<ClipboardItemEntity>>(jsonString)
                
                var importedCount = 0
                items.forEach { item ->
                    repository.insertItem(item.copy(id = 0))
                    importedCount++
                }
                onSuccess(importedCount)
            } catch (t: Throwable) {
                onError(t)
            }
        }
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
