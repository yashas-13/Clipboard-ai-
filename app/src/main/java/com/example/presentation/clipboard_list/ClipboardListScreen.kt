package com.example.presentation.clipboard_list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import com.example.presentation.dashboard.UsageDashboardScreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ClipboardItemEntity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

import androidx.compose.ui.platform.LocalClipboardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ClipboardListScreen(
    viewModel: ClipboardViewModel,
    isDarkTheme: Boolean = false,
    onThemeToggle: () -> Unit = {}
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val aiResult by viewModel.aiResult.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isPrivacyModeEnabled by viewModel.isPrivacyModeEnabled.collectAsStateWithLifecycle()
    val selectedItemIds by viewModel.selectedItemIds.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableStateOf(0) }
    var inputText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    var showMenu by remember { mutableStateOf(false) }
    var showCategorizeDialog by remember { mutableStateOf(false) }
    var isExportingSelected by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            if (uri != null) {
                if (isExportingSelected) {
                    viewModel.exportSelected(
                        contentResolver = contentResolver,
                        uri = uri,
                        onSuccess = {
                            Toast.makeText(context, "Selected items exported successfully", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            Toast.makeText(context, "Failed to export selected: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    viewModel.exportBackup(
                        contentResolver = contentResolver,
                        uri = uri,
                        onSuccess = {
                            Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            Toast.makeText(context, "Failed to export: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.importBackup(
                    contentResolver = contentResolver,
                    uri = uri,
                    onSuccess = { count ->
                        Toast.makeText(context, "Successfully imported $count items", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        Toast.makeText(context, "Failed to import: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    )

    if (showCategorizeDialog) {
        val categoriesList = listOf(
            "General", "Sensitive", "URL", "Email", "Phone", "OTP", "Code", "JSON", "Markdown", "Rich Text"
        )
        AlertDialog(
            onDismissRequest = { showCategorizeDialog = false },
            title = { Text("Categorize Selected Items") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text("Select a category to apply to all ${selectedItemIds.size} selected items:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    categoriesList.forEach { category ->
                        Button(
                            onClick = {
                                viewModel.categorizeSelected(category)
                                showCategorizeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(category)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCategorizeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (aiResult != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAiResult() },
            title = { Text("AI Summary") },
            text = { Text(aiResult ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAiResult() }) {
                    Text("OK")
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
                                IconButton(onClick = { viewModel.selectAll(items.map { it.id }) }) {
                                    Icon(Icons.Filled.Check, contentDescription = "Select All")
                                }
                                IconButton(onClick = { showCategorizeDialog = true }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Categorize Selected")
                                }
                                IconButton(onClick = {
                                    isExportingSelected = true
                                    exportLauncher.launch("selected_clipboard_items.json")
                                }) {
                                    Icon(Icons.Filled.Backup, contentDescription = "Export Selected")
                                }
                                IconButton(onClick = { viewModel.deleteSelected() }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    } else {
                        TopAppBar(
                            title = { Text("Clipboard AI", fontWeight = FontWeight.SemiBold) },
                            actions = {
                                IconButton(onClick = { viewModel.togglePrivacyMode() }) {
                                    Icon(
                                        imageVector = if (isPrivacyModeEnabled) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = "Toggle Privacy Masking",
                                        tint = if (isPrivacyModeEnabled) PrimaryPurple else MaterialTheme.colorScheme.onSurface
                                    )
                                }
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
                                            isExportingSelected = false
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
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        placeholder = { Text("Search clipboard...", fontSize = 14.sp) },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = PrimaryPurple,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                    // Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(selected = searchQuery.isBlank(), label = { Text("All") }, onClick = { viewModel.updateSearchQuery("") })
                        FilterChip(selected = searchQuery.equals("url", ignoreCase = true), label = { Text("Links") }, onClick = { viewModel.updateSearchQuery("url") })
                        FilterChip(selected = searchQuery.equals("code", ignoreCase = true), label = { Text("Code") }, onClick = { viewModel.updateSearchQuery("code") })
                        FilterChip(selected = searchQuery.equals("email", ignoreCase = true), label = { Text("Email") }, onClick = { viewModel.updateSearchQuery("email") })
                    }
                }
            } else {
                TopAppBar(
                    title = { Text("Usage Analytics", fontWeight = FontWeight.SemiBold) },
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
                                text = { Text("Export Backup") },
                                leadingIcon = { Icon(Icons.Default.Backup, contentDescription = null, tint = PrimaryPurple) },
                                onClick = {
                                    showMenu = false
                                    isExportingSelected = false
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
                            imageVector = if (selectedTab == 1) Icons.Filled.BarChart else Icons.Outlined.BarChart,
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
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Save to Clipboard AI", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
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
                                        placeholder = { Text("Paste here...", fontSize = 14.sp) },
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
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(PrimaryPurple, RoundedCornerShape(12.dp))
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = "Fetch", tint = Color.White)
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
                            ClipboardItemCard(
                                item = item,
                                isSelected = isSelected,
                                isSelectionMode = selectedItemIds.isNotEmpty(),
                                isPrivacyModeEnabled = isPrivacyModeEnabled,
                                onSelectToggle = { viewModel.toggleSelection(item.id) },
                                onDelete = { viewModel.deleteItem(item) },
                                onSummarize = { prompt -> viewModel.summarizeItem(item, prompt) },
                                onTogglePin = { viewModel.togglePin(item) }
                            )
                        }
                    }
                }
            }
            1 -> {
                UsageDashboardScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
fun FilterChip(selected: Boolean, text: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) PrimaryPurpleContainer else CardBackgroundLight)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else CardBorder,
                shape = CircleShape
            )
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (selected) OnPrimaryPurpleContainer else Slate600,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

data class CardThemeColors(
    val cardBg: Color,
    val textColor: Color,
    val borderColor: Color,
    val iconBg: Color,
    val iconColor: Color,
    val labelColor: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClipboardItemCard(
    item: ClipboardItemEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isPrivacyModeEnabled: Boolean,
    onSelectToggle: () -> Unit,
    onDelete: () -> Unit,
    onSummarize: (String) -> Unit,
    onTogglePin: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    
    val (cardBg, textColor, borderColor, iconBg, iconColor, labelColor) = when (item.category) {
        "Code" -> if (isDark) {
            CardThemeColors(Color(0xFF1E293B), Color(0xFF38BDF8), Color(0xFF334155), Color(0xFF0F172A), Color(0xFF38BDF8), Color(0xFF64748B))
        } else {
            CardThemeColors(Color(0xFFF1F5F9), Color(0xFF0369A1), Color(0xFFCBD5E1), Color(0xFFE2E8F0), Color(0xFF0369A1), Color(0xFF64748B))
        }
        "Sensitive" -> if (isDark) {
            CardThemeColors(Color(0xFF450A0A), Color(0xFFFCA5A5), Color(0xFF7F1D1D), Color(0xFF2C0000), Color(0xFFFCA5A5), Color(0xFFEF4444))
        } else {
            CardThemeColors(Color(0xFFFEF2F2), Color(0xFF991B1B), Color(0xFFFCA5A5), Color(0xFFFEE2E2), Color(0xFF991B1B), Color(0xFFEF4444))
        }
        "URL" -> if (isDark) {
            CardThemeColors(Color(0xFF172554), Color(0xFF93C5FD), Color(0xFF1E3A8A), Color(0xFF0F172A), Color(0xFF93C5FD), Color(0xFF3B82F6))
        } else {
            CardThemeColors(Color(0xFFEFF6FF), Color(0xFF1D4ED8), Color(0xFFBFDBFE), Color(0xFFDBEAFE), Color(0xFF1D4ED8), Color(0xFF3B82F6))
        }
        "Email" -> if (isDark) {
            CardThemeColors(Color(0xFF042F2E), Color(0xFF5EEAD4), Color(0xFF115E59), Color(0xFF0F172A), Color(0xFF5EEAD4), Color(0xFF0D9488))
        } else {
            CardThemeColors(Color(0xFFF0FDFA), Color(0xFF0F766E), Color(0xFF99F6E4), Color(0xFFCCFBF1), Color(0xFF0F766E), Color(0xFF0D9488))
        }
        "Phone" -> if (isDark) {
            CardThemeColors(Color(0xFF064E3B), Color(0xFF86EFAC), Color(0xFF166534), Color(0xFF0F172A), Color(0xFF86EFAC), Color(0xFF16A34A))
        } else {
            CardThemeColors(Color(0xFFF0FDF4), Color(0xFF15803D), Color(0xFFBBF7D0), Color(0xFFDCFCE7), Color(0xFF15803D), Color(0xFF16A34A))
        }
        "OTP" -> if (isDark) {
            CardThemeColors(Color(0xFF78350F), Color(0xFFFCD34D), Color(0xFF92400E), Color(0xFF0F172A), Color(0xFFFCD34D), Color(0xFFD97706))
        } else {
            CardThemeColors(Color(0xFFFFFBEB), Color(0xFFB45309), Color(0xFFFDE68A), Color(0xFFFEF3C7), Color(0xFFB45309), Color(0xFFD97706))
        }
        "JSON" -> if (isDark) {
            CardThemeColors(Color(0xFF4A044E), Color(0xFFF0ABFC), Color(0xFF701A75), Color(0xFF0F172A), Color(0xFFF0ABFC), Color(0xFFC084FC))
        } else {
            CardThemeColors(Color(0xFFFDF4FF), Color(0xFFA21CAF), Color(0xFFF5D0FE), Color(0xFFFAE8FF), Color(0xFFA21CAF), Color(0xFFC084FC))
        }
        "Markdown" -> if (isDark) {
            CardThemeColors(Color(0xFF1E1B4B), Color(0xFFC7D2FE), Color(0xFF312E81), Color(0xFF0F172A), Color(0xFFC7D2FE), Color(0xFF818CF8))
        } else {
            CardThemeColors(Color(0xFFEEF2FF), Color(0xFF4338CA), Color(0xFFC7D2FE), Color(0xFFE0E7FF), Color(0xFF4338CA), Color(0xFF818CF8))
        }
        "Rich Text" -> if (isDark) {
            CardThemeColors(Color(0xFF500724), Color(0xFFF9A8D4), Color(0xFF70083C), Color(0xFF0F172A), Color(0xFFF9A8D4), Color(0xFFF472B6))
        } else {
            CardThemeColors(Color(0xFFFDF2F8), Color(0xFFBE185D), Color(0xFFFBCFE8), Color(0xFFFCE7F3), Color(0xFFBE185D), Color(0xFFF472B6))
        }
        "Image" -> if (isDark) {
            CardThemeColors(Color(0xFF2E1065), Color(0xFFC084FC), Color(0xFF5B21B6), Color(0xFF0F172A), Color(0xFFC084FC), Color(0xFF8B5CF6))
        } else {
            CardThemeColors(Color(0xFFF5F3FF), Color(0xFF6D28D9), Color(0xFFDDD6FE), Color(0xFFEDE9FE), Color(0xFF6D28D9), Color(0xFF8B5CF6))
        }
        else -> { // Default Text / General
            CardThemeColors(
                MaterialTheme.colorScheme.surfaceContainer,
                MaterialTheme.colorScheme.onSurface,
                MaterialTheme.colorScheme.outlineVariant,
                if (isDark) Color(0xFF334155) else BlueBackground,
                if (isDark) Color(0xFFE2E2E2) else BlueAccent,
                Slate400
            )
        }
    }
    
    val isCode = item.category == "Code"
    val isSensitive = item.category == "Sensitive"
    
    var isRevealed by remember { mutableStateOf(false) }
    
    val isSensitiveCategory = item.category.lowercase() in setOf("sensitive", "password", "otp", "api key", "jwt", "credit card", "banking")
    val matchesSensitiveRegex = item.text.matches(Regex(".*[A-Za-z0-9_-]{32,}.*")) ||
                                item.text.contains("sk-") ||
                                item.text.contains("API_KEY") ||
                                item.text.startsWith("eyJ")
    val isSensitiveItem = isSensitiveCategory || matchesSensitiveRegex
    val isMasked = isPrivacyModeEnabled && isSensitiveItem && !isRevealed
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onSelectToggle()
                    }
                },
                onLongClick = {
                    if (!isSelectionMode) {
                        onSelectToggle()
                    }
                }
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else cardBg),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) PrimaryPurple else borderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onSelectToggle() },
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.category.take(1).uppercase(),
                                color = iconColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "${item.category.uppercase()} • ${item.sourceApp}",
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
                            contentDescription = "Pin",
                            tint = if (item.isPinned) PrimaryPurple else (if (isCode || isDark) Color.White.copy(alpha = 0.6f) else Slate400),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isMasked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF2C0000) else Color(0xFFFEF2F2))
                        .border(1.dp, if (isDark) Color(0xFF7F1D1D) else Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                        .clickable { isRevealed = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VisibilityOff,
                            contentDescription = "Hidden Sensitive Data",
                            tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Sensitive data masked • Tap to reveal",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                        )
                    }
                }
            } else {
                Text(
                    text = item.text,
                    fontSize = 14.sp,
                    color = textColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp,
                    fontFamily = if (isCode || isSensitive) androidx.compose.ui.text.font.FontFamily.Monospace else androidx.compose.ui.text.font.FontFamily.Default
                )
            }
            
            if (item.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                        val displayTag = if (tag.startsWith("#")) tag else "#$tag"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(iconBg.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = displayTag,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iconColor
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Metadata: word count, character count, time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.wordCount} words • ${item.charCount} chars",
                    fontSize = 11.sp,
                    color = labelColor.copy(alpha = 0.8f)
                )
                val dateStr = android.text.format.DateFormat.format("hh:mm a", item.timestamp).toString()
                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = labelColor.copy(alpha = 0.8f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.text.length > 50) {
                    ActionChip(text = "Summarize", onClick = { onSummarize("Please provide a brief summary of this text.") }, isDark = isCode || isDark)
                }
                ActionChip(text = "Delete", onClick = onDelete, isDark = isCode || isDark)
            }
            
            val suggestions = when(item.category) {
                "URL" -> listOf("Summarize URL", "Extract Keywords")
                "Code" -> listOf("Explain Code", "Fix Bugs")
                "Sensitive" -> listOf("Analyze Risk", "Redact Info")
                "Email" -> listOf("Write Reply", "Extract Domain")
                "Phone" -> listOf("Format Number", "Extract Country")
                "OTP" -> listOf("Extract Code", "Copy Code")
                "JSON" -> listOf("Format JSON", "Convert CSV")
                "Markdown" -> listOf("Render HTML", "Check Syntax")
                "Rich Text" -> listOf("Convert Plain", "Clean Tags")
                "Image" -> listOf("Extract Text (OCR)", "Describe Image")
                else -> listOf("Fix Grammar", "Rewrite")
            }
            
            if (suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("AI SUGGESTIONS", fontSize = 10.sp, color = labelColor, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        SuggestionChip(
                            onClick = { onSummarize(suggestion) },
                            label = { Text(suggestion, fontSize = 10.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = iconBg,
                                labelColor = iconColor
                            ),
                            border = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionChip(text: String, onClick: () -> Unit, isDark: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFF8FAFC))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White.copy(alpha = 0.7f) else Slate600
        )
    }
}
