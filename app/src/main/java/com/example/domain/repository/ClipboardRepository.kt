package com.example.domain.repository

import com.example.data.local.ClipboardItemEntity
import kotlinx.coroutines.flow.Flow

interface ClipboardRepository {
    fun getAllItems(): Flow<List<ClipboardItemEntity>>
    fun getFavorites(): Flow<List<ClipboardItemEntity>>
    suspend fun insertItem(item: ClipboardItemEntity): Long
    suspend fun updateItem(item: ClipboardItemEntity)
    suspend fun deleteItem(item: ClipboardItemEntity)
}
