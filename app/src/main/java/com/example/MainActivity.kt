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
                try {
                    startService(Intent(this@MainActivity, ClipboardOverlayService::class.java))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
        } else {
            LaunchedEffect(Unit) {
                try {
                    startService(Intent(this@MainActivity, ClipboardOverlayService::class.java))
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
