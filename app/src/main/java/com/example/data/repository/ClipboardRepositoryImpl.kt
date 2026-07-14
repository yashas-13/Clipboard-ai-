package com.example.data.repository

import com.example.data.local.ClipboardDao
import com.example.data.local.ClipboardItemEntity
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
        dao.deleteItem(item)
    }
}
