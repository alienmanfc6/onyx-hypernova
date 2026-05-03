package com.alienmantech.onyx_hypernova.data.repository

import com.alienmantech.onyx_hypernova.data.db.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RankItRepository @Inject constructor(
    private val listDao: RankedListDao,
    private val itemDao: RankedItemDao,
    private val tagDao: TagDao
) {
    // ── Lists ──────────────────────────────────────────────────────────────

    fun getAllLists(): Flow<List<RankedListEntity>> = listDao.getAllLists()

    suspend fun getListById(id: Long): RankedListEntity? = listDao.getListById(id)

    fun getItemCount(listId: Long): Flow<Int> = itemDao.getItemCount(listId)

    suspend fun createList(name: String): Long =
        listDao.insertList(RankedListEntity(name = name.trim()))

    suspend fun renameList(list: RankedListEntity, newName: String) =
        listDao.updateList(list.copy(name = newName.trim(), updatedAt = System.currentTimeMillis()))

    suspend fun deleteList(list: RankedListEntity) = listDao.deleteList(list)

    // ── Items ──────────────────────────────────────────────────────────────

    fun getItemsForList(listId: Long): Flow<List<RankedItemEntity>> =
        itemDao.getItemsForList(listId)

    suspend fun addItem(listId: Long, name: String, position: Int, tags: List<String> = emptyList()) {
        val id = itemDao.insertItem(RankedItemEntity(listId = listId, name = name.trim(), position = position))
        setTagsForItem(id, tags)
        touchList(listId)
    }

    suspend fun renameItem(item: RankedItemEntity, newName: String) =
        itemDao.updateItem(item.copy(name = newName.trim()))

    suspend fun deleteItem(item: RankedItemEntity) {
        itemDao.deleteItem(item)
        touchList(item.listId)
    }

    suspend fun updateItemColor(item: RankedItemEntity, color: String?) =
        itemDao.updateItemColor(item.id, color)

    /** Persist a full reordered list, updating each item's position index. */
    suspend fun reorderItems(items: List<RankedItemEntity>) {
        val updated = items.mapIndexed { index, item -> item.copy(position = index) }
        itemDao.updateItems(updated)
        if (items.isNotEmpty()) touchList(items.first().listId)
    }

    // ── Tags ───────────────────────────────────────────────────────────────

    fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

    fun getTagsForItem(itemId: Long): Flow<List<TagEntity>> = tagDao.getTagsForItem(itemId)

    suspend fun setTagsForItem(itemId: Long, tagNames: List<String>) {
        tagDao.clearTagsForItem(itemId)
        tagNames.forEach { name ->
            val trimmed = name.trim().lowercase()
            if (trimmed.isBlank()) return@forEach
            val existing = tagDao.getTagByName(trimmed)
            val tagId = existing?.id ?: tagDao.insertTag(TagEntity(name = trimmed))
            tagDao.insertCrossRef(ItemTagCrossRef(itemId, tagId))
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private suspend fun touchList(listId: Long) {
        listDao.getListById(listId)?.let {
            listDao.updateList(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }
}
