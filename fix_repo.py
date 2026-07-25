import re
with open('app/src/main/java/com/example/data/repository/GeminiAiRepositoryImpl.kt', 'r') as f:
    content = f.read()

content = content.replace('class GeminiAiRepositoryImpl :', 'class GeminiAiRepositoryImpl(private val context: android.content.Context) :')
content = content.replace('BuildConfig.GEMINI_API_KEY', 'getApiKey()')
content = content.replace('    private val apiService: GeminiApiService by lazy {',
'''    private fun getApiKey(): String {
        val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        val userKey = prefs.getString("api_key", "")
        return if (!userKey.isNullOrEmpty()) userKey else BuildConfig.GEMINI_API_KEY
    }

    private val apiService: GeminiApiService by lazy {''')

with open('app/src/main/java/com/example/data/repository/GeminiAiRepositoryImpl.kt', 'w') as f:
    f.write(content)
