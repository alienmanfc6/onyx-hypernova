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
    enum class ItemTransferMode {
        COPY,
        MOVE
    }

    sealed interface ItemTransferResult {
        data object Success : ItemTransferResult
        data object ItemNotFound : ItemTransferResult
        data object InvalidDestination : ItemTransferResult
        data object DuplicateName : ItemTransferResult
    }

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

    suspend fun addItem(
        listId: Long,
        name: String,
        position: Int,
        tags: List<String> = emptyList()
    ) = db.withTransaction {
        val existingItems = itemDao.getItemsForListOnce(listId)
        val insertPosition = position.coerceIn(0, existingItems.size)
        val shiftedItems = existingItems.mapIndexed { index, item ->
            item.copy(position = if (index < insertPosition) index else index + 1)
        }

        itemDao.updateItems(shiftedItems)

        val id = itemDao.insertItem(
            RankedItemEntity(
                listId = listId,
                name = name.trim(),
                position = insertPosition
            )
        )
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

    suspend fun copyItemToList(
        itemId: Long,
        destinationListId: Long,
        destinationRank: Int
    ): ItemTransferResult = transferItem(
        itemId = itemId,
        destinationListId = destinationListId,
        destinationRank = destinationRank,
        mode = ItemTransferMode.COPY
    )

    suspend fun moveItemToList(
        itemId: Long,
        destinationListId: Long,
        destinationRank: Int
    ): ItemTransferResult = transferItem(
        itemId = itemId,
        destinationListId = destinationListId,
        destinationRank = destinationRank,
        mode = ItemTransferMode.MOVE
    )

    // ── Tags ───────────────────────────────────────────────────────────────

    fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

    fun getTagSummaries(): Flow<List<TagSummary>> = tagDao.getTagSummaries()

    fun getTagsForItem(itemId: Long): Flow<List<TagEntity>> = tagDao.getTagsForItem(itemId)

    suspend fun createTag(name: String): TagEntity? = db.withTransaction {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return@withTransaction null

        tagDao.getTagByName(trimmed)?.let { return@withTransaction it }

        val tagId = tagDao.insertTag(TagEntity(name = trimmed))
        tagDao.getTagById(tagId)
    }

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

    suspend fun renameTag(tagId: Long, newName: String): TagRenameResult = db.withTransaction {
        val sourceTag = tagDao.getTagById(tagId) ?: return@withTransaction TagRenameResult.NotFound
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return@withTransaction TagRenameResult.InvalidName

        if (sourceTag.name.equals(trimmed, ignoreCase = true)) {
            tagDao.updateTagName(tagId, trimmed)
            return@withTransaction TagRenameResult.Renamed
        }

        val existing = tagDao.getTagByName(trimmed)
        if (existing != null) {
            return@withTransaction TagRenameResult.RequiresMerge(
                source = sourceTag,
                target = existing
            )
        }

        tagDao.updateTagName(tagId, trimmed)
        TagRenameResult.Renamed
    }

    suspend fun mergeTags(sourceTagId: Long, targetTagId: Long) = db.withTransaction {
        if (sourceTagId == targetTagId) return@withTransaction

        val source = tagDao.getTagById(sourceTagId) ?: return@withTransaction
        val target = tagDao.getTagById(targetTagId) ?: return@withTransaction

        tagDao.moveCrossRefsToTag(source.id, target.id)
        tagDao.clearItemsForTag(source.id)
        tagDao.deleteTagById(source.id)
    }

    suspend fun deleteTag(tagId: Long) = db.withTransaction {
        tagDao.clearItemsForTag(tagId)
        tagDao.deleteTagById(tagId)
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

    private suspend fun transferItem(
        itemId: Long,
        destinationListId: Long,
        destinationRank: Int,
        mode: ItemTransferMode
    ): ItemTransferResult = db.withTransaction {
        val sourceItem = itemDao.getItemById(itemId) ?: return@withTransaction ItemTransferResult.ItemNotFound
        if (sourceItem.listId == destinationListId) {
            return@withTransaction ItemTransferResult.InvalidDestination
        }

        val destinationList = listDao.getListById(destinationListId)
            ?: return@withTransaction ItemTransferResult.InvalidDestination
        if (itemDao.hasItemNamed(destinationList.id, sourceItem.name.trim())) {
            return@withTransaction ItemTransferResult.DuplicateName
        }

        val destinationItems = itemDao.getItemsForListOnce(destinationList.id)
        val insertPosition = (destinationRank - 1).coerceIn(0, destinationItems.size)
        val shiftedItems = destinationItems.mapIndexed { index, item ->
            item.copy(position = if (index < insertPosition) index else index + 1)
        }
        itemDao.updateItems(shiftedItems)

        val newItemId = itemDao.insertItem(
            sourceItem.copy(
                id = 0,
                listId = destinationList.id,
                position = insertPosition
            )
        )
        val tags = tagDao.getTagsForItemOnce(sourceItem.id).map { it.name }
        setTagsForItem(newItemId, tags)

        if (mode == ItemTransferMode.MOVE) {
            itemDao.deleteItem(sourceItem)
            touchList(sourceItem.listId)
        }

        touchList(destinationList.id)
        ItemTransferResult.Success
    }

    private suspend fun touchList(listId: Long) {
        listDao.getListById(listId)?.let {
            listDao.updateList(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }
}

sealed interface TagRenameResult {
    data object Renamed : TagRenameResult
    data object InvalidName : TagRenameResult
    data object NotFound : TagRenameResult
    data class RequiresMerge(
        val source: TagEntity,
        val target: TagEntity
    ) : TagRenameResult
}
