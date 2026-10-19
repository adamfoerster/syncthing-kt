package com.adamfoerster.syncthing.feature.conflicts.presentation.binary

import androidx.compose.ui.graphics.ImageBitmap

/** A downsampled preview plus the original pixel size ("1080×1350"). */
class ImagePreview(val bitmap: ImageBitmap, val width: Int, val height: Int)

/** Decodes [bytes] scaled down so neither side exceeds [maxSide]; null if it isn't a decodable image. */
expect fun decodeImagePreview(bytes: ByteArray, maxSide: Int): ImagePreview?
