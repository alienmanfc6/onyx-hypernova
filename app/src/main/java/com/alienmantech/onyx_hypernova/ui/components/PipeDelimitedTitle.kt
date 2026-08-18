package com.alienmantech.onyx_hypernova.ui.components

private const val PipeCaptionDelimiter = " | "

data class PipeDelimitedTitleParts(
    val title: String,
    val caption: String
)

fun parsePipeDelimitedTitle(text: String): PipeDelimitedTitleParts {
    val delimiterIndex = text.indexOf(PipeCaptionDelimiter)
    if (delimiterIndex < 0) {
        return PipeDelimitedTitleParts(title = text, caption = "")
    }

    val title = text.substring(0, delimiterIndex)
    val caption = text.substring(delimiterIndex + PipeCaptionDelimiter.length)
    return if (title.isBlank() || caption.isBlank()) {
        PipeDelimitedTitleParts(title = text, caption = "")
    } else {
        PipeDelimitedTitleParts(title = title, caption = caption)
    }
}

fun buildPipeDelimitedTitle(title: String, caption: String): String {
    val trimmedTitle = title.trim()
    val trimmedCaption = caption.trim()
    return if (trimmedTitle.isBlank() || trimmedCaption.isBlank()) {
        trimmedTitle
    } else {
        "$trimmedTitle$PipeCaptionDelimiter$trimmedCaption"
    }
}
