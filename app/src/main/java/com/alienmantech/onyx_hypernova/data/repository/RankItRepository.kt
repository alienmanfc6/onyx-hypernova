package com.alienmantech.onyx_hypernova.data.repository

import androidx.room.withTransaction
import com.alienmantech.onyx_hypernova.data.backup.BackupFile
import com.alienmantech.onyx_hypernova.data.backup.BackupItem
import com.alienmantech.onyx_hypernova.data.backup.BackupList
import com.alienmantech.onyx_hypernova.data.db.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RankItRepository @Inject constructor(
    private val db: RankItDatabase,
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
            val trimmed = name.trim()
            if (trimmed.isBlank()) return@forEach
            val existing = tagDao.getTagByName(trimmed)
            val tagId = existing?.id ?: tagDao.insertTag(TagEntity(name = trimmed))
            tagDao.insertCrossRef(ItemTagCrossRef(itemId, tagId))
        }
    }

    // ── Backup ─────────────────────────────────────────────────────────────

    suspend fun exportBackup(): BackupFile {
        val lists = listDao.getAllListsOnce()
        return BackupFile(lists = lists.map { list ->
            val items = itemDao.getItemsForListOnce(list.id)
            BackupList(
                name = list.name,
                createdAt = list.createdAt,
                updatedAt = list.updatedAt,
                items = items.map { item ->
                    BackupItem(
                        name = item.name,
                        position = item.position,
                        color = item.color,
                        tags = tagDao.getTagsForItemOnce(item.id).map { it.name }
                    )
                }
            )
        })
    }

    suspend fun importBackup(backup: BackupFile) = db.withTransaction {
        listDao.deleteAllLists()
        tagDao.deleteAllTags()
        backup.lists.forEach { bl ->
            val listId = listDao.insertList(
                RankedListEntity(name = bl.name, createdAt = bl.createdAt, updatedAt = bl.updatedAt)
            )
            bl.items.forEach { bi ->
                val itemId = itemDao.insertItem(
                    RankedItemEntity(listId = listId, name = bi.name, position = bi.position, color = bi.color)
                )
                setTagsForItem(itemId, bi.tags)
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private suspend fun touchList(listId: Long) {
        listDao.getListById(listId)?.let {
            listDao.updateList(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }
}
