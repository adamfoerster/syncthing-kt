package com.adamfoerster.meshsync.desktop

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import java.awt.image.BufferedImage
import kotlin.test.Test

class MenuBarIconTest {
    private fun render(px: Int): BufferedImage {
        val image = MenuBarIcon().toAwtImage(Density(1f), LayoutDirection.Ltr, Size(px.toFloat(), px.toFloat()))
        return BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB).apply {
            createGraphics().apply { drawImage(image, 0, 0, null); dispose() }
        }
    }

    private fun BufferedImage.alpha(fx: Double, fy: Double) = getRGB((fx * width).toInt(), (fy * height).toInt()) ushr 24

    @Test
    fun `the vertex squares are solid, not hollow`() {
        val icon = render(256)
        // Centres of the four corner squares and of the middle one.
        for ((x, y) in listOf(0.13 to 0.15, 0.87 to 0.15, 0.13 to 0.85, 0.87 to 0.85, 0.5 to 0.67)) {
            assertThat(icon.alpha(x, y)).isEqualTo(255)
        }
    }

    @Test
    fun `the background is transparent`() {
        val icon = render(256)
        assertThat(icon.alpha(0.01, 0.01)).isEqualTo(0)
        // Inside the M, between the leg and the diagonal.
        assertThat(icon.alpha(0.3, 0.5)).isEqualTo(0)
    }

    @Test
    fun `the links to the bottom vertices stay fainter`() {
        val icon = render(256)
        // Midpoint between the centre square and the bottom-left square.
        assertThat(icon.alpha(0.322, 0.709)).isBetween(1, 254)
    }
}
