package com.adamfoerster.syncthing.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

object NgTheme {
    val colors: NgColors
        @Composable @ReadOnlyComposable get() = LocalNgColors.current

    val type: NgTypography
        @Composable @ReadOnlyComposable get() = LocalNgTypography.current
}

/** [darkTheme] picks the palette; previews and callers that do not care follow the device. */
@Composable
fun SyncthingTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) NgColors.Dark else NgColors.Light
    val typography = rememberNgTypography()
    // Material components (dialogs, text fields, snackbars) pick up the same palette.
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
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
