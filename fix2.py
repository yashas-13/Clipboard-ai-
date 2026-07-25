import re
with open('app/src/main/java/com/example/presentation/clipboard_list/ClipboardListScreen.kt', 'r') as f:
    content = f.read()

replacement = '''                        items(items, key = { it.id }) { item ->
                            val isSelected = selectedItemIds.contains(item.id)
                            SwipeRevealBox(
                                modifier = Modifier.animateItem(),
                                backgroundContent = {
                                    val clipboardManager = LocalClipboardManager.current
                                    val context = LocalContext.current
                                    androidx.compose.foundation.layout.Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.IconButton(
                                            onClick = {
                                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(item.text))
                                                android.widget.Toast.makeText(context, "Copied as Plain Text", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, androidx.compose.foundation.shape.CircleShape)
                                        ) {
                                            androidx.compose.material3.Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                        
                                        androidx.compose.foundation.layout.Row(
                                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                                        ) {
                                            androidx.compose.material3.IconButton(
                                                onClick = {
                                                    val shareIntent = android.content.Intent().apply {
                                                        action = android.content.Intent.ACTION_SEND
                                                        putExtra(android.content.Intent.EXTRA_TEXT, item.text)
                                                        type = "text/plain"
                                                    }
                                                    context.startActivity(android.content.Intent.createChooser(shareIntent, null))
                                                },
                                                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape)
                                            ) {
                                                androidx.compose.material3.Icon(Icons.Filled.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                            }
                                            androidx.compose.material3.IconButton(
                                                onClick = { viewModel.deleteItem(item) },
                                                modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer, androidx.compose.foundation.shape.CircleShape)
                                            ) {
                                                androidx.compose.material3.Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
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
                                    onTogglePin = { viewModel.togglePin(item) }
                                )
                            }
                        }'''

content = content.replace(
'''                        items(items, key = { it.id }) { item ->
                            val isSelected = selectedItemIds.contains(item.id)
                            ClipboardItemCard(
                                modifier = Modifier.animateItem(),
                                item = item,
                                isSelected = isSelected,
                                isSelectionMode = selectedItemIds.isNotEmpty(),
                                isPrivacyModeEnabled = isPrivacyModeEnabled,
                                onSelectToggle = { viewModel.toggleSelection(item.id) },
                                onDelete = { viewModel.deleteItem(item) },
                                onSummarize = { prompt -> viewModel.summarizeItem(item, prompt) },
                                onTogglePin = { viewModel.togglePin(item) }
                            )
                        }''', replacement)

if 'import androidx.compose.material.icons.filled.Share' not in content:
    content = content.replace('import androidx.compose.material.icons.filled.Person', 'import androidx.compose.material.icons.filled.Person\\nimport androidx.compose.material.icons.filled.Share')

with open('app/src/main/java/com/example/presentation/clipboard_list/ClipboardListScreen.kt', 'w') as f:
    f.write(content)
