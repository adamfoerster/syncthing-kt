package com.adamfoerster.meshsync.core.presentation

/**
 * Turns the document ID of a folder picked with the system chooser (`primary:Documents/Fotos`,
 * `1A2B-3C4D:Backup`) into a file-system path Syncthing can use. Syncthing works on paths, not on
 * content URIs, and the app already holds all-files access, so the path is enough.
 *
 * @param primaryRoot where the `primary` volume is mounted, e.g. `/storage/emulated/0`.
 * @return null for providers that are not backed by a volume (Downloads, Drive…).
 */
fun documentIdToPath(documentId: String, primaryRoot: String): String? {
    val separator = documentId.indexOf(':')
    if (separator <= 0) return null
    val volume = documentId.substring(0, separator)
    val relative = documentId.substring(separator + 1).trim('/')
    val root = when {
        volume == "primary" -> primaryRoot.trimEnd('/')
        VOLUME_ID.matches(volume) -> "/storage/$volume"
        else -> return null
    }
    return if (relative.isEmpty()) root else "$root/$relative"
}

/** Removable volumes are identified by their serial, e.g. `1A2B-3C4D`. */
private val VOLUME_ID = Regex("[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}")
