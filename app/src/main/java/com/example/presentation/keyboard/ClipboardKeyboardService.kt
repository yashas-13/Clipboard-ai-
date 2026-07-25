package com.example.presentation.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.repository.ClipboardRepositoryImpl
import com.example.data.repository.GeminiAiRepositoryImpl
import com.example.ui.theme.MyApplicationTheme

class ClipboardKeyboardService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    lateinit var db: AppDatabase
    lateinit var clipboardRepository: ClipboardRepositoryImpl
    lateinit var aiRepository: GeminiAiRepositoryImpl

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "clipboard_db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
        clipboardRepository = ClipboardRepositoryImpl(db.clipboardDao())
        aiRepository = GeminiAiRepositoryImpl(this)
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ClipboardKeyboardService)
            setViewTreeViewModelStoreOwner(this@ClipboardKeyboardService)
            setViewTreeSavedStateRegistryOwner(this@ClipboardKeyboardService)
            setContent {
                MyApplicationTheme {
                    KeyboardScreen(
                        clipboardRepository = clipboardRepository,
                        aiRepository = aiRepository,
                        inputConnectionProvider = { currentInputConnection }
                    )
                }
            }
        }
        return composeView
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore
        get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
}
