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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.room.Room
import android.provider.Settings

class ClipboardOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    
    private lateinit var clipboardManager: ClipboardManager
    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastSavedText: String = ""
    
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        if (clipboardManager.hasPrimaryClip()) {
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
        }
    }
    
    private fun saveToDatabase(text: String) {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "clipboard_db"
        ).fallbackToDestructiveMigration().build()
        val repository = ClipboardRepositoryImpl(db.clipboardDao())
        val aiRepository = com.example.data.repository.GeminiAiRepositoryImpl()
        
        scope.launch {
            val category = ClipboardClassifier.classify(text)
            val preview = ClipboardClassifier.getPreview(text)
            val wordCount = if (text.isBlank()) 0 else text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            
            val entity = ClipboardItemEntity(
                text = text,
                category = category,
                preview = preview,
                wordCount = wordCount,
                charCount = text.length,
                sourceApp = "Auto Fetch"
            )
            val insertedId = repository.insertItem(entity)
            
            // Run background Gemini analysis to refine and extract tags/hashtags
            launch {
                val result = aiRepository.analyzeClipboardContent(text)
                result.onSuccess { analysis ->
                    val updatedEntity = entity.copy(
                        id = insertedId.toInt(),
                        category = analysis.category,
                        tags = analysis.tags.joinToString(", ")
                    )
                    repository.updateItem(updatedEntity)
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
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        try {
            if (Settings.canDrawOverlays(this)) {
                // showOverlay() - Disabled automatically showing to prevent AppOps restricted logs
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener(clipboardListener)
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
