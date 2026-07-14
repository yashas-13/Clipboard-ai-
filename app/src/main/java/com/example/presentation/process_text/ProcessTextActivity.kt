package com.example.presentation.process_text

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.data.local.AppDatabase
import com.example.data.local.ClipboardItemEntity
import com.example.data.repository.ClipboardRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.room.Room

import com.example.data.local.ClipboardClassifier

class ProcessTextActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        
        if (text != null && text.isNotBlank()) {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "clipboard_db"
            ).fallbackToDestructiveMigration().build()
            val repository = ClipboardRepositoryImpl(db.clipboardDao())
            
            CoroutineScope(Dispatchers.IO).launch {
                val category = ClipboardClassifier.classify(text)
                val preview = ClipboardClassifier.getPreview(text)
                val wordCount = text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                
                repository.insertItem(
                    ClipboardItemEntity(
                        text = text,
                        category = category,
                        preview = preview,
                        wordCount = wordCount,
                        charCount = text.length,
                        sourceApp = "Text Selection"
                    )
                )
            }
            
            Toast.makeText(this, "Saved to Clipboard AI", Toast.LENGTH_SHORT).show()
        }
        
        finish()
    }
}
