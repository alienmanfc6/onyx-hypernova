package com.alienmantech.onyx_hypernova.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alienmantech.onyx_hypernova.data.db.RankedItemEntity
import com.alienmantech.onyx_hypernova.data.db.RankedListEntity
import com.alienmantech.onyx_hypernova.data.db.TagEntity
import com.alienmantech.onyx_hypernova.data.repository.ListUiPreferencesRepository
import com.alienmantech.onyx_hypernova.data.repository.RankItRepository
import com.alienmantech.onyx_hypernova.data.repository.RankItRepository.ItemTransferResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListDetailUiState(
    val list: RankedListEntity? = null,
    val items: List<RankedItemEntity> = emptyList(),
    val searchQuery: String = "",
    val filteredItems: List<RankedItemEntity> = emptyList(),
    val isSearchActive: Boolean = false,
    val itemTags: Map<Long, List<TagEntity>> = emptyMap(),
    val allTags: List<TagEntity> = emptyList(),
    val groupedSections: List<TagGroupedItemsSection> = emptyList(),
    val isGroupedByTag: Boolean = false,
    val availableLists: List<TransferListOption> = emptyList(),
    val transferErrorMessage: String? = null,
    val transferSuccessToken: Int = 0
)

data class ReorderFeedback(
    val message: String,
    val undoLabel: String = "Undo"
)

data class TagGroupedItemsSection(
    val header: String,
    val items: List<RankedItemEntity>
)

data class TransferListOption(
    val list: RankedListEntity,
    val itemCount: Int
)

@HiltViewModel
class ListDetailViewModel @Inject constructor(
    private val repo: RankItRepository,
    private val listUiPreferencesRepository: ListUiPreferencesRepository,
    savedState: SavedStateHandle
) : ViewModel() {

    private val listId: Long = requireNotNull(savedState.get<Long>("listId")) {
        "listId nav argument is required"
    }

    // Mutable local copy for optimistic drag-and-drop updates
    private val _localItems = MutableStateFlow<List<RankedItemEntity>?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _transferErrorMessage = MutableStateFlow<String?>(null)
    private val _transferSuccessToken = MutableStateFlow(0)
    private val _reorderFeedback = MutableSharedFlow<ReorderFeedback>(extraBufferCapacity = 1)
    private val listFlow = flow { emit(repo.getListById(listId)) }
    private val groupByTagFlow = listUiPreferencesRepository.isGroupByTag(listId)
    private var dragStartItemId: Long? = null
    private var dragStartItems: List<RankedItemEntity>? = null
    private var pendingUndoItems: List<RankedItemEntity>? = null

    private val dbItems: Flow<List<RankedItemEntity>> = repo.getItemsForList(listId)
    private val availableListsFlow: Flow<List<TransferListOption>> = repo.getAllLists()
        .map { lists -> lists.filter { it.id != listId } }
        .flatMapLatest { lists ->
            if (lists.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(lists.map { list ->
                    repo.getItemCount(list.id).map { count -> TransferListOption(list, count) }
                }) { options -> options.toList() }
            }
        }

    // For each item, collect its tags and merge into a map
    private val itemTagsFlow: Flow<Map<Long, List<TagEntity>>> = dbItems
        .flatMapLatest { items ->
            if (items.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(items.map { item ->
                    repo.getTagsForItem(item.id).map { tags -> item.id to tags }
                }) { pairs -> pairs.toMap() }
            }
        }

    private val baseUiStateFlow: Flow<ListDetailUiState> = combine(
        dbItems,
        _localItems,
        listFlow,
        itemTagsFlow,
        repo.getAllTags()
    ) { dbItemsArr, local, list, itemTags, allTags ->
        val resolvedItems = local ?: dbItemsArr
        ListDetailUiState(
            list = list,
            items = resolvedItems,
            itemTags = itemTags,
            allTags = allTags,
            groupedSections = buildGroupedSections(
                items = resolvedItems,
                itemTags = itemTags
            )
        )
    }.combine(_searchQuery) { baseState, searchQuery ->
        val normalizedQuery = searchQuery.trim()
        val isSearchActive = normalizedQuery.isNotEmpty()
        val filteredItems = if (!isSearchActive) {
            baseState.items
        } else {
            baseState.items.filter { item ->
                item.name.contains(normalizedQuery, ignoreCase = true) ||
                    baseState.itemTags[item.id].orEmpty().any { tag ->
                        tag.name.contains(normalizedQuery, ignoreCase = true)
                    }
            }
        }
        baseState.copy(
            searchQuery = searchQuery,
            filteredItems = filteredItems,
            isSearchActive = isSearchActive
        )
    }

    val uiState: StateFlow<ListDetailUiState> = combine(
        baseUiStateFlow,
        groupByTagFlow,
        availableListsFlow,
        _transferErrorMessage,
        _transferSuccessToken
    ) { baseState, isGroupedByTag, availableLists, transferErrorMessage, transferSuccessToken ->
        baseState.copy(
            isGroupedByTag = isGroupedByTag,
            availableLists = availableLists,
            transferErrorMessage = transferErrorMessage,
            transferSuccessToken = transferSuccessToken
        )
    }
        .catch { /* TODO: surface DB errors to UI */ }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListDetailUiState())

    val reorderFeedback: SharedFlow<ReorderFeedback> = _reorderFeedback.asSharedFlow()

    fun addItem(name: String, initialRank: Int, tags: List<String> = emptyList()) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val insertPosition = (initialRank - 1).coerceIn(0, uiState.value.items.size)
            repo.addItem(listId, name, insertPosition, tags)
        }
    }

    fun hasItemNamed(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return false
        return uiState.value.items.any { it.name.equals(trimmed, ignoreCase = true) }
    }

    fun renameItem(item: RankedItemEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { repo.renameItem(item, newName) }
    }

    fun deleteItem(item: RankedItemEntity) {
        viewModelScope.launch { repo.deleteItem(item) }
    }

    fun updateItemColor(item: RankedItemEntity, color: String?) {
        viewModelScope.launch { repo.updateItemColor(item, color) }
    }

    fun updateItemTags(item: RankedItemEntity, tags: List<String>) {
        viewModelScope.launch { repo.setTagsForItem(item.id, tags) }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun copyItemToList(item: RankedItemEntity, destinationListId: Long, destinationRank: Int) {
        viewModelScope.launch {
            _transferErrorMessage.value = null
            val result = repo.copyItemToList(
                itemId = item.id,
                destinationListId = destinationListId,
                destinationRank = destinationRank
            )
            when (result) {
                ItemTransferResult.Success -> _transferSuccessToken.value += 1
                else -> _transferErrorMessage.value = result.toErrorMessage()
            }
        }
    }

    fun moveItemToList(item: RankedItemEntity, destinationListId: Long, destinationRank: Int) {
        viewModelScope.launch {
            _transferErrorMessage.value = null
            val result = repo.moveItemToList(
                itemId = item.id,
                destinationListId = destinationListId,
                destinationRank = destinationRank
            )
            when (result) {
                ItemTransferResult.Success -> _transferSuccessToken.value += 1
                else -> _transferErrorMessage.value = result.toErrorMessage()
            }
        }
    }

    fun clearTransferError() {
        _transferErrorMessage.value = null
    }

    fun setGroupByTag(enabled: Boolean) {
        viewModelScope.launch {
            listUiPreferencesRepository.setGroupByTag(listId, enabled)
        }
    }

    /** Called on every drag move — updates local UI immediately. */
    fun onDragMove(reordered: List<RankedItemEntity>) {
        _localItems.value = reordered
    }

    fun onDragStarted(itemId: Long, currentItems: List<RankedItemEntity>) {
        dragStartItemId = itemId
        dragStartItems = currentItems
    }

    /** Called when drag is released — persist to DB and clear local override. */
    fun onDragEnd(reordered: List<RankedItemEntity>) {
        viewModelScope.launch {
            val originalItems = dragStartItems
            val draggedItemId = dragStartItemId
            dragStartItems = null
            dragStartItemId = null

            if (originalItems == null || draggedItemId == null) {
                repo.reorderItems(reordered)
                _localItems.value = null
                return@launch
            }

            val originalIndex = originalItems.indexOfFirst { it.id == draggedItemId }
            val newIndex = reordered.indexOfFirst { it.id == draggedItemId }
            val draggedItem = reordered.firstOrNull { it.id == draggedItemId }

            if (originalIndex == -1 || newIndex == -1 || draggedItem == null) {
                repo.reorderItems(reordered)
                _localItems.value = null
                return@launch
            }

            if (originalIndex == newIndex) {
                pendingUndoItems = null
                _localItems.value = null
                return@launch
            }

            repo.reorderItems(reordered)
            pendingUndoItems = originalItems
            _localItems.value = null
            _reorderFeedback.emit(
                ReorderFeedback(message = "Moved \"${draggedItem.name}\" to #${newIndex + 1}")
            )
        }
    }

    fun undoLastReorder() {
        val snapshot = pendingUndoItems ?: return
        viewModelScope.launch {
            repo.reorderItems(snapshot)
            pendingUndoItems = null
        }
    }

    private fun buildGroupedSections(
        items: List<RankedItemEntity>,
        itemTags: Map<Long, List<TagEntity>>
    ): List<TagGroupedItemsSection> {
        if (items.isEmpty()) return emptyList()

        val groupedItems = linkedMapOf<String, MutableList<RankedItemEntity>>()

        items.forEach { item ->
            val tags = itemTags[item.id].orEmpty()
            if (tags.isEmpty()) {
                groupedItems.getOrPut(UNTAGGED_HEADER) { mutableListOf() }.add(item)
            } else {
                tags.forEach { tag ->
                    groupedItems.getOrPut(tag.name) { mutableListOf() }.add(item)
                }
            }
        }

        return groupedItems.entries
            .sortedWith(compareBy<Map.Entry<String, MutableList<RankedItemEntity>>>(
                { it.key == UNTAGGED_HEADER },
                { it.key.lowercase() }
            ))
            .map { (header, groupedSectionItems) ->
                TagGroupedItemsSection(
                    header = header,
                    items = groupedSectionItems
                )
            }
    }

    private companion object {
        const val UNTAGGED_HEADER = "Untagged"
    }

    private fun ItemTransferResult.toErrorMessage(): String? = when (this) {
        ItemTransferResult.Success -> null
        ItemTransferResult.DuplicateName -> "That item is already on the selected list."
        ItemTransferResult.InvalidDestination -> "Choose a different destination list."
        ItemTransferResult.ItemNotFound -> "That item could not be found."
    }
}
