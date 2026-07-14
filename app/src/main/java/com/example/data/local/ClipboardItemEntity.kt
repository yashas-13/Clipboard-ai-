package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "clipboard_items")
@Serializable
data class ClipboardItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceApp: String = "Unknown",
    val category: String = "General",
    val preview: String = "",
    val tags: String = "",
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val wordCount: Int = 0,
    val charCount: Int = 0,
    val copyCount: Int = 1,
    val lastUsed: Long = System.currentTimeMillis()
)
