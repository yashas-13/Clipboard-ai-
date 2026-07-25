package com.example.data.local

import androidx.room.Entity

@Entity(
    tableName = "group_item_cross_ref",
    primaryKeys = ["groupId", "itemId"]
)
data class GroupItemCrossRefEntity(
    val groupId: Int,
    val itemId: Int
)
