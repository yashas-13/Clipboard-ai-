with open('app/src/main/java/com/example/presentation/clipboard_list/ClipboardListScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('var showMenu by remember { mutableStateOf(false) }', 'var showMenu by remember { mutableStateOf(false) }\n    var showApiKeyDialog by remember { mutableStateOf(false) }')

with open('app/src/main/java/com/example/presentation/clipboard_list/ClipboardListScreen.kt', 'w') as f:
    f.write(content)
