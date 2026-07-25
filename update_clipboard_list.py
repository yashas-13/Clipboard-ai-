with open('app/src/main/java/com/example/presentation/clipboard_list/ClipboardListScreen.kt', 'r') as f:
    content = f.read()

# Add import
if 'import com.example.presentation.form_assistant.FormAssistantScreen' not in content:
    content = 'import com.example.presentation.form_assistant.FormAssistantScreen\n' + content

# Update title in else block of topBar
old_top_bar = '''            } else {
                TopAppBar(
                    title = { Text("Usage Analytics", fontWeight = FontWeight.SemiBold) },'''

new_top_bar = '''            } else {
                val titleText = when (selectedTab) {
                    1 -> "Form Assistant & Smart Paste"
                    2 -> "RAG Studio"
                    else -> "Usage Analytics"
                }
                TopAppBar(
                    title = { Text(titleText, fontWeight = FontWeight.SemiBold) },'''

content = content.replace(old_top_bar, new_top_bar)

# Update NavigationBar items
old_nav_bar = '''                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "RAG Studio"
                        )
                    },
                    label = { Text("RAG Studio") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = PrimaryPurple,
                        indicatorColor = PrimaryPurple
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                            contentDescription = "Analytics"
                        )
                    },
                    label = { Text("Analytics") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = PrimaryPurple,
                        indicatorColor = PrimaryPurple
                    )
                )'''

new_nav_bar = '''                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.AssignmentTurnedIn,
                            contentDescription = "Form Paste"
                        )
                    },
                    label = { Text("Form Paste") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = PrimaryPurple,
                        indicatorColor = PrimaryPurple
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "RAG Studio"
                        )
                    },
                    label = { Text("RAG Studio") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = PrimaryPurple,
                        indicatorColor = PrimaryPurple
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 3) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                            contentDescription = "Analytics"
                        )
                    },
                    label = { Text("Analytics") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = PrimaryPurple,
                        indicatorColor = PrimaryPurple
                    )
                )'''

content = content.replace(old_nav_bar, new_nav_bar)

# Update when (selectedTab)
old_when = '''            1 -> {
                RagStudioTab(
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding)
                )
            }
            2 -> {
                UsageDashboardScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding)
                )
            }'''

new_when = '''            1 -> {
                FormAssistantScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding)
                )
            }
            2 -> {
                RagStudioTab(
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding)
                )
            }
            3 -> {
                UsageDashboardScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding)
                )
            }'''

content = content.replace(old_when, new_when)

# Update SwipeRevealBox copy action to include Form Analyze icon button
old_swipe_left = '''                                        androidx.compose.material3.IconButton(
                                            onClick = {
                                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(item.text))
                                                android.widget.Toast.makeText(context, "Copied as Plain Text", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, androidx.compose.foundation.shape.CircleShape)
                                        ) {
                                            androidx.compose.material3.Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }'''

new_swipe_left = '''                                        androidx.compose.foundation.layout.Row(
                                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
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
                                            androidx.compose.material3.IconButton(
                                                onClick = {
                                                    viewModel.updateFormInputText(item.text)
                                                    viewModel.analyzeFormText(item.text)
                                                    selectedTab = 1
                                                },
                                                modifier = Modifier.background(PrimaryPurpleContainer, androidx.compose.foundation.shape.CircleShape)
                                            ) {
                                                androidx.compose.material3.Icon(Icons.Filled.AssignmentTurnedIn, contentDescription = "Form Analyze", tint = PrimaryPurple)
                                            }
                                        }'''

content = content.replace(old_swipe_left, new_swipe_left)

with open('app/src/main/java/com/example/presentation/clipboard_list/ClipboardListScreen.kt', 'w') as f:
    f.write(content)
