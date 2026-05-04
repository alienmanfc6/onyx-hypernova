package com.alienmantech.onyx_hypernova.ui.listdetail

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alienmantech.onyx_hypernova.data.db.RankedItemEntity
import com.alienmantech.onyx_hypernova.data.db.TagEntity
import com.alienmantech.onyx_hypernova.ui.components.AddItemWithTagsDialog
import com.alienmantech.onyx_hypernova.ui.components.ConfirmDeleteDialog
import com.alienmantech.onyx_hypernova.ui.components.TagPickerDialog
import com.alienmantech.onyx_hypernova.ui.components.TextInputDialog
import com.alienmantech.onyx_hypernova.ui.theme.colorPalette
import com.alienmantech.onyx_hypernova.ui.theme.notePadHighlightColor
import com.alienmantech.onyx_hypernova.ui.theme.notePadInkColor
import com.alienmantech.onyx_hypernova.ui.theme.notePadLineColor
import com.alienmantech.onyx_hypernova.ui.theme.notePadPageColor
import com.alienmantech.onyx_hypernova.ui.theme.notePadSurfaceColor
import com.alienmantech.onyx_hypernova.ui.theme.notePadToolbarColor
import com.alienmantech.onyx_hypernova.viewmodel.ListDetailViewModel
import com.alienmantech.onyx_hypernova.viewmodel.TagGroupedItemsSection
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ListDetailScreen(
    onBack: () -> Unit,
    onManageTags: () -> Unit,
    viewModel: ListDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val toolbarColor = notePadToolbarColor()
    val inkColor = notePadInkColor()
    val pageColor = notePadPageColor()
    val lineColor = notePadLineColor()

    var showTags by rememberSaveable { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addItemError by remember { mutableStateOf<String?>(null) }
    var itemToRename by remember { mutableStateOf<RankedItemEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<RankedItemEntity?>(null) }
    var itemToRecolor by remember { mutableStateOf<RankedItemEntity?>(null) }
    var itemToEditTags by remember { mutableStateOf<RankedItemEntity?>(null) }
    var itemWithMenu by remember { mutableStateOf<RankedItemEntity?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var isGroupedByTag by rememberSaveable { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()
    val rankByItemId = remember(state.items) {
        state.items.mapIndexed { index, item -> item.id to (index + 1) }.toMap()
    }

    val lazyListState = rememberLazyListState()
    var isDragging by remember { mutableStateOf(false) }
    var dragPointerY by remember { mutableStateOf(Float.NaN) }
    var listViewportHeightPx by remember { mutableIntStateOf(0) }
    val edgeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
    val maxAutoScrollStepPx = with(LocalDensity.current) { 28.dp.toPx() }
    val reorderState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            val mutable = state.items.toMutableList()
            mutable.add(to.index, mutable.removeAt(from.index))
            viewModel.onDragMove(mutable)
        }
    )

    LaunchedEffect(isDragging, dragPointerY, listViewportHeightPx) {
        while (isDragging) {
            val delta = autoScrollDelta(
                pointerY = dragPointerY,
                viewportHeightPx = listViewportHeightPx,
                edgeThresholdPx = edgeThresholdPx,
                maxStepPx = maxAutoScrollStepPx
            )

            if (delta != 0f) {
                lazyListState.scrollBy(delta)
            }

            delay(16)
        }
    }

    Scaffold(
        containerColor = pageColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.list?.name ?: "",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (showTags) "Hide Tags" else "Show Tags") },
                                leadingIcon = {
                                    Icon(
                                        if (showTags) Icons.Default.LabelOff else Icons.Default.Label,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showTags = !showTags
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Manage Tags") },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onManageTags()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isGroupedByTag) "Show Flat List" else "Group by Tag"
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (isGroupedByTag) Icons.Default.ViewAgenda else Icons.Default.ViewStream,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    isGroupedByTag = !isGroupedByTag
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = toolbarColor,
                    titleContentColor = inkColor,
                    actionIconContentColor = inkColor,
                    navigationIconContentColor = inkColor
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = toolbarColor,
                contentColor = inkColor
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        }
    ) { padding ->

        if (state.items.isEmpty()) {
            EmptyListState(modifier = Modifier.padding(padding))
        } else if (isGroupedByTag) {
            GroupedItemsList(
                sections = state.groupedSections,
                rankByItemId = rankByItemId,
                itemTags = state.itemTags,
                showTags = showTags,
                pageColor = pageColor,
                lineColor = lineColor,
                inkColor = inkColor,
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 88.dp
                ),
                onItemTap = { itemWithMenu = it }
            )
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { listViewportHeightPx = it.height }
                    .pointerInput(isDragging) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (!isDragging) continue

                                val activePointer = event.changes.firstOrNull { it.pressed }
                                if (activePointer != null) {
                                    dragPointerY = activePointer.position.y
                                }
                            }
                        }
                    }
                    .background(pageColor),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 88.dp
                )
            ) {
                itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
                    ReorderableItem(reorderState, key = item.id) { isItemDragging ->
                        Column {
                            RankedItemRow(
                                item = item,
                                rank = index + 1,
                                tags = if (showTags) state.itemTags[item.id].orEmpty() else emptyList(),
                                isDragging = isItemDragging,
                                dragHandle = {
                                    Icon(
                                        Icons.Default.DragHandle,
                                        contentDescription = "Drag to reorder",
                                        tint = inkColor.copy(alpha = 0.5f),
                                        modifier = Modifier.draggableHandle(
                                            onDragStarted = {
                                                isDragging = true
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onDragStopped = {
                                                isDragging = false
                                                dragPointerY = Float.NaN
                                                viewModel.onDragEnd(state.items)
                                            }
                                        )
                                    )
                                },
                                onTap = { itemWithMenu = item },
                                inkColor = inkColor
                            )
                            HorizontalDivider(color = lineColor, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }

    // ── Item options bottom sheet ──────────────────────────────────────────

    itemWithMenu?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { itemWithMenu = null },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ListItem(
                headlineContent = { Text("Rename") },
                leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                modifier = Modifier.clickable {
                    itemWithMenu = null
                    itemToRename = item
                }
            )
            ListItem(
                headlineContent = { Text("Change Color") },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                color = if (item.color != null)
                                    runCatching { Color(AndroidColor.parseColor(item.color)) }
                                        .getOrDefault(Color.Gray)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                },
                modifier = Modifier.clickable {
                    itemWithMenu = null
                    itemToRecolor = item
                }
            )
            ListItem(
                headlineContent = { Text("Edit Tags") },
                leadingContent = { Icon(Icons.Default.Label, contentDescription = null) },
                modifier = Modifier.clickable {
                    itemWithMenu = null
                    itemToEditTags = item
                }
            )
            ListItem(
                headlineContent = {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.clickable {
                    itemWithMenu = null
                    itemToDelete = item
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────

    if (showAddDialog) {
        AddItemWithTagsDialog(
            allTags = state.allTags.map { it.name },
            currentItemCount = state.items.size,
            errorMessage = addItemError,
            onNameChange = { addItemError = null },
            onConfirm = { name, tags, initialRank ->
                if (viewModel.hasItemNamed(name)) {
                    addItemError = "That item is already on this list."
                    return@AddItemWithTagsDialog
                }
                viewModel.addItem(name, initialRank, tags)
                addItemError = null
                showAddDialog = false
            },
            onDismiss = {
                addItemError = null
                showAddDialog = false
            }
        )
    }

    itemToRename?.let { item ->
        TextInputDialog(
            title = "Rename Item",
            placeholder = "Item name",
            initialValue = item.name,
            confirmLabel = "Rename",
            onConfirm = { newName ->
                viewModel.renameItem(item, newName)
                itemToRename = null
            },
            onDismiss = { itemToRename = null }
        )
    }

    itemToDelete?.let { item ->
        ConfirmDeleteDialog(
            title = "Delete Item",
            body = "Remove \"${item.name}\" from this list?",
            onConfirm = {
                viewModel.deleteItem(item)
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }

    itemToRecolor?.let { item ->
        ColorPickerDialog(
            currentColor = item.color,
            onColorSelected = { color ->
                viewModel.updateItemColor(item, color)
                itemToRecolor = null
            },
            onDismiss = { itemToRecolor = null }
        )
    }

    itemToEditTags?.let { item ->
        TagPickerDialog(
            currentTags = state.itemTags[item.id].orEmpty().map { it.name },
            allTags = state.allTags.map { it.name },
            onConfirm = { tags ->
                viewModel.updateItemTags(item, tags)
                itemToEditTags = null
            },
            onDismiss = { itemToEditTags = null }
        )
    }
}

private fun autoScrollDelta(
    pointerY: Float,
    viewportHeightPx: Int,
    edgeThresholdPx: Float,
    maxStepPx: Float
): Float {
    if (pointerY.isNaN() || viewportHeightPx <= 0) return 0f

    val topEdgeDistance = pointerY
    if (topEdgeDistance < edgeThresholdPx) {
        val progress = ((edgeThresholdPx - topEdgeDistance) / edgeThresholdPx).coerceIn(0f, 1f)
        return -maxStepPx * progress
    }

    val bottomEdgeStart = viewportHeightPx - edgeThresholdPx
    if (pointerY > bottomEdgeStart) {
        val progress = ((pointerY - bottomEdgeStart) / edgeThresholdPx).coerceIn(0f, 1f)
        return maxStepPx * progress
    }

    return 0f
}

@Composable
private fun ColorPickerDialog(
    currentColor: String?,
    onColorSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Choose Color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                colorPalette.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { colorHex ->
                            val isSelected = colorHex == currentColor
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        color = if (colorHex != null)
                                            runCatching { Color(AndroidColor.parseColor(colorHex)) }
                                                .getOrDefault(Color.Gray)
                                        else
                                            Color.Transparent
                                    )
                                    .border(
                                        width = if (isSelected) 3.dp else if (colorHex == null) 1.5.dp else 0.dp,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable { onColorSelected(colorHex) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (colorHex == null) {
                                    Icon(
                                        Icons.Default.Block,
                                        contentDescription = "No color",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = notePadInkColor()) }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RankedItemRow(
    item: RankedItemEntity,
    rank: Int,
    tags: List<TagEntity>,
    isDragging: Boolean,
    dragHandle: (@Composable () -> Unit)?,
    onTap: () -> Unit,
    inkColor: Color
) {
    val parsedItemColor = remember(item.color) {
        item.color?.let { hex -> runCatching { Color(AndroidColor.parseColor(hex)) }.getOrDefault(Color.Gray) }
    }
    val bgColor = when {
        isDragging -> notePadHighlightColor()
        parsedItemColor != null -> parsedItemColor
        else -> notePadPageColor()
    }
    val chipColors = SuggestionChipDefaults.suggestionChipColors(
        containerColor = notePadSurfaceColor()
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onTap)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = inkColor
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier.widthIn(max = 112.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.forEach { tag ->
                        SuggestionChip(
                            onClick = {},
                            colors = chipColors,
                            modifier = Modifier.defaultMinSize(minHeight = 24.dp),
                            border = null,
                            label = {
                                Text(
                                    tag.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = inkColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))
            dragHandle?.invoke()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupedItemsList(
    sections: List<TagGroupedItemsSection>,
    rankByItemId: Map<Long, Int>,
    itemTags: Map<Long, List<TagEntity>>,
    showTags: Boolean,
    pageColor: Color,
    lineColor: Color,
    inkColor: Color,
    contentPadding: PaddingValues,
    onItemTap: (RankedItemEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(pageColor),
        contentPadding = contentPadding
    ) {
        sections.forEach { section ->
            stickyHeader(key = "header-${section.header}") {
                TagGroupHeader(
                    title = section.header,
                    count = section.items.size,
                    pageColor = pageColor,
                    lineColor = lineColor,
                    inkColor = inkColor
                )
            }

            itemsIndexed(
                items = section.items,
                key = { _, item -> "${section.header}-${item.id}" }
            ) { _, item ->
                Column {
                    RankedItemRow(
                        item = item,
                        rank = rankByItemId[item.id] ?: 0,
                        tags = if (showTags) itemTags[item.id].orEmpty() else emptyList(),
                        isDragging = false,
                        dragHandle = null,
                        onTap = { onItemTap(item) },
                        inkColor = inkColor
                    )
                    HorizontalDivider(color = lineColor, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun TagGroupHeader(
    title: String,
    count: Int,
    pageColor: Color,
    lineColor: Color,
    inkColor: Color
) {
    val headerBackground = notePadSurfaceColor()

    Surface(
        color = headerBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBackground)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = inkColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "($count)",
                    style = MaterialTheme.typography.bodySmall,
                    color = inkColor.copy(alpha = 0.7f)
                )
            }
            HorizontalDivider(color = lineColor, thickness = 1.dp)
        }
    }
}

@Composable
private fun EmptyListState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(notePadPageColor()),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.PlaylistAdd,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No items yet",
                style = MaterialTheme.typography.titleMedium,
                color = notePadInkColor()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Tap + to add your first item",
                style = MaterialTheme.typography.bodyMedium,
                color = notePadInkColor().copy(alpha = 0.7f)
            )
        }
    }
}
