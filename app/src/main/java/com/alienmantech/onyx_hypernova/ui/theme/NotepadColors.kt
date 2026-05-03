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
    val page: Color,
    val toolbar: Color,
    val line: Color,
    val highlight: Color,
    val ink: Color,
    val surface: Color
)

val LightNotepadPalette = NotepadPalette(
    page = NotePadYellowLight,
    toolbar = NotePadToolbarLight,
    line = NotePadLineLight,
    highlight = NotePadDragHighlightLight,
    ink = NotePadInkLight,
    surface = NotePadSurfaceLight
)

val DarkNotepadPalette = NotepadPalette(
    page = NotePadYellowDark,
    toolbar = NotePadToolbarDark,
    line = NotePadLineDark,
    highlight = NotePadDragHighlightDark,
    ink = NotePadInkDark,
    surface = NotePadSurfaceDark
)

val LocalNotepadPalette = staticCompositionLocalOf { LightNotepadPalette }

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
