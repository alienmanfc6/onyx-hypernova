package com.alienmantech.onyx_hypernova.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

private const val PipeCaptionDelimiter = " | "

@Composable
fun InlinePipeCaptionText(
    text: String,
    style: TextStyle,
    color: Color,
    captionStyle: TextStyle,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    captionAlpha: Float = 0.7f
) {
    val splitText = splitPipeCaptionText(text)

    if (splitText == null) {
        Text(
            text = text,
            modifier = modifier,
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = overflow
        )
        return
    }

    Text(
        text = buildAnnotatedString {
            append(splitText.title)
            append("   ")
            withStyle(
                SpanStyle(
                    color = color.copy(alpha = captionAlpha),
                    fontSize = captionStyle.fontSize,
                    fontWeight = captionStyle.fontWeight,
                    fontStyle = captionStyle.fontStyle,
                    fontFamily = captionStyle.fontFamily,
                    letterSpacing = captionStyle.letterSpacing
                )
            ) {
                append(splitText.caption)
            }
        },
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow
    )
}

private data class PipeCaptionParts(
    val title: String,
    val caption: String
)

private fun splitPipeCaptionText(text: String): PipeCaptionParts? {
    val delimiterIndex = text.indexOf(PipeCaptionDelimiter)
    if (delimiterIndex < 0) return null

    val title = text.substring(0, delimiterIndex)
    val caption = text.substring(delimiterIndex + PipeCaptionDelimiter.length)
    if (title.isBlank() || caption.isBlank()) return null

    return PipeCaptionParts(title = title, caption = caption)
}
