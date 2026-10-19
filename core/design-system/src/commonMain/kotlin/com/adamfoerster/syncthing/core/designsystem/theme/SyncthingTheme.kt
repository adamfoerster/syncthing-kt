package com.adamfoerster.syncthing.core.designsystem.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

object NgTheme {
    val colors: NgColors
        @Composable @ReadOnlyComposable get() = LocalNgColors.current

    val type: NgTypography
        @Composable @ReadOnlyComposable get() = LocalNgTypography.current
}

@Composable
fun SyncthingTheme(content: @Composable () -> Unit) {
    val colors = NgColors()
    val typography = rememberNgTypography()
    // Material components (dialogs, text fields, snackbars) pick up the same palette.
    val scheme = darkColorScheme(
        primary = colors.cyan,
        onPrimary = colors.bg,
        secondary = colors.info,
        background = colors.bg,
        onBackground = colors.text,
        surface = colors.panel,
        onSurface = colors.text,
        surfaceVariant = colors.raised,
        onSurfaceVariant = colors.text2,
        surfaceContainerHigh = colors.panel,
        surfaceContainer = colors.panel,
        outline = colors.lineStrong,
        outlineVariant = colors.line,
        error = colors.err,
    )
    CompositionLocalProvider(
        LocalNgColors provides colors,
        LocalNgTypography provides typography,
        LocalTextSelectionColors provides TextSelectionColors(colors.cyan, colors.cyan.copy(alpha = 0.3f)),
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
