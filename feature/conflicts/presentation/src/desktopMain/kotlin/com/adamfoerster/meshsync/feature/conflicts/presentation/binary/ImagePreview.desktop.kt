package com.adamfoerster.meshsync.feature.conflicts.presentation.binary

import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

actual fun decodeImagePreview(bytes: ByteArray, maxSide: Int): ImagePreview? {
    // ImageIO throws or returns null for anything it can't decode; both mean "not an image".
    val source = runCatching { ImageIO.read(bytes.inputStream()) }.getOrNull() ?: return null
    val width = source.width
    val height = source.height
    if (width <= 0 || height <= 0) return null
    val scale = minOf(1.0, maxSide.toDouble() / maxOf(width, height))
    val preview = if (scale < 1.0) source.scaledTo(
        (width * scale).toInt().coerceAtLeast(1),
        (height * scale).toInt().coerceAtLeast(1),
    ) else source
    return ImagePreview(preview.toComposeImageBitmap(), width, height)
}

private fun BufferedImage.scaledTo(targetWidth: Int, targetHeight: Int): BufferedImage {
    val scaled = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB)
    val graphics = scaled.createGraphics()
    try {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.drawImage(this, 0, 0, targetWidth, targetHeight, null)
    } finally {
        graphics.dispose()
    }
    return scaled
}
