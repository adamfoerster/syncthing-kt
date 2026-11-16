package com.adamfoerster.meshsync.desktop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter

/**
 * The MeshSync "M" for the macOS menu bar, drawn as vectors so it stays sharp at the menu bar's ~18 pt
 * (shrinking the 512 px app icon blurred its thin hollow squares). It is a template image: macOS uses only
 * its alpha and paints it in the menu bar's colour, like the system icons. Same geometry as `app-icon.png`,
 * but the vertex squares are solid so they read at that size.
 */
class MenuBarIcon : Painter() {
    override val intrinsicSize: Size = Size(SIZE, SIZE)

    override fun DrawScope.onDraw() {
        val scale = size.minDimension / EXTENT
        // Centre the glyph's box in the square.
        val left = (size.width - GLYPH_WIDTH * scale) / 2f
        val top = (size.height - GLYPH_HEIGHT * scale) / 2f
        fun p(x: Float, y: Float) = Offset(left + (x - MIN_X) * scale, top + (y - MIN_Y) * scale)

        // The fainter links from the centre to the bottom vertices, as in the app icon.
        for (bottom in listOf(BOTTOM_LEFT, BOTTOM_RIGHT)) {
            drawLine(Color.White.copy(alpha = LINK_ALPHA), p(CENTRE.x, CENTRE.y), p(bottom.x, bottom.y), LINK_WIDTH * scale)
        }
        // Legs and diagonals.
        drawLine(Color.White, p(TOP_LEFT.x, TOP_LEFT.y), p(BOTTOM_LEFT.x, BOTTOM_LEFT.y), LEG_WIDTH * scale)
        drawLine(Color.White, p(TOP_RIGHT.x, TOP_RIGHT.y), p(BOTTOM_RIGHT.x, BOTTOM_RIGHT.y), LEG_WIDTH * scale)
        drawLine(Color.White, p(TOP_LEFT.x, TOP_LEFT.y), p(CENTRE.x, CENTRE.y), DIAGONAL_WIDTH * scale)
        drawLine(Color.White, p(TOP_RIGHT.x, TOP_RIGHT.y), p(CENTRE.x, CENTRE.y), DIAGONAL_WIDTH * scale)
        // Solid vertices on top.
        for (vertex in listOf(TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTRE)) {
            val half = SQUARE / 2f
            drawRect(Color.White, p(vertex.x - half, vertex.y - half), Size(SQUARE * scale, SQUARE * scale))
        }
    }

    companion object {
        /** Makes AWT mark tray images as templates. Read once by the tray peer, so set it before any tray exists. */
        const val TEMPLATE_PROPERTY = "apple.awt.enableTemplateImages"

        private const val SIZE = 44f

        // Geometry in app-icon.png pixels (512 px canvas), measured from the icon.
        private const val SQUARE = 80f
        private const val LEG_WIDTH = 23f
        private const val DIAGONAL_WIDTH = 21f
        private const val LINK_WIDTH = 9f
        private const val LINK_ALPHA = 0.45f
        private val TOP_LEFT = Offset(118f, 125.5f)
        private val TOP_RIGHT = Offset(378.5f, 125.5f)
        private val BOTTOM_LEFT = Offset(118f, 371f)
        private val BOTTOM_RIGHT = Offset(378.5f, 371f)
        private val CENTRE = Offset(248f, 279f)

        private const val MIN_X = 118f - SQUARE / 2f
        private const val MIN_Y = 125.5f - SQUARE / 2f
        private const val GLYPH_WIDTH = 378.5f - 118f + SQUARE
        private const val GLYPH_HEIGHT = 371f - 125.5f + SQUARE

        /** Glyph box plus a small margin, mapped onto the icon's square. */
        private const val EXTENT = GLYPH_WIDTH * 1.08f
    }
}
