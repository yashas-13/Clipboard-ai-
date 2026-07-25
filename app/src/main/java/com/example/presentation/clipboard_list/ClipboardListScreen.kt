package com.example.presentation.clipboard_list

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ClipboardItemEntity
import com.example.presentation.dashboard.UsageDashboardScreen
import com.example.presentation.dialogs.OfflineAiSummaryDialog
import com.example.presentation.dialogs.SmartRulesDialog
import com.example.presentation.form_assistant.FormAssistantScreen
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleContainer
import com.example.ui.theme.Slate400
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardListScreen(
    viewModel: ClipboardViewModel,
    isDarkTheme: Boolean = false,
    onThemeToggle: () -> Unit = {}
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val aiResult by viewModel.aiResult.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsStateWithLifecycle()
    val availableTags by viewModel.availableTags.collectAsStateWithLifecycle()
    val isPrivacyModeEnabled by viewModel.isPrivacyModeEnabled.collectAsStateWithLifecycle()
    val selectedItemIds by viewModel.selectedItemIds.collectAsStateWithLifecycle()

    val smartRules by viewModel.smartRules.collectAsStateWithLifecycle()
    val activeSummaryItem by viewModel.activeSummaryItem.collectAsStateWithLifecycle()
    val activeSummaryResult by viewModel.activeSummaryResult.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableStateOf(0) }
    var inputText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    var systemClipText by remember { mutableStateOf<String?>(null) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                systemClipText = try {
                    clipboardManager.getText()?.text
                } catch (e: Throwable) {
                    null
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showCategorizeDialog by remember { mutableStateOf(false) }
    var showAddToGroupDialog by remember { mutableStateOf(false) }
    var showSmartRulesDialog by remember { mutableStateOf(false) }
    var isExportingSelected by remember { mutableStateOf(false) }

    // Offline AI Summary Dialog
    if (activeSummaryItem != null && activeSummaryResult != null) {
        OfflineAiSummaryDialog(
            item = activeSummaryItem!!,
            summary = activeSummaryResult!!,
            onDismiss = { viewModel.dismissOfflineSummary() },
            onSaveAsClip = { summaryText ->
                viewModel.addManualItem(summaryText)
            }
        )
    }

    // Smart Folder Rules Dialog
    if (showSmartRulesDialog) {
        SmartRulesDialog(
            rules = smartRules,
            onDismiss = { showSmartRulesDialog = false },
            onCreateRule = { rName, targetFolder, app, kw, cat, tags ->
                viewModel.createSmartRule(rName, targetFolder, app, kw, cat, tags)
            },
            onToggleRule = { rule -> viewModel.toggleSmartRule(rule) },
            onDeleteRule = { rule -> viewModel.deleteSmartRule(rule) }
        )
    }
    
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportBackup(context.contentResolver, it,
                onSuccess = { Toast.makeText(context, "Export successful!", Toast.LENGTH_SHORT).show() },
                onError = { t -> Toast.makeText(context, "Export failed: ${t.localizedMessage}", Toast.LENGTH_LONG).show() }
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importBackup(context.contentResolver, it,
                onSuccess = { count -> Toast.makeText(context, "Imported $count items!", Toast.LENGTH_SHORT).show() },
                onError = { t -> Toast.makeText(context, "Import failed: ${t.localizedMessage}", Toast.LENGTH_LONG).show() }
            )
        }
    }

    // AI Result Dialog
    if (aiResult != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearAiResult() },
            title = { Text("AI Insight", fontWeight = FontWeight.Bold) },
            text = { Text(text = aiResult ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAiResult() }) {
                    Text("OK")
                }
            }
        )
    }

    // AI API Key Setup Dialog
    if (showApiKeyDialog) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        var apiKeyInput by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
        
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("AI API Key Setup", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter your custom Gemini API Key (BYOK) to unlock advanced AI capabilities.", fontSize = 13.sp, color = Slate400)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Gemini API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putString("api_key", apiKeyInput.trim()).apply()
                    Toast.makeText(context, "API Key saved successfully!", Toast.LENGTH_SHORT).show()
                    showApiKeyDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (selectedTab == 0) {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    if (selectedItemIds.isNotEmpty()) {
                        TopAppBar(
                            title = { Text("${selectedItemIds.size} selected", fontWeight = FontWeight.SemiBold) },
                            navigationIcon = {
                                IconButton(onClick = { viewModel.clearSelection() }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear selection")
                                }
                            },
                            actions = {
                                IconButton(onClick = { viewModel.deleteSelected() }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete selected")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    } else {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(PrimaryPurpleContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = null,
                                            tint = PrimaryPurple,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Clipboard AI",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = onThemeToggle) {
                                    Icon(
                                        imageVector = if (isDarkTheme) Icons.Filled.Brightness7 else Icons.Filled.Brightness4,
                                        contentDescription = "Toggle Theme",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = "More options",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("AI API Key Setup") },
                                        leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null, tint = PrimaryPurple) },
                                        onClick = {
                                            showMenu = false
                                            showApiKeyDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (isPrivacyModeEnabled) "Disable Privacy Mask" else "Enable Privacy Mask") },
                                        leadingIcon = {
                                            Icon(
                                                if (isPrivacyModeEnabled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null,
                                                tint = PrimaryPurple
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            viewModel.togglePrivacyMode()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export Backup") },
                                        leadingIcon = { Icon(Icons.Default.Backup, contentDescription = null, tint = PrimaryPurple) },
                                        onClick = {
                                            showMenu = false
                                            exportLauncher.launch("clipboard_backup.json")
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Import Backup") },
                                        leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null, tint = PrimaryPurple) },
                                        onClick = {
                                            showMenu = false
                                            importLauncher.launch(arrayOf("application/json"))
                                        }
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    // Search Bar & Local Keyword/Tag Filtering
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        placeholder = { Text("Search content keyword or #tags...", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = "Search", tint = PrimaryPurple)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank() || selectedTagFilter != null) {
                                IconButton(onClick = {
                                    viewModel.updateSearchQuery("")
                                    viewModel.selectTagFilter(null)
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear Search", tint = Slate400)
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = PrimaryPurple,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )

                    // Filter Chips & Smart Tag Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            item {
                                FilterChip(
                                    selected = searchQuery.isBlank() && selectedTagFilter == null,
                                    label = { Text("All") },
                                    onClick = {
                                        viewModel.updateSearchQuery("")
                                        viewModel.selectTagFilter(null)
                                    }
                                )
                            }

                            items(availableTags) { (tag, count) ->
                                val isSelected = selectedTagFilter.equals(tag, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    label = { Text("$tag ($count)") },
                                    onClick = { viewModel.selectTagFilter(tag) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryPurpleContainer,
                                        selectedLabelColor = PrimaryPurple
                                    )
                                )
                            }
                        }

                        AssistChip(
                            onClick = { showSmartRulesDialog = true },
                            label = { Text("Rules", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = PrimaryPurpleContainer.copy(alpha = 0.5f))
                        )
                    }
                }
            } else {
                val titleText = when (selectedTab) {
                    1 -> "Form Assistant & Smart Paste"
                    2 -> "RAG Studio"
                    else -> "Usage Analytics"
                }
                TopAppBar(
                    title = { Text(titleText, fontWeight = FontWeight.SemiBold) },
                    actions = {
                        IconButton(onClick = onThemeToggle) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Filled.Brightness7 else Icons.Filled.Brightness4,
                                contentDescription = "Toggle Theme",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { viewModel.addMockItem() },
                    containerColor = PrimaryPurple,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Mock Item")
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "History"
                        )
                    },
                    label = { Text("History") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = PrimaryPurple,
                        indicatorColor = PrimaryPurple
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.AssignmentTurnedIn,
                            contentDescription = "Form Paste"
                        )
                    },
                    label = { Text("Form Paste") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = PrimaryPurple,
                        indicatorColor = PrimaryPurple
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "RAG Studio"
                        )
                    },
                    label = { Text("RAG Studio") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = PrimaryPurple,
                        indicatorColor = PrimaryPurple
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 3) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                            contentDescription = "Analytics"
                        )
                    },
                    label = { Text("Analytics") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = PrimaryPurple,
                        indicatorColor = PrimaryPurple
                    )
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        val autoSuggestOptions = remember(inputText, systemClipText, items) {
                            viewModel.getAutoSuggestOptions(inputText, systemClipText)
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Quick Paste & Auto-Suggest",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (autoSuggestOptions.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = PrimaryPurpleContainer.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                text = "${autoSuggestOptions.size} Auto Suggestions",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryPurple,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = inputText,
                                        onValueChange = { inputText = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("Type or paste here...", fontSize = 14.sp) },
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                            focusedBorderColor = PrimaryPurple,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                            focusedContainerColor = MaterialTheme.colorScheme.background
                                        )
                                    )
                                    IconButton(
                                        onClick = {
                                            val textToSave = inputText.ifBlank {
                                                clipboardManager.getText()?.text ?: ""
                                            }
                                            if (textToSave.isNotBlank()) {
                                                viewModel.addManualItem(textToSave)
                                                inputText = ""
                                                Toast.makeText(context, "Saved to Clipboard History!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(PrimaryPurple, RoundedCornerShape(12.dp))
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = "Fetch", tint = Color.White)
                                    }
                                }

                                // Auto-Suggest Paste Options Bar
                                if (autoSuggestOptions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Smart Auto-Paste Suggestions:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate400
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(autoSuggestOptions, key = { it.id }) { suggestion ->
                                            SuggestionChip(
                                                onClick = {
                                                    inputText = suggestion.textToPaste
                                                    Toast.makeText(context, "Filled '${suggestion.title}'", Toast.LENGTH_SHORT).show()
                                                },
                                                label = {
                                                    Column {
                                                        Text(
                                                            text = suggestion.title,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = suggestion.subtitle,
                                                            fontSize = 10.sp,
                                                            color = Slate400,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                },
                                                icon = {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = PrimaryPurpleContainer,
                                                        modifier = Modifier.padding(end = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = suggestion.badgeLabel,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = PrimaryPurple,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = MaterialTheme.colorScheme.background
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (items.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                Text("Clipboard is empty", style = MaterialTheme.typography.bodyLarge, color = Slate400)
                            }
                        }
                    } else {
                        items(items, key = { it.id }) { item ->
                            val isSelected = selectedItemIds.contains(item.id)
                            SwipeRevealBox(
                                modifier = Modifier.animateItem(),
                                backgroundContent = {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(item.text))
                                                    Toast.makeText(context, "Copied as Plain Text", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                            ) {
                                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.updateFormInputText(item.text)
                                                    viewModel.analyzeFormText(item.text)
                                                    selectedTab = 1
                                                },
                                                modifier = Modifier.background(PrimaryPurpleContainer, CircleShape)
                                            ) {
                                                Icon(Icons.Filled.AssignmentTurnedIn, contentDescription = "Analyze as Form", tint = PrimaryPurple)
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            IconButton(
                                                onClick = {
                                                    val shareIntent = android.content.Intent().apply {
                                                        action = android.content.Intent.ACTION_SEND
                                                        putExtra(android.content.Intent.EXTRA_TEXT, item.text)
                                                        type = "text/plain"
                                                    }
                                                    context.startActivity(android.content.Intent.createChooser(shareIntent, null))
                                                },
                                                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                            ) {
                                                Icon(Icons.Filled.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteItem(item) },
                                                modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                                            ) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
                                            }
                                        }
                                    }
                                }
                            ) {
                                ClipboardItemCard(
                                    item = item,
                                    isSelected = isSelected,
                                    isSelectionMode = selectedItemIds.isNotEmpty(),
                                    isPrivacyModeEnabled = isPrivacyModeEnabled,
                                    onSelectToggle = { viewModel.toggleSelection(item.id) },
                                    onDelete = { viewModel.deleteItem(item) },
                                    onSummarize = { prompt -> viewModel.summarizeItem(item, prompt) },
                                    onOfflineSummarize = { viewModel.requestOfflineSummary(item) },
                                    onTogglePin = { viewModel.togglePin(item) }
                                )
                            }
                        }
                    }
                }
            }
            1 -> {
                FormAssistantScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding)
                )
            }
            2 -> {
                RagStudioTab(
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding)
                )
            }
            3 -> {
                UsageDashboardScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
fun RagStudioTab(
    viewModel: ClipboardViewModel,
    modifier: Modifier = Modifier
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val selectedGroupId by viewModel.selectedGroupId.collectAsStateWithLifecycle()
    val groupItems by viewModel.groupItems.collectAsStateWithLifecycle()
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()

    var chatInputText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Group Selector Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Select Knowledge Context:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = PrimaryPurple
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedGroupId == null,
                        onClick = { viewModel.selectGroup(null) },
                        label = { Text("All Clipboards") }
                    )
                    groups.forEach { group ->
                        FilterChip(
                            selected = selectedGroupId == group.id,
                            onClick = { viewModel.selectGroup(group.id) },
                            label = { Text(group.name) }
                        )
                    }
                }
            }
        }

        // Chat History List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chatHistory) { msg ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (msg.isUser) PrimaryPurple else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = if (msg.isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Chat Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { viewModel.clearChat() }
            ) {
                Icon(Icons.Filled.Restore, contentDescription = "Clear Chat")
            }

            OutlinedTextField(
                value = chatInputText,
                onValueChange = { chatInputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask Clipboard AI...") },
                shape = RoundedCornerShape(20.dp),
                singleLine = true
            )

            IconButton(
                onClick = {
                    if (chatInputText.isNotBlank()) {
                        viewModel.sendChatQuery(chatInputText, selectedGroupId)
                        chatInputText = ""
                    }
                },
                enabled = !isChatLoading
            ) {
                if (isChatLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = PrimaryPurple)
                }
            }
        }
    }
}

private data class CardThemeColors(
    val cardBg: Color,
    val textColor: Color,
    val borderColor: Color,
    val iconBg: Color,
    val iconColor: Color,
    val labelColor: Color
)

@Composable
fun ClipboardItemCard(
    item: ClipboardItemEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isPrivacyModeEnabled: Boolean,
    onSelectToggle: () -> Unit,
    onDelete: () -> Unit,
    onSummarize: (String) -> Unit,
    onOfflineSummarize: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    
    val (cardBg, textColor, borderColor, iconBg, iconColor, labelColor) = when (item.category) {
        "Code" -> if (isDark) CardThemeColors(Color(0xFF1E293B), Color(0xFF38BDF8), Color(0xFF334155), Color(0xFF0F172A), Color(0xFF38BDF8), Color(0xFF64748B))
                  else CardThemeColors(Color(0xFFF1F5F9), Color(0xFF0369A1), Color(0xFFCBD5E1), Color(0xFFE2E8F0), Color(0xFF0369A1), Color(0xFF64748B))
        "API Key" -> if (isDark) CardThemeColors(Color(0xFF3B0764), Color(0xFFD8B4FE), Color(0xFF581C87), Color(0xFF2E1065), Color(0xFFD8B4FE), Color(0xFFA855F7))
                    else CardThemeColors(Color(0xFFFAF5FF), Color(0xFF7E22CE), Color(0xFFE9D5FF), Color(0xFFF3E8FF), Color(0xFF7E22CE), Color(0xFFA855F7))
        "Credentials" -> if (isDark) CardThemeColors(Color(0xFF4C1D95), Color(0xFFC4B5FD), Color(0xFF5B21B6), Color(0xFF2E1065), Color(0xFFC4B5FD), Color(0xFF8B5CF6))
                        else CardThemeColors(Color(0xFFF5F3FF), Color(0xFF6D28D9), Color(0xFFDDD6FE), Color(0xFFEDE9FE), Color(0xFF6D28D9), Color(0xFF8B5CF6))
        "Sensitive" -> if (isDark) CardThemeColors(Color(0xFF450A0A), Color(0xFFFCA5A5), Color(0xFF7F1D1D), Color(0xFF2C0000), Color(0xFFFCA5A5), Color(0xFFEF4444))
                       else CardThemeColors(Color(0xFFFEF2F2), Color(0xFFB91C1C), Color(0xFFFECACA), Color(0xFFFEE2E2), Color(0xFFB91C1C), Color(0xFFEF4444))
        "URL" -> if (isDark) CardThemeColors(Color(0xFF064E3B), Color(0xFF6EE7B7), Color(0xFF065F46), Color(0xFF022C22), Color(0xFF6EE7B7), Color(0xFF10B981))
                 else CardThemeColors(Color(0xFFECFDF5), Color(0xFF047857), Color(0xFFA7F3D0), Color(0xFFD1FAE5), Color(0xFF047857), Color(0xFF10B981))
        "Email" -> if (isDark) CardThemeColors(Color(0xFF1E3A8A), Color(0xFF93C5FD), Color(0xFF1E40AF), Color(0xFF172554), Color(0xFF93C5FD), Color(0xFF3B82F6))
                   else CardThemeColors(Color(0xFFEFF6FF), Color(0xFF1D4ED8), Color(0xFFBFDBFE), Color(0xFFDBEAFE), Color(0xFF1D4ED8), Color(0xFF3B82F6))
        else -> if (isDark) CardThemeColors(Color(0xFF18181B), Color(0xFFE4E4E7), Color(0xFF27272A), Color(0xFF27272A), Color(0xFFA1A1AA), Color(0xFF71717A))
                else CardThemeColors(Color(0xFFFFFFFF), Color(0xFF18181B), Color(0xFFE4E4E7), Color(0xFFF4F4F5), Color(0xFF71717A), Color(0xFFA1A1AA))
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (isSelectionMode) onSelectToggle()
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onSelectToggle() }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = item.category.uppercase(Locale.getDefault()),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = labelColor
                    )
                }

                if (!isSelectionMode) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isPinned) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Pin item",
                            tint = if (item.isPinned) Color.Red else labelColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val textToShow = if (isPrivacyModeEnabled && (item.category == "API Key" || item.category == "Password" || item.category == "Credentials" || item.category == "Sensitive")) {
                "••••••••••••••••"
            } else {
                item.preview
            }

            Text(
                text = textToShow,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = textColor,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action bar & AI Summary Quick Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = onOfflineSummarize,
                        label = {
                            Text(
                                text = if (item.text.length > 60) "✨ AI Summary" else "⚡ Summarize",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryPurple
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Summary",
                                tint = PrimaryPurple,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = PrimaryPurpleContainer.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (item.sourceApp.isNotBlank() && item.sourceApp != "Unknown") {
                        Text(
                            text = "• ${item.sourceApp}",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.tags.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryPurpleContainer.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = item.tags,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryPurple,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(item.timestamp)),
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }
            }
        }
    }
}
