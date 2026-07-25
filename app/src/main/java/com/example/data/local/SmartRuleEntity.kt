package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "smart_rules")
@Serializable
data class SmartRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ruleName: String,
    val targetFolderName: String,
    val sourceAppFilter: String = "",
    val keywordFilter: String = "",
    val categoryFilter: String = "",
    val tagsToApply: String = "",
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
