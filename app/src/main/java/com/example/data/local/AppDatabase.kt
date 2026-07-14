package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ClipboardItemEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clipboardDao(): ClipboardDao
}
