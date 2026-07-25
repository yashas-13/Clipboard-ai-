package com.example.data.repository

import com.example.data.local.ClipboardDao
import com.example.data.local.ClipboardItemEntity
import com.example.data.local.ClipboardGroupEntity
import com.example.data.local.GroupItemCrossRefEntity
import com.example.data.local.SmartRuleEntity
import com.example.domain.repository.ClipboardRepository
import kotlinx.coroutines.flow.Flow

class ClipboardRepositoryImpl(
    private val dao: ClipboardDao
) : ClipboardRepository {
    override fun getAllItems(): Flow<List<ClipboardItemEntity>> = dao.getAllItems()
    
    override fun getFavorites(): Flow<List<ClipboardItemEntity>> = dao.getFavorites()
    
    override suspend fun insertItem(item: ClipboardItemEntity): Long = dao.insertItem(item)
    
    override suspend fun updateItem(item: ClipboardItemEntity) {
        dao.updateItem(item)
    }
    
    override suspend fun deleteItem(item: ClipboardItemEntity) {
        dao.deleteCrossRefsForItem(item.id)
        dao.deleteItem(item)
    }

    override fun getAllGroups(): Flow<List<ClipboardGroupEntity>> = dao.getAllGroups()

    override fun getItemsForGroup(groupId: Int): Flow<List<ClipboardItemEntity>> = dao.getItemsForGroup(groupId)

    override suspend fun createGroup(name: String, description: String): Long {
        return dao.insertGroup(ClipboardGroupEntity(name = name, description = description))
    }

    override suspend fun deleteGroup(group: ClipboardGroupEntity) {
        dao.deleteCrossRefsForGroup(group.id)
        dao.deleteGroup(group)
    }

    override suspend fun addItemToGroup(groupId: Int, itemId: Int) {
        dao.insertGroupCrossRef(GroupItemCrossRefEntity(groupId, itemId))
    }

    override suspend fun removeItemFromGroup(groupId: Int, itemId: Int) {
        dao.deleteGroupCrossRef(groupId, itemId)
    }

    override fun getAllCrossRefs(): Flow<List<GroupItemCrossRefEntity>> = dao.getAllCrossRefs()

    override fun getAllSmartRules(): Flow<List<SmartRuleEntity>> = dao.getAllSmartRules()

    override suspend fun insertSmartRule(rule: SmartRuleEntity): Long = dao.insertSmartRule(rule)

    override suspend fun updateSmartRule(rule: SmartRuleEntity) {
        dao.updateSmartRule(rule)
    }

    override suspend fun deleteSmartRule(rule: SmartRuleEntity) {
        dao.deleteSmartRule(rule)
    }
}
