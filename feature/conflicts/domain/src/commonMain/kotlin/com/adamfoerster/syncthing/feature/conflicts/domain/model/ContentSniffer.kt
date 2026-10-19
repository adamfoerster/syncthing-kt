package com.adamfoerster.syncthing.feature.conflicts.domain.model

/** Decides how a conflict can be resolved from the file names and the first bytes of both versions. */
object ContentSniffer {
    /** Larger text files fall back to "pick a version": a phone screen can't sensibly review them. */
    const val MAX_TEXT_BYTES = 2L * 1024 * 1024
    const val SAMPLE_BYTES = 8 * 1024

    private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "heic", "heif")

    fun kindOf(fileName: String, samples: List<ByteArray>, sizes: List<Long>): ConflictKind {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when {
            ext in IMAGE_EXTENSIONS -> ConflictKind.IMAGE
            sizes.all { it <= MAX_TEXT_BYTES } && samples.all(::looksLikeText) -> ConflictKind.TEXT
            else -> ConflictKind.BINARY
        }
    }

    /** No NUL bytes and valid UTF-8 (a sequence cut off at the end of the sample is fine). */
    fun looksLikeText(sample: ByteArray): Boolean {
        if (sample.any { it == 0.toByte() }) return false
        var i = 0
        while (i < sample.size) {
            val b = sample[i].toInt() and 0xFF
            val extra = when {
                b < 0x80 -> 0
                b in 0xC2..0xDF -> 1
                b in 0xE0..0xEF -> 2
                b in 0xF0..0xF4 -> 3
                else -> return false
            }
            for (k in 1..extra) {
                if (i + k >= sample.size) return true
                if ((sample[i + k].toInt() and 0xC0) != 0x80) return false
            }
            i += extra + 1
        }
        return true
    }
}
