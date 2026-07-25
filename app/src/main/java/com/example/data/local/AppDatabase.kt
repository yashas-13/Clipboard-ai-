package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ClipboardItemEntity::class,
        ClipboardGroupEntity::class,
        GroupItemCrossRefEntity::class,
        SmartRuleEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clipboardDao(): ClipboardDao
}
