package com.example.presentation.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SmartRuleEntity
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleContainer
import com.example.ui.theme.Slate400

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartRulesDialog(
    rules: List<SmartRuleEntity>,
    onDismiss: () -> Unit,
    onCreateRule: (ruleName: String, targetFolder: String, sourceApp: String, keywords: String, category: String, tags: String) -> Unit,
    onToggleRule: (SmartRuleEntity) -> Unit,
    onDeleteRule: (SmartRuleEntity) -> Unit
) {
    var showCreateForm by remember { mutableStateOf(false) }

    var ruleNameInput by remember { mutableStateOf("") }
    var targetFolderInput by remember { mutableStateOf("") }
    var sourceAppInput by remember { mutableStateOf("") }
    var keywordInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryPurpleContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = "Smart Folders Rules",
                            tint = PrimaryPurple
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Smart Folder Rules",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Auto-sort & tag clipboard items automatically",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!showCreateForm) {
                // Top bar with Add Rule action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Rules (${rules.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Button(
                        onClick = { showCreateForm = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Rule")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (rules.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Rule,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Slate400
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No Smart Rules configured yet", color = Slate400, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { showCreateForm = true }) {
                                Text("Create First Rule")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(rules, key = { it.id }) { rule ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (rule.isEnabled) {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = rule.ruleName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            AssistChip(
                                                onClick = {},
                                                label = { Text("📁 ${rule.targetFolderName}", fontSize = 11.sp) }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        val details = buildList {
                                            if (rule.sourceAppFilter.isNotBlank()) add("App: ${rule.sourceAppFilter}")
                                            if (rule.keywordFilter.isNotBlank()) add("Keywords: ${rule.keywordFilter}")
                                            if (rule.categoryFilter.isNotBlank()) add("Category: ${rule.categoryFilter}")
                                            if (rule.tagsToApply.isNotBlank()) add("Auto Tags: ${rule.tagsToApply}")
                                        }

                                        Text(
                                            text = if (details.isNotEmpty()) details.joinToString(" • ") else "Applies to all matching clips",
                                            fontSize = 12.sp,
                                            color = Slate400
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = rule.isEnabled,
                                            onCheckedChange = { onToggleRule(rule) }
                                        )
                                        IconButton(onClick = { onDeleteRule(rule) }) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete Rule",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Create New Rule Form
                Text(
                    text = "Create New Smart Rule",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ruleNameInput,
                    onValueChange = { ruleNameInput = it },
                    label = { Text("Rule Name (e.g. Chrome Shopping)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = targetFolderInput,
                    onValueChange = { targetFolderInput = it },
                    label = { Text("Target Folder Name (e.g. Shopping)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = sourceAppInput,
                    onValueChange = { sourceAppInput = it },
                    label = { Text("Source App Filter (e.g. Chrome, Slack)") },
                    placeholder = { Text("Leave blank for any app") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = keywordInput,
                    onValueChange = { keywordInput = it },
                    label = { Text("Keywords (comma separated e.g. buy, cart, price)") },
                    placeholder = { Text("e.g. shopping, order, amazon") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Auto-Apply Tags (e.g. #shopping, #web)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showCreateForm = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (ruleNameInput.isNotBlank() && targetFolderInput.isNotBlank()) {
                                onCreateRule(
                                    ruleNameInput.trim(),
                                    targetFolderInput.trim(),
                                    sourceAppInput.trim(),
                                    keywordInput.trim(),
                                    categoryInput.trim(),
                                    tagsInput.trim()
                                )
                                showCreateForm = false
                                ruleNameInput = ""
                                targetFolderInput = ""
                                sourceAppInput = ""
                                keywordInput = ""
                                categoryInput = ""
                                tagsInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Save Smart Rule")
                    }
                }
            }
        }
    }
}
