with open('app/src/main/java/com/example/presentation/clipboard_list/ClipboardViewModel.kt', 'r') as f:
    content = f.read()

# Add Form imports
if 'import com.example.domain.repository.FormField' not in content:
    content = content.replace('import com.example.domain.repository.ClipboardAnalysis', 'import com.example.domain.repository.ClipboardAnalysis\nimport com.example.domain.repository.FormField\nimport com.example.domain.repository.FormStructure')

viewmodel_additions = '''
    // --- Form Analysis & Smart Paste states ---
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
        val category = field.category
        val label = field.label

        return allItems.sortedByDescending { item ->
            var score = 0
            if (item.category.equals(category, ignoreCase = true)) score += 100
            if (category == "API Key" && (item.category == "Credentials" || item.category == "Sensitive")) score += 80
            if (category == "Email" && item.text.contains("@")) score += 90
            if (category == "Phone" && item.text.any { it.isDigit() }) score += 60
            if (category == "URL" && (item.text.startsWith("http://") || item.text.startsWith("https://"))) score += 90
            if (item.tags.any { tag -> tag.contains(category, ignoreCase = true) || tag.contains(label, ignoreCase = true) }) score += 50
            if (item.text.contains(label, ignoreCase = true)) score += 40
            score
        }
    }

    private fun findBestMatchForField(field: FormField, allItems: List<ClipboardItemEntity>): ClipboardItemEntity? {
        if (allItems.isEmpty()) return null
        val candidates = getSuggestedItemsForField(field)
        return candidates.firstOrNull()
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
'''

# Insert before companion object
if 'companion object {' in content:
    content = content.replace('companion object {', viewmodel_additions + '\n    companion object {')

with open('app/src/main/java/com/example/presentation/clipboard_list/ClipboardViewModel.kt', 'w') as f:
    f.write(content)
