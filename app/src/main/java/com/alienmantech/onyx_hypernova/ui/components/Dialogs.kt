package com.alienmantech.onyx_hypernova.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.alienmantech.onyx_hypernova.ui.theme.notePadInkColor
import com.alienmantech.onyx_hypernova.viewmodel.TransferListOption

/** Single-field text input dialog used for create / rename operations. */
@Composable
fun TextInputDialog(
    title: String,
    placeholder: String,
    initialValue: String = "",
    confirmLabel: String = "Confirm",
    errorMessage: String? = null,
    onValueChange: ((String) -> Unit)? = null,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }
    val inkColor = notePadInkColor()
    val dialogColor = MaterialTheme.colorScheme.surface
    val fieldColor = MaterialTheme.colorScheme.surfaceVariant

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogColor,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    onValueChange?.invoke(it)
                },
                placeholder = { Text(placeholder) },
                singleLine = true,
                isError = errorMessage != null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (text.isNotBlank()) onConfirm(text) }
                ),
                supportingText = errorMessage?.let { message ->
                    { Text(message) }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = fieldColor,
                    unfocusedContainerColor = fieldColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) { Text(confirmLabel, color = inkColor) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = inkColor) }
        }
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

/**
 * Tag picker content — selected chips, quick suggestions, and a text field for new tags.
 * Intended to be embedded in dialogs, not used standalone.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagPickerContent(
    selectedTags: List<String>,
    allTags: List<String>,
    onTagsChanged: (List<String>) -> Unit
) {
    var newTagText by remember { mutableStateOf("") }
    val inkColor = notePadInkColor()
    val fieldColor = MaterialTheme.colorScheme.surfaceVariant
    val chipColor = MaterialTheme.colorScheme.surfaceVariant
    val selectedTagsLower = selectedTags.map { it.lowercase() }.toSet()

    fun addTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isNotBlank() && trimmed.lowercase() !in selectedTagsLower) {
            onTagsChanged(selectedTags + trimmed)
        }
        newTagText = ""
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Selected tags
        if (selectedTags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                selectedTags.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { onTagsChanged(selectedTags - tag) },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = chipColor,
                            selectedContainerColor = chipColor,
                            selectedLabelColor = inkColor,
                            selectedTrailingIconColor = inkColor
                        ),
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove tag",
                                modifier = Modifier.size(InputChipDefaults.AvatarSize)
                            )
                        }
                    )
                }
            }
        }

        // Text field to enter a new tag
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newTagText,
                onValueChange = { newTagText = it },
                placeholder = { Text("New tag") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { addTag(newTagText) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = fieldColor,
                    unfocusedContainerColor = fieldColor
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { addTag(newTagText) },
                enabled = newTagText.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add tag")
            }
        }

        // Suggestions: previously used tags not already selected
        val suggestions = allTags.filter { it.lowercase() !in selectedTagsLower }
        if (suggestions.isNotEmpty()) {
            Text(
                "Suggestions",
                style = MaterialTheme.typography.labelSmall,
                color = inkColor.copy(alpha = 0.75f)
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                suggestions.forEach { tag ->
                    SuggestionChip(
                        onClick = { onTagsChanged(selectedTags + tag) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = chipColor
                        ),
                        label = { Text(tag, color = inkColor) }
                    )
                }
            }
        }
    }
}

/** Combined add-item dialog: name field + tag picker. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddItemWithTagsDialog(
    allTags: List<String>,
    currentItemCount: Int,
    errorMessage: String? = null,
    onNameChange: (() -> Unit)? = null,
    onConfirm: (name: String, tags: List<String>, initialRank: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(emptyList<String>()) }
    val rankOptions = remember(currentItemCount) { (1..(currentItemCount + 1)).toList() }
    var selectedRank by remember(currentItemCount) { mutableIntStateOf(currentItemCount + 1) }
    var isRankMenuExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val inkColor = notePadInkColor()
    val dialogColor = MaterialTheme.colorScheme.surface
    val fieldColor = MaterialTheme.colorScheme.surfaceVariant

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogColor,
        title = { Text("Add Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onNameChange?.invoke()
                    },
                    placeholder = { Text("Item name") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { msg -> { Text(msg) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (name.isNotBlank()) onConfirm(name, selectedTags, selectedRank)
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldColor,
                        unfocusedContainerColor = fieldColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                ExposedDropdownMenuBox(
                    expanded = isRankMenuExpanded,
                    onExpandedChange = { isRankMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedRank.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Initial rank") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRankMenuExpanded)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = fieldColor,
                            unfocusedContainerColor = fieldColor
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isRankMenuExpanded,
                        onDismissRequest = { isRankMenuExpanded = false }
                    ) {
                        rankOptions.forEach { rank ->
                            DropdownMenuItem(
                                text = { Text(rank.toString()) },
                                onClick = {
                                    selectedRank = rank
                                    isRankMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                TagPickerContent(
                    selectedTags = selectedTags,
                    allTags = allTags,
                    onTagsChanged = { selectedTags = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, selectedTags, selectedRank) },
                enabled = name.isNotBlank()
            ) { Text("Add", color = inkColor) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = inkColor) }
        }
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

/** Standalone tag editing dialog for existing items. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagPickerDialog(
    currentTags: List<String>,
    allTags: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTags by remember { mutableStateOf(currentTags) }
    val inkColor = notePadInkColor()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Edit Tags") },
        text = {
            TagPickerContent(
                selectedTags = selectedTags,
                allTags = allTags,
                onTagsChanged = { selectedTags = it }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedTags) }) { Text("Done", color = inkColor) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = inkColor) }
        }
    )
}

enum class ItemTransferDialogMode {
    COPY,
    MOVE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemTransferDialog(
    itemName: String,
    mode: ItemTransferDialogMode,
    availableLists: List<TransferListOption>,
    errorMessage: String? = null,
    onSelectionChange: () -> Unit = {},
    onConfirm: (destinationListId: Long, destinationRank: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val initialDestination = availableLists.firstOrNull() ?: return
    var selectedListId by remember(availableLists) { mutableLongStateOf(initialDestination.list.id) }
    val selectedList = availableLists.firstOrNull { it.list.id == selectedListId } ?: initialDestination
    val rankOptions = remember(selectedList.itemCount) { (1..(selectedList.itemCount + 1)).toList() }
    var selectedRank by remember(selectedList.list.id, selectedList.itemCount) {
        mutableIntStateOf(selectedList.itemCount + 1)
    }
    var isListMenuExpanded by remember { mutableStateOf(false) }
    var isRankMenuExpanded by remember { mutableStateOf(false) }
    val inkColor = notePadInkColor()
    val dialogColor = MaterialTheme.colorScheme.surface
    val fieldColor = MaterialTheme.colorScheme.surfaceVariant
    val actionLabel = if (mode == ItemTransferDialogMode.MOVE) "Move" else "Copy"

    LaunchedEffect(availableLists, selectedListId) {
        if (availableLists.none { it.list.id == selectedListId }) {
            selectedListId = initialDestination.list.id
            selectedRank = initialDestination.itemCount + 1
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogColor,
        title = { Text("$actionLabel Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose where to $actionLabel \"${itemName.trim()}\".")
                ExposedDropdownMenuBox(
                    expanded = isListMenuExpanded,
                    onExpandedChange = { isListMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedList.list.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Destination list") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isListMenuExpanded)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = fieldColor,
                            unfocusedContainerColor = fieldColor
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isListMenuExpanded,
                        onDismissRequest = { isListMenuExpanded = false }
                    ) {
                        availableLists.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.list.name) },
                                onClick = {
                                    selectedListId = option.list.id
                                    selectedRank = option.itemCount + 1
                                    isListMenuExpanded = false
                                    onSelectionChange()
                                }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = isRankMenuExpanded,
                    onExpandedChange = { isRankMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedRank.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Destination rank") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRankMenuExpanded)
                        },
                        supportingText = { Text("1 is top, ${selectedList.itemCount + 1} is end") },
                        isError = errorMessage != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = fieldColor,
                            unfocusedContainerColor = fieldColor
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isRankMenuExpanded,
                        onDismissRequest = { isRankMenuExpanded = false }
                    ) {
                        rankOptions.forEach { rank ->
                            DropdownMenuItem(
                                text = { Text(rank.toString()) },
                                onClick = {
                                    selectedRank = rank
                                    isRankMenuExpanded = false
                                    onSelectionChange()
                                }
                            )
                        }
                    }
                }
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedList.list.id, selectedRank) }) {
                Text(actionLabel, color = inkColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = inkColor) }
        }
    )
}

/** Simple yes/no confirmation dialog. */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    body: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val inkColor = notePadInkColor()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = inkColor) }
        }
    )
}

@Composable
fun ConfirmImportDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val inkColor = notePadInkColor()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Import Backup") },
        text = { Text("This will permanently replace all current lists, items, and tags. This cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Import", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = inkColor) }
        }
    )
}
