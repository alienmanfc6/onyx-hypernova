package com.alienmantech.onyx_hypernova.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alienmantech.onyx_hypernova.data.db.RankedListEntity
import com.alienmantech.onyx_hypernova.ui.components.ConfirmDeleteDialog
import com.alienmantech.onyx_hypernova.ui.components.TextInputDialog
import com.alienmantech.onyx_hypernova.ui.theme.notePadInkColor
import com.alienmantech.onyx_hypernova.ui.theme.notePadLineColor
import com.alienmantech.onyx_hypernova.ui.theme.notePadPageColor
import com.alienmantech.onyx_hypernova.ui.theme.notePadSurfaceColor
import com.alienmantech.onyx_hypernova.ui.theme.notePadToolbarColor
import com.alienmantech.onyx_hypernova.viewmodel.HomeViewModel
import com.alienmantech.onyx_hypernova.viewmodel.ListWithCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onListClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val toolbarColor = notePadToolbarColor()
    val inkColor = notePadInkColor()
    val pageColor = notePadPageColor()
    val lineColor = notePadLineColor()
    val surfaceColor = notePadSurfaceColor()

    var showCreateDialog by remember { mutableStateOf(false) }
    var listToRename by remember { mutableStateOf<RankedListEntity?>(null) }
    var listToDelete by remember { mutableStateOf<RankedListEntity?>(null) }

    Scaffold(
        containerColor = pageColor,
        topBar = {
            TopAppBar(
                title = { Text("RankIt", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = toolbarColor,
                    titleContentColor = inkColor,
                    actionIconContentColor = inkColor
                ),
                actions = {
                    IconButton(onClick = { viewModel.toggleDarkMode() }) {
                        Icon(
                            imageVector = if (state.isDarkMode) Icons.Outlined.LightMode
                                          else Icons.Outlined.DarkMode,
                            contentDescription = "Toggle theme"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New List") },
                containerColor = toolbarColor,
                contentColor = inkColor
            )
        }
    ) { padding ->

        if (state.lists.isEmpty()) {
            EmptyHomeState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(pageColor),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 88.dp
                )
            ) {
                items(state.lists, key = { it.list.id }) { item ->
                    ListRow(
                        item = item,
                        onClick = { onListClick(item.list.id) },
                        onRename = { listToRename = item.list },
                        onDelete = { listToDelete = item.list },
                        inkColor = inkColor,
                        surfaceColor = surfaceColor
                    )
                    HorizontalDivider(color = lineColor, thickness = 1.dp)
                }
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────

    if (showCreateDialog) {
        TextInputDialog(
            title = "New List",
            placeholder = "List name",
            confirmLabel = "Create",
            onConfirm = { name ->
                viewModel.createList(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    listToRename?.let { list ->
        TextInputDialog(
            title = "Rename List",
            placeholder = "List name",
            initialValue = list.name,
            confirmLabel = "Rename",
            onConfirm = { newName ->
                viewModel.renameList(list, newName)
                listToRename = null
            },
            onDismiss = { listToRename = null }
        )
    }

    listToDelete?.let { list ->
        ConfirmDeleteDialog(
            title = "Delete List",
            body = "Delete \"${list.name}\" and all its items? This cannot be undone.",
            onConfirm = {
                viewModel.deleteList(list)
                listToDelete = null
            },
            onDismiss = { listToDelete = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListRow(
    item: ListWithCount,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    inkColor: androidx.compose.ui.graphics.Color,
    surfaceColor: androidx.compose.ui.graphics.Color
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.list.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = inkColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.itemCount} ${if (item.itemCount == 1) "item" else "items"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = inkColor.copy(alpha = 0.7f)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = inkColor.copy(alpha = 0.7f)
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            containerColor = surfaceColor
        ) {
            DropdownMenuItem(
                text = { Text("Rename") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = { showMenu = false; onRename() }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = { showMenu = false; onDelete() }
            )
        }
    }
}

@Composable
private fun EmptyHomeState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(notePadPageColor()),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.FormatListNumbered,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No lists yet",
                style = MaterialTheme.typography.titleMedium,
                color = notePadInkColor()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Tap + to create your first ranking list",
                style = MaterialTheme.typography.bodyMedium,
                color = notePadInkColor().copy(alpha = 0.7f)
            )
        }
    }
}
