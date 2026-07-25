package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.theme.MyApplicationTheme

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.repository.ClipboardRepositoryImpl
import com.example.data.repository.GeminiAiRepositoryImpl
import com.example.presentation.clipboard_list.ClipboardListScreen
import com.example.presentation.clipboard_list.ClipboardViewModel
import com.example.presentation.onboarding.OnboardingScreen
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxSize

import android.content.Intent
import android.content.Context
import android.provider.Settings
import com.example.presentation.overlay.ClipboardOverlayService
import com.example.data.worker.ClipboardSyncScheduler

import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.example.data.local.ClipboardItemEntity
import com.example.data.local.ClipboardClassifier
import com.example.presentation.widget.ClipboardWidgetProvider

class MainActivity : ComponentActivity() {
  private lateinit var database: AppDatabase
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Schedule WorkManager periodic sync task for battery-efficient background operation
    ClipboardSyncScheduler.schedulePeriodicSync(applicationContext)
    ClipboardSyncScheduler.runOneTimeSync(applicationContext)
    
    database = Room.databaseBuilder(
        applicationContext,
        AppDatabase::class.java,
        "clipboard_db"
    ).fallbackToDestructiveMigration(dropAllTables = true).build()
    
    val repository = ClipboardRepositoryImpl(database.clipboardDao())
    val aiRepository = GeminiAiRepositoryImpl(applicationContext)
    
    enableEdgeToEdge()
    setContent {
      val viewModel: ClipboardViewModel = viewModel(factory = ClipboardViewModel.provideFactory(repository, aiRepository))
      
      val sharedPrefs = getSharedPreferences("clipboard_prefs", Context.MODE_PRIVATE)
      var showOnboarding by remember { mutableStateOf(sharedPrefs.getBoolean("show_onboarding", true)) }
      
      val systemDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
      var isDarkTheme by remember { mutableStateOf(sharedPrefs.getBoolean("dark_theme", systemDarkTheme)) }
      
      MyApplicationTheme(darkTheme = isDarkTheme) {
        if (showOnboarding) {
            OnboardingScreen(onFinish = {
                sharedPrefs.edit().putBoolean("show_onboarding", false).apply()
                showOnboarding = false
                viewModel.seedPrebuiltItemsAndRules()
                try {
                    val intent = Intent(this@MainActivity, ClipboardOverlayService::class.java)
                    ContextCompat.startForegroundService(this@MainActivity, intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
        } else {
            LaunchedEffect(Unit) {
                try {
                    val intent = Intent(this@MainActivity, ClipboardOverlayService::class.java)
                    ContextCompat.startForegroundService(this@MainActivity, intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            ClipboardListScreen(
                viewModel = viewModel,
                isDarkTheme = isDarkTheme,
                onThemeToggle = { 
                    isDarkTheme = !isDarkTheme
                    sharedPrefs.edit().putBoolean("dark_theme", isDarkTheme).apply()
                }
            )
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    checkAndSyncClipboardOnFocus()
  }

  private fun checkAndSyncClipboardOnFocus() {
    try {
      val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
      if (clipboardManager != null && clipboardManager.hasPrimaryClip()) {
        val clip = clipboardManager.primaryClip
        if (clip != null && clip.itemCount > 0) {
          val clipText = clip.getItemAt(0).text?.toString()
          if (!clipText.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
              val dao = database.clipboardDao()
              val all = dao.getAllItems().first()
              if (all.none { it.text == clipText }) {
                val category = ClipboardClassifier.classify(clipText)
                val preview = ClipboardClassifier.getPreview(clipText)
                val item = ClipboardItemEntity(
                  text = clipText,
                  category = category,
                  preview = preview,
                  timestamp = System.currentTimeMillis(),
                  sourceApp = "Clipboard Auto-Detect"
                )
                dao.insertItem(item)
                ClipboardWidgetProvider.updateAllWidgets(applicationContext)
              }
            }
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("Android") }
}
