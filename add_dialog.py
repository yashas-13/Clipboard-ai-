import re
with open('app/src/main/java/com/example/presentation/clipboard_list/ClipboardListScreen.kt', 'r') as f:
    content = f.read()

dialog_code = '''
    if (showApiKeyDialog) {
        var apiKeyInput by remember { mutableStateOf(context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE).getString("api_key", "") ?: "") }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("AI API Key Setup") },
            text = {
                Column {
                    Text("Enter your Gemini API Key to use BYOK. If left empty, the default key will be used.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE).edit().putString("api_key", apiKeyInput.trim()).apply()
                    showApiKeyDialog = false
                    android.widget.Toast.makeText(context, "API Key Saved", android.widget.Toast.LENGTH_SHORT).show()
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
'''

content = content.replace('if (showCategorizeDialog) {', dialog_code + '    if (showCategorizeDialog) {')

with open('app/src/main/java/com/example/presentation/clipboard_list/ClipboardListScreen.kt', 'w') as f:
    f.write(content)
