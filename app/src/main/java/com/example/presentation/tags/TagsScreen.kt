package com.example.presentation.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(onNavigateBack: () -> Unit) {
    val autoCategories = listOf("Code", "URL", "Email", "OTP")
    val userTags = remember { mutableStateListOf("Work", "Personal", "Research") }
    var showAddDialog by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Tag") },
            text = {
                OutlinedTextField(
                    value = newTagText,
                    onValueChange = { newTagText = it },
                    label = { Text("Tag Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTagText.isNotBlank()) {
                        userTags.add(newTagText)
                        newTagText = ""
                        showAddDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tags & Categories", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryPurple,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Tag")
            }
        },
        containerColor = BackgroundLight
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Auto Categories", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    autoCategories.forEach { category ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text(category) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = BlueBackground,
                                labelColor = BlueAccent
                            ),
                            border = null
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("User Tags", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            items(userTags) { tag ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackgroundLight),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tag, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        IconButton(onClick = { userTags.remove(tag) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Slate400)
                        }
                    }
                }
            }
        }
    }
}
