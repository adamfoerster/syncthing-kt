package com.adamfoerster.syncthing.feature.conflicts.presentation.binary

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap

actual fun decodeImagePreview(bytes: ByteArray, maxSide: Int): ImagePreview? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxSide || bounds.outHeight / (sample * 2) >= maxSide) sample *= 2
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: return null
    return ImagePreview(bitmap.asImageBitmap(), bounds.outWidth, bounds.outHeight)
}
