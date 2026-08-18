package com.alienmantech.onyx_hypernova.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.alienmantech.onyx_hypernova.ui.theme.notePadDialogColor
import com.alienmantech.onyx_hypernova.ui.theme.notePadInkColor

@Composable
fun TitleCaptionInputDialog(
    title: String,
    titlePlaceholder: String,
    captionPlaceholder: String,
    initialValue: String = "",
    confirmLabel: String = "Confirm",
    errorMessage: String? = null,
    onValueChange: ((String) -> Unit)? = null,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialParts = remember(initialValue) { parsePipeDelimitedTitle(initialValue) }
    var mainTitle by remember(initialValue) { mutableStateOf(initialParts.title) }
    var caption by remember(initialValue) { mutableStateOf(initialParts.caption) }
    val focusRequester = remember { FocusRequester() }
    val inkColor = notePadInkColor()
    val dialogColor = notePadDialogColor()
    val textFieldColors = notePadDialogTextFieldColors()

    fun currentValue(): String = buildPipeDelimitedTitle(mainTitle, caption)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogColor,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = mainTitle,
                    onValueChange = {
                        mainTitle = it
                        onValueChange?.invoke(currentValue())
                    },
                    placeholder = { Text(titlePlaceholder) },
                    singleLine = true,
                    isError = errorMessage != null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    supportingText = errorMessage?.let { message -> { Text(message) } },
                    colors = textFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                OutlinedTextField(
                    value = caption,
                    onValueChange = {
                        caption = it
                        onValueChange?.invoke(currentValue())
                    },
                    placeholder = { Text(captionPlaceholder) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val value = currentValue()
                            if (value.isNotBlank()) onConfirm(value)
                        }
                    ),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val combinedValue = currentValue()
            TextButton(
                onClick = { onConfirm(combinedValue) },
                enabled = combinedValue.isNotBlank()
            ) { Text(confirmLabel, color = inkColor) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = inkColor) }
        }
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
