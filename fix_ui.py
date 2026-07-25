import re
with open('app/src/main/java/com/example/presentation/clipboard_list/ClipboardListScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('var showExportDialog by remember { mutableStateOf(false) }', 'var showExportDialog by remember { mutableStateOf(false) }\n    var showApiKeyDialog by remember { mutableStateOf(false) }')

content = content.replace(
'''                                DropdownMenuItem(
                                    text = { Text("Export All JSON") },
                                    leadingIcon = { Icon(Icons.Filled.Backup, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        isExportingSelected = false
                                        exportLauncher.launch("clipboard_export.json")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Import JSON") },
                                    leadingIcon = { Icon(Icons.Filled.Restore, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        importLauncher.launch(arrayOf("application/json"))
                                    }
                                )''',
'''                                DropdownMenuItem(
                                    text = { Text("AI API Key Setup") },
                                    leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        showApiKeyDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export All JSON") },
                                    leadingIcon = { Icon(Icons.Filled.Backup, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        isExportingSelected = false
                                        exportLauncher.launch("clipboard_export.json")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Import JSON") },
                                    leadingIcon = { Icon(Icons.Filled.Restore, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        importLauncher.launch(arrayOf("application/json"))
                                    }
                                )''')

if 'import androidx.compose.material.icons.filled.Key' not in content:
    content = content.replace('import androidx.compose.material.icons.filled.Keyboard', 'import androidx.compose.material.icons.filled.Keyboard\nimport androidx.compose.material.icons.filled.Key')

with open('app/src/main/java/com/example/presentation/clipboard_list/ClipboardListScreen.kt', 'w') as f:
    f.write(content)
