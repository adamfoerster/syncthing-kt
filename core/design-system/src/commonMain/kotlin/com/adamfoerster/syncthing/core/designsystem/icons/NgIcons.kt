package com.adamfoerster.syncthing.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Line icons taken verbatim from the mockups' 24×24 SVG paths
 * (1.5 stroke, square caps, miter joins). Tint them with Icon's `tint`.
 */
object NgIcons {
    val Home = icon("Home", "M3 11l9-8 9 8M5 10v11h14V10M10 21v-6h4v6")
    val Folder = icon("Folder", "M3 6h6l2 2h10v12H3z")
    val Devices = icon("Devices", "M3 5h13v10H3zM7 19h5M9.5 15v4M18 8h3v11h-5")
    val Settings = icon("Settings", "M4 7h10M18 7h2M14 5v4h4V5zM4 17h2M10 17h10M6 15v4h4v-4z")
    val Phone = icon("Phone", "M7 2h10v20H7zM11 18h2")
    val ShareMap = icon("ShareMap", "M3 5h5v5H3zM3 14h5v5H3zM16 9h5v6h-5zM8 7.5h4l4 4.5M8 16.5h4l4-4.5")
    val Back = icon("Back", "M15 5l-7 7 7 7")
    val Chevron = icon("Chevron", "M9 5l7 7-7 7")
    val Plus = icon("Plus", "M12 4v16M4 12h16")
    val Check = icon("Check", "M4 12l5 5L20 6")
    val Close = icon("Close", "M6 6l12 12M18 6L6 18")
    val Warning = icon("Warning", "M12 3l10 18H2zM12 10v5M12 17.5v1")
    val Info = icon("Info", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM12 11v6M12 7.5v1")
    val Transfer = icon("Transfer", "M4 9h14l-4-4M20 15H6l4 4")
    val Qr = icon("Qr", "M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h3v3h-3zM19 14v3M14 20h7")
    val TextFile = icon("TextFile", "M6 3h8l4 4v14H6zM14 3v4h4M9 12h6M9 16h6")
    val File = icon("File", "M6 3h8l4 4v14H6zM14 3v4h4")
    val Image = icon("Image", "M3 4h18v16H3zM3 17l6-6 4 4 3-3 5 5M16 9h1")
    val Edit = icon("Edit", "M4 20h4L19 9l-4-4L4 16zM13 7l4 4")
    val Pause = icon("Pause", "M8 5v14M16 5v14")
    val Play = icon("Play", "M7 4l13 8-13 8z")
    val Copy = icon("Copy", "M8 8h12v12H8zM4 16V4h12")
    val Power = icon("Power", "M12 3v9M6.3 6.3a8 8 0 1 0 11.4 0")
    val Block = icon("Block", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM5.6 5.6l12.8 12.8")
    val Restart = icon("Restart","M4 4v6h6M20 12a8 8 0 1 1-2.3-5.7L20 9")

    /** Status glyphs (`.st`): square = connected, diamond = syncing, triangle = offline. */
    val StatusSquare = icon("StatusSquare", "M4 4h16v16H4z")
    val StatusDiamond = icon("StatusDiamond", "M12 2l10 10-10 10L2 12z")
    val StatusTriangle = icon("StatusTriangle", "M12 3l10 18H2z")

    private fun icon(name: String, path: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = addPathNodes(path),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Square,
            strokeLineJoin = StrokeJoin.Miter,
        ).build()
}
