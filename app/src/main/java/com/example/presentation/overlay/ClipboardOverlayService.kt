package com.example.presentation.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.ui.theme.MyApplicationTheme
import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PrimaryPurple
import android.content.ClipboardManager
import com.example.data.local.AppDatabase
import com.example.data.repository.ClipboardRepositoryImpl
import com.example.data.local.ClipboardItemEntity
import com.example.data.local.ClipboardClassifier
import com.example.domain.rules.SmartFolderEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.room.Room
import android.provider.Settings

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.presentation.widget.ClipboardWidgetProvider

class ClipboardOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    
    private lateinit var clipboardManager: ClipboardManager
    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastSavedText: String = ""

    companion object {
        const val CHANNEL_ID = "clipboard_ai_fg_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_MONITOR"
        const val ACTION_STOP = "ACTION_STOP_MONITOR"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Clipboard AI Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Running background foreground service for Clipboard AI smart features"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ClipboardOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Clipboard AI Active ✨")
            .setContentText("Monitoring copied items & smart neural categorizer")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingOpenIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Open App", pendingOpenIntent)
            .addAction(0, "Pause", pendingStopIntent)
            .build()
    }
    
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        try {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val item = clip.getItemAt(0)
                val text = item.text?.toString()
                val uri = item.uri
                
                if (text != null && text.isNotBlank() && text != lastSavedText) {
                    lastSavedText = text
                    saveToDatabase(text)
                } else if (uri != null) {
                    val uriString = uri.toString()
                    if (uriString != lastSavedText) {
                        lastSavedText = uriString
                        saveToDatabase(uriString)
                    }
                }
            }
        } catch (e: SecurityException) {
            // Android 10+ restricts background clipboard access when not in focus.
            // Catching gracefully respects Android OS security policy.
        } catch (e: Throwable) {
            // Handle any other runtime clipboard exception safely
        }
    }
    
    private fun saveToDatabase(text: String) {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "clipboard_db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
        val repository = ClipboardRepositoryImpl(db.clipboardDao())
        val aiRepository = com.example.data.repository.GeminiAiRepositoryImpl(this)
        
        scope.launch {
            val baseCategory = ClipboardClassifier.classify(text)
            val preview = ClipboardClassifier.getPreview(text)
            val wordCount = if (text.isBlank()) 0 else text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            
            // Apply Smart Folder Rules & Auto-Tagging
            val rules = repository.getAllSmartRules().first()
            val matchResult = SmartFolderEngine.evaluateRules(text, "Foreground Service", baseCategory, rules)

            val finalCategory = matchResult.matchedFolderName ?: baseCategory
            val autoTagsStr = matchResult.autoTags.joinToString(", ")

            val entity = ClipboardItemEntity(
                text = text,
                category = finalCategory,
                preview = preview,
                wordCount = wordCount,
                charCount = text.length,
                sourceApp = "Foreground Monitor",
                tags = autoTagsStr
            )
            val insertedId = repository.insertItem(entity)
            
            // Immediately refresh Home Screen App Widget
            ClipboardWidgetProvider.updateAllWidgets(applicationContext)

            // Run background Gemini analysis to refine and extract tags/hashtags
            launch {
                val result = aiRepository.analyzeClipboardContent(text)
                result.onSuccess { analysis ->
                    val combinedTags = (matchResult.autoTags + analysis.tags).toSet().joinToString(", ")
                    val updatedEntity = entity.copy(
                        id = insertedId.toInt(),
                        category = matchResult.matchedFolderName ?: analysis.category,
                        tags = combinedTags
                    )
                    repository.updateItem(updatedEntity)
                    ClipboardWidgetProvider.updateAllWidgets(applicationContext)
                }.onFailure {
                    it.printStackTrace()
                }
            }
        }
    }
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        try {
            if (Settings.canDrawOverlays(this)) {
                // showOverlay()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener(clipboardListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        return START_STICKY
    }

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(this)) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.END
        params.x = 0
        params.y = 100

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ClipboardOverlayService)
            setViewTreeViewModelStoreOwner(this@ClipboardOverlayService)
            setViewTreeSavedStateRegistryOwner(this@ClipboardOverlayService)
            setContent {
                MyApplicationTheme {
                    FloatingBubble()
                }
            }
        }

        try {
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboardManager.removePrimaryClipChangedListener(clipboardListener)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        
        composeView?.let { windowManager.removeView(it) }
        store.clear()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
}

@Composable
fun FloatingBubble() {
    Surface(
        shape = CircleShape,
        color = PrimaryPurple,
        modifier = Modifier.size(56.dp).padding(4.dp),
        shadowElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "AI Actions",
                tint = Color.White
            )
        }
    }
}
