package com.example.domain.repository

import com.example.data.local.ClipboardItemEntity
import com.example.data.local.ClipboardGroupEntity
import com.example.data.local.GroupItemCrossRefEntity
import com.example.data.local.SmartRuleEntity
import kotlinx.coroutines.flow.Flow

interface ClipboardRepository {
    fun getAllItems(): Flow<List<ClipboardItemEntity>>
    fun getFavorites(): Flow<List<ClipboardItemEntity>>
    suspend fun insertItem(item: ClipboardItemEntity): Long
    suspend fun updateItem(item: ClipboardItemEntity)
    suspend fun deleteItem(item: ClipboardItemEntity)

    // Clipboard groups
    fun getAllGroups(): Flow<List<ClipboardGroupEntity>>
    fun getItemsForGroup(groupId: Int): Flow<List<ClipboardItemEntity>>
    suspend fun createGroup(name: String, description: String = ""): Long
    suspend fun deleteGroup(group: ClipboardGroupEntity)
    suspend fun addItemToGroup(groupId: Int, itemId: Int)
    suspend fun removeItemFromGroup(groupId: Int, itemId: Int)
    fun getAllCrossRefs(): Flow<List<GroupItemCrossRefEntity>>

    // Smart Rules
    fun getAllSmartRules(): Flow<List<SmartRuleEntity>>
    suspend fun insertSmartRule(rule: SmartRuleEntity): Long
    suspend fun updateSmartRule(rule: SmartRuleEntity)
    suspend fun deleteSmartRule(rule: SmartRuleEntity)
}
