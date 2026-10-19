package com.adamfoerster.syncthing.core.domain.model

import kotlin.jvm.JvmInline

/** Full Syncthing device ID, e.g. `MFZWI3D-BONSGYC-YLTMRWG-C43ENR5-QXGZDMM-FZWI3DP-BONSGYY-LTMRWAD`. */
@JvmInline
value class DeviceId(val value: String) {
    /** Syncthing's short ID: the first group, used in conflict file names and `modifiedBy`. */
    val short: String get() = value.replace("-", "").take(SHORT_LENGTH)

    companion object {
        const val SHORT_LENGTH = 7
        private val FORMAT = Regex("^[A-Z2-7]{7}(-[A-Z2-7]{7}){7}$")

        /** Accepts the ID with or without dashes/spaces, any case, and normalizes it. */
        fun parse(raw: String): DeviceId? {
            val compact = raw.trim().uppercase().filter { it.isLetterOrDigit() }
            if (compact.length != 56) return null
            val dashed = compact.chunked(7).joinToString("-")
            return if (FORMAT.matches(dashed)) DeviceId(dashed) else null
        }
    }
}
