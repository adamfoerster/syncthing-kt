package com.adamfoerster.syncthing.feature.conflicts.domain.model

/**
 * Syncthing names a conflict copy `<base>.sync-conflict-<YYYYMMDD>-<HHMMSS>-<shortID><ext>`,
 * where `<ext>` is the original's last extension (`a.tar.gz` → `a.tar.sync-conflict-…-ID.gz`)
 * and `<shortID>` identifies the device that last modified the copy's content.
 */
data class ConflictFileName(
    val originalName: String,
    /** `YYYYMMDD-HHMMSS`, local time of the device that detected the conflict. */
    val stamp: String,
    val shortDeviceId: String,
) {
    companion object {
        private val PATTERN = Regex("""^(.*)\.sync-conflict-(\d{8}-\d{6})-([A-Z2-7]{7})(\.[^.]*)?$""")

        fun parse(fileName: String): ConflictFileName? {
            val match = PATTERN.matchEntire(fileName) ?: return null
            val (base, stamp, shortId, ext) = match.destructured
            if (base.isEmpty()) return null
            return ConflictFileName(originalName = base + ext, stamp = stamp, shortDeviceId = shortId)
        }

        fun isConflictCopy(fileName: String): Boolean = parse(fileName) != null
    }
}
