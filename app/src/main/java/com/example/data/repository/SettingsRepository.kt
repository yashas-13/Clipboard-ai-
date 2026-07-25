package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _apiKeyFlow = MutableStateFlow(prefs.getString("api_key", null))
    val apiKeyFlow: StateFlow<String?> = _apiKeyFlow

    fun saveApiKey(key: String) {
        prefs.edit().putString("api_key", key).apply()
        _apiKeyFlow.value = key
    }

    fun getApiKey(): String? {
        return prefs.getString("api_key", null)
    }
}
