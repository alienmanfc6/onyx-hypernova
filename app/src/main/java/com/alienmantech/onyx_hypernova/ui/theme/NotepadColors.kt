package com.alienmantech.onyx_hypernova.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val NotePadYellowLight = Color(0xFFFEF9C3)
private val NotePadToolbarLight = Color(0xFFFDE68A)
private val NotePadLineLight = Color(0xFF93C5FD)
private val NotePadDragHighlightLight = Color(0xFFFCD34D)
private val NotePadInkLight = Color(0xFF713F12)
private val NotePadSurfaceLight = Color(0xFFFFF7D6)

private val NotePadYellowDark = Color(0xFF332A21)
private val NotePadToolbarDark = Color(0xFF4A3D30)
private val NotePadLineDark = Color(0xFF6A7A8A)
private val NotePadDragHighlightDark = Color(0xFF6B572C)
private val NotePadInkDark = Color(0xFFF6E7BF)
private val NotePadSurfaceDark = Color(0xFF3D3227)

@Immutable
data class NotepadPalette(
    val isDark: Boolean,
    val page: Color,
    val toolbar: Color,
    val line: Color,
    val highlight: Color,
    val ink: Color,
    val surface: Color,
    val dialogSurface: Color,
    val fieldSurface: Color,
    val fieldFocus: Color,
    val fieldOutline: Color,
    val fieldPlaceholder: Color
)

val LightNotepadPalette = NotepadPalette(
    isDark = false,
    page = NotePadYellowLight,
    toolbar = NotePadToolbarLight,
    line = NotePadLineLight,
    highlight = NotePadDragHighlightLight,
    ink = NotePadInkLight,
    surface = NotePadSurfaceLight,
    dialogSurface = Color(0xFFFFF3C4),
    fieldSurface = Color(0xFFF8EDBC),
    fieldFocus = Color(0xFF9A6414),
    fieldOutline = Color(0xFFB7A27B),
    fieldPlaceholder = Color(0xFF8B6B46)
)

val DarkNotepadPalette = NotepadPalette(
    isDark = true,
    page = NotePadYellowDark,
    toolbar = NotePadToolbarDark,
    line = NotePadLineDark,
    highlight = NotePadDragHighlightDark,
    ink = NotePadInkDark,
    surface = NotePadSurfaceDark,
    dialogSurface = Color(0xFF46392D),
    fieldSurface = Color(0xFF544536),
    fieldFocus = Color(0xFFF4DE8A),
    fieldOutline = Color(0xFF9B876C),
    fieldPlaceholder = Color(0xFFC7B69F)
)

val LocalNotepadPalette = staticCompositionLocalOf { LightNotepadPalette }

private val darkModeItemColorOverrides = mapOf(
    "#FFFF00" to "#8A6A00",
    "#00FFFF" to "#0B6E69"
)

val NotePadYellow = NotePadYellowLight
val NotePadToolbar = NotePadToolbarLight
val NotePadLine = NotePadLineLight
val NotePadDragHighlight = NotePadDragHighlightLight
val NotePadBrown = NotePadInkLight

@Composable
fun notePadPageColor(): Color = LocalNotepadPalette.current.page

@Composable
fun notePadToolbarColor(): Color = LocalNotepadPalette.current.toolbar

@Composable
fun notePadLineColor(): Color = LocalNotepadPalette.current.line

@Composable
fun notePadHighlightColor(): Color = LocalNotepadPalette.current.highlight

@Composable
fun notePadInkColor(): Color = LocalNotepadPalette.current.ink

@Composable
fun notePadSurfaceColor(): Color = LocalNotepadPalette.current.surface

@Composable
fun notePadDialogColor(): Color = LocalNotepadPalette.current.dialogSurface

@Composable
fun notePadFieldColor(): Color = LocalNotepadPalette.current.fieldSurface

@Composable
fun notePadFieldFocusColor(): Color = LocalNotepadPalette.current.fieldFocus

@Composable
fun notePadFieldOutlineColor(): Color = LocalNotepadPalette.current.fieldOutline

@Composable
fun notePadFieldPlaceholderColor(): Color = LocalNotepadPalette.current.fieldPlaceholder

@Composable
fun displayItemColorHex(colorHex: String?): String? {
    if (colorHex == null) return null

    val normalized = colorHex.uppercase()
    return if (LocalNotepadPalette.current.isDark) {
        darkModeItemColorOverrides[normalized] ?: normalized
    } else {
        normalized
    }
}

val colorPalette = listOf(
    null,
    "#FF0040", // Neon Red
    "#FF6600", // Neon Orange
    "#FFFF00", // Neon Yellow
    "#39FF14", // Neon Green
    "#00FFFF", // Neon Cyan
    "#0080FF", // Neon Blue
    "#BF00FF", // Neon Purple
    "#FF00AA", // Neon Pink
)
