package com.alienmantech.onyx_hypernova.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.alienmantech.onyx_hypernova.data.db.TagEntity
import com.alienmantech.onyx_hypernova.data.db.TagSummary
import com.alienmantech.onyx_hypernova.data.repository.TagRenameResult
import com.alienmantech.onyx_hypernova.ui.components.ConfirmDeleteDialog
import com.alienmantech.onyx_hypernova.ui.components.TextInputDialog
import com.alienmantech.onyx_hypernova.ui.theme.notePadInkColor
import com.alienmantech.onyx_hypernova.ui.theme.notePadLineColor
import com.alienmantech.onyx_hypernova.ui.theme.notePadPageColor
import com.alienmantech.onyx_hypernova.ui.theme.notePadSurfaceColor
import com.alienmantech.onyx_hypernova.ui.theme.notePadToolbarColor
import com.alienmantech.onyx_hypernova.viewmodel.ManageTagsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTagsScreen(
    onBack: () -> Unit,
    viewModel: ManageTagsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val toolbarColor = notePadToolbarColor()
    val inkColor = notePadInkColor()
    val pageColor = notePadPageColor()
    val lineColor = notePadLineColor()
    val surfaceColor = notePadSurfaceColor()
    val fieldColor = MaterialTheme.colorScheme.surfaceVariant

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf<String?>(null) }
    var tagToRename by remember { mutableStateOf<TagSummary?>(null) }
    var renameError by remember { mutableStateOf<String?>(null) }
    var tagToDelete by remember { mutableStateOf<TagSummary?>(null) }
    var pendingMerge by remember { mutableStateOf<PendingTagMerge?>(null) }

    val filteredTags = state.tags.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }

    Scaffold(
        containerColor = pageColor,
        topBar = {
            TopAppBar(
                title = { Text("Manage Tags", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        addError = null
                        showAddDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add tag")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = toolbarColor,
                    titleContentColor = inkColor,
                    navigationIconContentColor = inkColor,
                    actionIconContentColor = inkColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(pageColor)
                .padding(top = padding.calculateTopPadding())
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search tags") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Search
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = fieldColor,
                    unfocusedContainerColor = fieldColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            when {
                state.tags.isEmpty() -> {
                    EmptyTagsState(
                        message = "No tags yet",
                        modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
                    )
                }

                filteredTags.isEmpty() -> {
                    EmptyTagsState(
                        message = "No matching tags",
                        modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 24.dp)
                    ) {
                        items(filteredTags, key = { it.id }) { tag ->
                            TagSummaryRow(
                                tag = tag,
                                inkColor = inkColor,
                                surfaceColor = surfaceColor,
                                onRename = {
                                    renameError = null
                                    tagToRename = tag
                                },
                                onDelete = { tagToDelete = tag }
                            )
                            HorizontalDivider(color = lineColor, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        TextInputDialog(
            title = "Add Tag",
            placeholder = "Tag name",
            confirmLabel = "Add",
            errorMessage = addError,
            onValueChange = { addError = null },
            onConfirm = { name ->
                val trimmed = name.trim()
                when {
                    trimmed.isBlank() -> addError = "Tag name can't be blank."
                    viewModel.hasTagNamed(trimmed) -> addError = "That tag already exists."
                    else -> viewModel.createTag(trimmed) {
                        showAddDialog = false
                        addError = null
                    }
                }
            },
            onDismiss = {
                addError = null
                showAddDialog = false
            }
        )
    }

    tagToRename?.let { tag ->
        TextInputDialog(
            title = "Rename Tag",
            placeholder = "Tag name",
            initialValue = tag.name,
            confirmLabel = "Rename",
            errorMessage = renameError,
            onValueChange = { renameError = null },
            onConfirm = { newName ->
                val trimmed = newName.trim()
                if (trimmed.isBlank()) {
                    renameError = "Tag name can't be blank."
                    return@TextInputDialog
                }

                viewModel.renameTag(tag.id, trimmed) { result ->
                    when (result) {
                        TagRenameResult.Renamed -> {
                            renameError = null
                            tagToRename = null
                        }

                        TagRenameResult.InvalidName -> {
                            renameError = "Tag name can't be blank."
                        }

                        TagRenameResult.NotFound -> {
                            renameError = "That tag no longer exists."
                        }

                        is TagRenameResult.RequiresMerge -> {
                            pendingMerge = PendingTagMerge(
                                source = result.source,
                                target = result.target
                            )
                            tagToRename = null
                        }
                    }
                }
            },
            onDismiss = {
                renameError = null
                tagToRename = null
            }
        )
    }

    tagToDelete?.let { tag ->
        ConfirmDeleteDialog(
            title = "Delete Tag",
            body = "Delete \"${tag.name}\" from ${formatItemCount(tag.usageCount)}?",
            onConfirm = {
                viewModel.deleteTag(tag.id) {
                    tagToDelete = null
                }
            },
            onDismiss = { tagToDelete = null }
        )
    }

    pendingMerge?.let { merge ->
        MergeTagsDialog(
            sourceName = merge.source.name,
            targetName = merge.target.name,
            onConfirm = {
                viewModel.mergeTags(merge.source.id, merge.target.id) {
                    pendingMerge = null
                    renameError = null
                    tagToRename = null
                }
            },
            onDismiss = {
                pendingMerge = null
                tagToRename = state.tags.firstOrNull { it.id == merge.source.id }?.let { it }
            }
        )
    }
}

@Composable
private fun TagSummaryRow(
    tag: TagSummary,
    inkColor: androidx.compose.ui.graphics.Color,
    surfaceColor: androidx.compose.ui.graphics.Color,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(tag.name, style = MaterialTheme.typography.titleMedium, color = inkColor)
            Text(
                formatItemCount(tag.usageCount),
                style = MaterialTheme.typography.bodySmall,
                color = inkColor.copy(alpha = 0.75f)
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Tag options", tint = inkColor)
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = surfaceColor
            ) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onRename()
                    }
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
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyTagsState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(message, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun MergeTagsDialog(
    sourceName: String,
    targetName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val inkColor = notePadInkColor()

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Merge Tags") },
        text = { Text("Merge \"$sourceName\" into \"$targetName\"?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Merge", color = inkColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = inkColor)
            }
        }
    )
}

private fun formatItemCount(count: Int): String =
    if (count == 1) "1 item" else "$count items"

private data class PendingTagMerge(
    val source: TagEntity,
    val target: TagEntity
)
