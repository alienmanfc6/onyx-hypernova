package com.alienmantech.onyx_hypernova.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

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

private fun splitPipeCaptionText(text: String): PipeDelimitedTitleParts? {
    val parts = parsePipeDelimitedTitle(text)
    return if (parts.caption.isBlank() || parts.title.isBlank()) null else parts
}
