package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_items ORDER BY isPinned DESC, timestamp DESC")
    fun getAllItems(): Flow<List<ClipboardItemEntity>>
    
    @Query("SELECT * FROM clipboard_items WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<ClipboardItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ClipboardItemEntity): Long

    @Update
    suspend fun updateItem(item: ClipboardItemEntity)

    @Delete
    suspend fun deleteItem(item: ClipboardItemEntity)

    @Query("DELETE FROM clipboard_items WHERE id = :id")
    suspend fun deleteItemById(id: Int)

    // Clipboard group queries
    @Query("SELECT * FROM clipboard_groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<ClipboardGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: ClipboardGroupEntity): Long

    @Delete
    suspend fun deleteGroup(group: ClipboardGroupEntity)

    // Cross-reference queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupCrossRef(ref: GroupItemCrossRefEntity)

    @Query("DELETE FROM group_item_cross_ref WHERE groupId = :groupId AND itemId = :itemId")
    suspend fun deleteGroupCrossRef(groupId: Int, itemId: Int)

    @Query("DELETE FROM group_item_cross_ref WHERE groupId = :groupId")
    suspend fun deleteCrossRefsForGroup(groupId: Int)

    @Query("DELETE FROM group_item_cross_ref WHERE itemId = :itemId")
    suspend fun deleteCrossRefsForItem(itemId: Int)

    @Query("SELECT * FROM group_item_cross_ref")
    fun getAllCrossRefs(): Flow<List<GroupItemCrossRefEntity>>

    @Query("""
        SELECT ci.* FROM clipboard_items ci
        INNER JOIN group_item_cross_ref ref ON ci.id = ref.itemId
        WHERE ref.groupId = :groupId
        ORDER BY ci.isPinned DESC, ci.timestamp DESC
    """)
    fun getItemsForGroup(groupId: Int): Flow<List<ClipboardItemEntity>>

    // Smart rules queries
    @Query("SELECT * FROM smart_rules ORDER BY createdAt DESC")
    fun getAllSmartRules(): Flow<List<SmartRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmartRule(rule: SmartRuleEntity): Long

    @Update
    suspend fun updateSmartRule(rule: SmartRuleEntity)

    @Delete
    suspend fun deleteSmartRule(rule: SmartRuleEntity)

    @Query("DELETE FROM smart_rules WHERE id = :id")
    suspend fun deleteSmartRuleById(id: Int)
}
