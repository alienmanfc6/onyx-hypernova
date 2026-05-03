package com.alienmantech.onyx_hypernova.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RankedListDao {

    @Query("SELECT * FROM ranked_lists ORDER BY updatedAt DESC")
    fun getAllLists(): Flow<List<RankedListEntity>>

    @Query("SELECT * FROM ranked_lists WHERE id = :id")
    suspend fun getListById(id: Long): RankedListEntity?

    @Insert
    suspend fun insertList(list: RankedListEntity): Long

    @Update
    suspend fun updateList(list: RankedListEntity)

    @Delete
    suspend fun deleteList(list: RankedListEntity)

    @Query("SELECT * FROM ranked_lists")
    suspend fun getAllListsOnce(): List<RankedListEntity>

    @Query("DELETE FROM ranked_lists")
    suspend fun deleteAllLists()
}

@Dao
interface RankedItemDao {

    @Query("SELECT * FROM ranked_items WHERE listId = :listId ORDER BY position ASC")
    fun getItemsForList(listId: Long): Flow<List<RankedItemEntity>>

    @Insert
    suspend fun insertItem(item: RankedItemEntity): Long

    @Update
    suspend fun updateItem(item: RankedItemEntity)

    @Update
    suspend fun updateItems(items: List<RankedItemEntity>)

    @Delete
    suspend fun deleteItem(item: RankedItemEntity)

    @Query("SELECT COUNT(*) FROM ranked_items WHERE listId = :listId")
    fun getItemCount(listId: Long): Flow<Int>

    @Query("UPDATE ranked_items SET color = :color WHERE id = :itemId")
    suspend fun updateItemColor(itemId: Long, color: String?)

    @Query("SELECT * FROM ranked_items WHERE listId = :listId ORDER BY position ASC")
    suspend fun getItemsForListOnce(listId: Long): List<RankedItemEntity>
}

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query(
        """
        SELECT t.id, t.name, COUNT(x.itemId) AS usageCount
        FROM tags t
        LEFT JOIN item_tag_cross_ref x ON t.id = x.tagId
        GROUP BY t.id, t.name
        ORDER BY t.name COLLATE NOCASE ASC
        """
    )
    fun getTagSummaries(): Flow<List<TagSummary>>

    @Query("SELECT t.* FROM tags t INNER JOIN item_tag_cross_ref x ON t.id = x.tagId WHERE x.itemId = :itemId ORDER BY t.name ASC")
    fun getTagsForItem(itemId: Long): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Query("SELECT * FROM tags WHERE id = :tagId LIMIT 1")
    suspend fun getTagById(tagId: Long): TagEntity?

    @Query("UPDATE tags SET name = :name WHERE id = :tagId")
    suspend fun updateTagName(tagId: Long, name: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: ItemTagCrossRef)

    @Query("INSERT OR IGNORE INTO item_tag_cross_ref (itemId, tagId) SELECT itemId, :targetTagId FROM item_tag_cross_ref WHERE tagId = :sourceTagId")
    suspend fun moveCrossRefsToTag(sourceTagId: Long, targetTagId: Long)

    @Delete
    suspend fun deleteCrossRef(crossRef: ItemTagCrossRef)

    @Query("DELETE FROM item_tag_cross_ref WHERE tagId = :tagId")
    suspend fun clearItemsForTag(tagId: Long)

    @Query("DELETE FROM item_tag_cross_ref WHERE itemId = :itemId")
    suspend fun clearTagsForItem(itemId: Long)

    @Query("SELECT t.* FROM tags t INNER JOIN item_tag_cross_ref x ON t.id = x.tagId WHERE x.itemId = :itemId")
    suspend fun getTagsForItemOnce(itemId: Long): List<TagEntity>

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTagById(tagId: Long)

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()
}
