package com.example.presentation.form_assistant

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ClipboardItemEntity
import com.example.domain.repository.FormField
import com.example.presentation.clipboard_list.ClipboardViewModel
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleContainer
import com.example.ui.theme.Slate400

@Composable
fun FormAssistantScreen(
    viewModel: ClipboardViewModel,
    modifier: Modifier = Modifier
) {
    val formInputText by viewModel.formInputText.collectAsStateWithLifecycle()
    val formStructure by viewModel.formStructure.collectAsStateWithLifecycle()
    val isFormAnalyzing by viewModel.isFormAnalyzing.collectAsStateWithLifecycle()
    val fieldMatches by viewModel.fieldMatches.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val presets = listOf(
        "User Registration" to "Full Name, Email Address, Phone Number, Password, Confirm Password",
        "Developer Credentials" to "API Key, Secret Token, Client ID, Environment URL",
        "Checkout & Address" to "Full Name, Card Number, Expiry Date, CVV, Billing Address, Zip Code",
        "Job Application" to "Full Name, Email Address, Phone Number, Resume Link, Portfolio URL"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = PrimaryPurpleContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PrimaryPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AssignmentTurnedIn,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Smart Form Assistant & Paste",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Analyze form text or web fields and auto-match candidate values from your personal clipboard history!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Quick Preset Chips
        item {
            Column {
                Text(
                    text = "Form Presets",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presets) { (title, template) ->
                        FilterChip(
                            selected = formInputText == template,
                            onClick = {
                                viewModel.updateFormInputText(template)
                                viewModel.analyzeFormText(template)
                            },
                            label = { Text(title) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryPurpleContainer,
                                selectedLabelColor = PrimaryPurple
                            )
                        )
                    }
                }
            }
        }

        // Form Input Box Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Input Form / Fields to Analyze",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = formInputText,
                        onValueChange = { viewModel.updateFormInputText(it) },
                        placeholder = {
                            Text("Paste HTML snippet, form text, or labels (e.g. Email Address, Phone Number, API Key)...")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let { copiedText ->
                                    if (copiedText.isNotBlank()) {
                                        viewModel.updateFormInputText(copiedText)
                                        Toast.makeText(context, "Pasted form text from clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste System Clipboard")
                        }

                        Button(
                            onClick = { viewModel.analyzeFormText(formInputText) },
                            enabled = !isFormAnalyzing && formInputText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isFormAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyzing...")
                            } else {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyze Form")
                            }
                        }
                    }
                }
            }
        }

        // Analysis Result Card Section
        formStructure?.let { structure ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = structure.formTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${structure.fields.size} detected fields mapped with personal clipboard",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }

                    FilledTonalButton(
                        onClick = { viewModel.autoMatchFields(structure.fields) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Re-Match All")
                    }
                }
            }

            // Fields List
            items(structure.fields) { field ->
                val matchedItem = fieldMatches[field.fieldId]
                val suggestions = viewModel.getSuggestedItemsForField(field)

                FormFieldCard(
                    field = field,
                    matchedItem = matchedItem,
                    suggestions = suggestions,
                    onSelectMatch = { item -> viewModel.setFieldMatch(field.fieldId, item) },
                    onCopyValue = { valText ->
                        clipboardManager.setText(AnnotatedString(valText))
                        Toast.makeText(context, "Copied ${field.label} to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Bottom Actions Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.copyFilledFormToClipboard(context, clipboardManager)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy Complete Form")
                        }

                        OutlinedButton(
                            onClick = {
                                val builder = StringBuilder()
                                builder.appendLine("=== ${structure.formTitle} ===")
                                for (f in structure.fields) {
                                    val valText = fieldMatches[f.fieldId]?.text ?: f.placeholder
                                    builder.appendLine("${f.label}: $valText")
                                }
                                viewModel.addManualItem(builder.toString().trim())
                                Toast.makeText(context, "Saved filled form to Clipboard History!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to History")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormFieldCard(
    field: FormField,
    matchedItem: ClipboardItemEntity?,
    suggestions: List<ClipboardItemEntity>,
    onSelectMatch: (ClipboardItemEntity?) -> Unit,
    onCopyValue: (String) -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Field Name & Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getCategoryIcon(field.category),
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = field.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryPurpleContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = field.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryPurple,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Matched Value Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = if (matchedItem != null) PrimaryPurple.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { dropdownExpanded = true }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (matchedItem != null) {
                            Text(
                                text = matchedItem.text,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "From personal clipboard • Category: ${matchedItem.category}",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        } else {
                            Text(
                                text = field.placeholder.ifBlank { "Tap to select from personal clipboard" },
                                fontSize = 13.sp,
                                color = Slate400
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (matchedItem != null) {
                            IconButton(
                                onClick = { onCopyValue(matchedItem.text) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy field value",
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Select candidate match",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.widthIn(max = 300.dp)
                ) {
                    Text(
                        text = "Pick from Personal Clipboard:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = PrimaryPurple,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    HorizontalDivider()

                    if (suggestions.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No clipboard items available", fontSize = 13.sp) },
                            onClick = { dropdownExpanded = false }
                        )
                    } else {
                        suggestions.take(6).forEach { candidate ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = candidate.text,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${candidate.category} • ${candidate.tags}",
                                            fontSize = 10.sp,
                                            color = Slate400
                                        )
                                    }
                                },
                                onClick = {
                                    onSelectMatch(candidate)
                                    dropdownExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getCategoryIcon(candidate.category),
                                        contentDescription = null,
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Email" -> Icons.Filled.Email
        "Phone" -> Icons.Filled.Phone
        "Password" -> Icons.Filled.Lock
        "API Key" -> Icons.Filled.Key
        "Credentials" -> Icons.Filled.Badge
        "Address" -> Icons.Filled.Place
        "Banking" -> Icons.Filled.CreditCard
        "URL" -> Icons.Filled.Link
        else -> Icons.Filled.Assignment
    }
}
