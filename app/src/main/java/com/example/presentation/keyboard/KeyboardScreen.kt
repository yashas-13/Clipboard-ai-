package com.example.presentation.keyboard

import android.view.inputmethod.InputConnection
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ClipboardItemEntity
import com.example.data.repository.ClipboardRepositoryImpl
import com.example.data.repository.GeminiAiRepositoryImpl
import kotlinx.coroutines.launch

@Composable
fun KeyboardScreen(
    clipboardRepository: ClipboardRepositoryImpl,
    aiRepository: GeminiAiRepositoryImpl,
    inputConnectionProvider: () -> InputConnection?
) {
    var activeTab by remember { mutableStateOf(KeyboardTab.HISTORY) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp) // Typical keyboard height
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabButton(
                icon = Icons.Default.History,
                label = "History",
                selected = activeTab == KeyboardTab.HISTORY,
                onClick = { activeTab = KeyboardTab.HISTORY }
            )
            TabButton(
                icon = Icons.Default.AutoAwesome,
                label = "AI Tools",
                selected = activeTab == KeyboardTab.AI_TOOLS,
                onClick = { activeTab = KeyboardTab.AI_TOOLS }
            )
            TabButton(
                icon = Icons.Default.Bookmarks,
                label = "Templates",
                selected = activeTab == KeyboardTab.TEMPLATES,
                onClick = { activeTab = KeyboardTab.TEMPLATES }
            )
        }
        
        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                KeyboardTab.HISTORY -> HistoryTab(clipboardRepository, inputConnectionProvider)
                KeyboardTab.AI_TOOLS -> AiToolsTab(aiRepository, inputConnectionProvider)
                KeyboardTab.TEMPLATES -> TemplatesTab(inputConnectionProvider)
            }
        }
    }
}

enum class KeyboardTab { HISTORY, AI_TOOLS, TEMPLATES }

@Composable
fun TabButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Text(label, fontSize = 10.sp, color = color, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun HistoryTab(
    clipboardRepository: ClipboardRepositoryImpl,
    inputConnectionProvider: () -> InputConnection?
) {
    val historyItems by clipboardRepository.getAllItems().collectAsState(initial = emptyList())
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(historyItems) { item ->
            HistoryItemCard(item = item) {
                inputConnectionProvider()?.commitText(item.text, 1)
            }
        }
    }
}

@Composable
fun HistoryItemCard(item: ClipboardItemEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.text,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.category,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AiToolsTab(
    aiRepository: GeminiAiRepositoryImpl,
    inputConnectionProvider: () -> InputConnection?
) {
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf("") }
    
    val prompts = listOf(
        "Summarize", "Professional Tone", "Casual Tone", "Fix Grammar", "Rewrite"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                val ic = inputConnectionProvider()
                // Request extracted text
                val extracted = ic?.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
                selectedText = extracted?.text?.toString() ?: ""
                
                if (selectedText.isBlank()) {
                    // Try getting text before cursor if nothing is selected/extracted broadly
                    val before = ic?.getTextBeforeCursor(1000, 0)?.toString()
                    val after = ic?.getTextAfterCursor(1000, 0)?.toString()
                    selectedText = "${before ?: ""}${after ?: ""}"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Read Current Text Field")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (selectedText.isNotBlank()) {
            Text(
                text = "Extracted: ${selectedText.take(50)}${if (selectedText.length > 50) "..." else ""}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(prompts) { prompt ->
                    AssistChip(
                        onClick = {
                            if (isProcessing) return@AssistChip
                            isProcessing = true
                            scope.launch {
                                val query = "You are a keyboard AI. Apply this instruction: $prompt. Just output the result."
                                val result = aiRepository.chatWithContext(query, selectedText)
                                result.onSuccess { response ->
                                    val ic = inputConnectionProvider()
                                    // Try to replace all text. Since getting total length might be tricky, we just delete a lot around cursor
                                    ic?.deleteSurroundingText(10000, 10000)
                                    ic?.commitText(response, 1)
                                    selectedText = response
                                }
                                isProcessing = false
                            }
                        },
                        label = { Text(prompt) },
                        leadingIcon = {
                            if (isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TemplatesTab(
    inputConnectionProvider: () -> InputConnection?
) {
    val templates = listOf(
        "Sounds good to me!",
        "I'll get back to you on this.",
        "Thank you for the update.",
        "Can we schedule a quick call to discuss?",
        "Let me know if you need any further information."
    )
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(templates) { template ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { inputConnectionProvider()?.commitText(template, 1) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Text(
                    text = template,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}
