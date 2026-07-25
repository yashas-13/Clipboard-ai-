with open('app/src/main/java/com/example/presentation/clipboard_list/ClipboardListScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('import androidx.compose.material.icons.filled.Personimport androidx.compose.material.icons.filled.Shareimport androidx.compose.material.icons.filled.Passwordimport androidx.compose.material.icons.filled.SmartToy', 
'''import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.SmartToy''')

with open('app/src/main/java/com/example/presentation/clipboard_list/ClipboardListScreen.kt', 'w') as f:
    f.write(content)
