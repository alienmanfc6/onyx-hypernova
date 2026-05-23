package com.alienmantech.onyx_hypernova.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Palette ───────────────────────────────────────────────────────────────

private val primaryLight = Color(0xFF4F46E5)        // Indigo
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFE0DEFF)
private val onPrimaryContainerLight = Color(0xFF1A0F6C)
private val secondaryLight = Color(0xFF7C3AED)

private val primaryDark = Color(0xFFF4DE8A)
private val onPrimaryDark = Color(0xFF3F2A00)
private val primaryContainerDark = Color(0xFF6A5520)
private val onPrimaryContainerDark = Color(0xFFFFF2C4)
private val secondaryDark = Color(0xFFDCC78A)

private val LightColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    surface = LightNotepadPalette.surface,
    background = LightNotepadPalette.page,
    surfaceVariant = Color(0xFFE5E3F3),
    onSurfaceVariant = Color(0xFF47464F),
    outline = Color(0xFF78767F)
)

private val DarkColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    surface = DarkNotepadPalette.surface,
    background = DarkNotepadPalette.page,
    surfaceVariant = Color(0xFF43372A),
    onSurfaceVariant = Color(0xFFE3D6C5),
    outline = Color(0xFFAC9B84)
)

@Composable
fun RankItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val notepadPalette = if (darkTheme) DarkNotepadPalette else LightNotepadPalette

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = notepadPalette.page.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalNotepadPalette provides notepadPalette) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = notepadPalette.page
            ) {
                content()
            }
        }
    }
}
