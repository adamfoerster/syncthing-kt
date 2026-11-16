package com.adamfoerster.meshsync.feature.files.domain

import com.adamfoerster.meshsync.core.domain.util.DataError
import com.adamfoerster.meshsync.core.domain.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Content another app handed to MeshSync through the system share sheet: [uris] are opaque content
 * references (only the platform importer can read them) and [text] is plain text shared without a file.
 */
data class IncomingShare(val uris: List<String> = emptyList(), val text: String? = null) {
    /** How many files will be created in the chosen folder. */
    val itemCount: Int get() = uris.size + if (text.isNullOrEmpty()) 0 else 1
    val isEmpty: Boolean get() = itemCount == 0
}

/** Holds a share until the user has picked the folder for it; survives the host recreating its activity. */
class IncomingShareInbox {
    private val _pending = MutableStateFlow<IncomingShare?>(null)
    val pending: StateFlow<IncomingShare?> = _pending.asStateFlow()

    /** Empty shares are ignored; a newer share replaces an unanswered older one. */
    fun offer(share: IncomingShare) {
        if (!share.isEmpty) _pending.value = share
    }

    fun clear() {
        _pending.value = null
    }
}

/** Copies a share into a synced folder; implemented by the host, which owns the content references. */
interface ShareImporter {
    /**
     * Writes every item of [share] into [targetDirectory] (absolute) and returns how many files were created.
     * A name already taken is kept: the newcomer becomes `name (1).ext`, so nothing is ever overwritten.
     */
    suspend fun import(share: IncomingShare, targetDirectory: String): Result<Int, DataError.Local>
}

/** Name for a shared file: the provider's [displayName] cleaned into one safe path segment, or [fallback]. */
fun sharedFileName(displayName: String?, fallback: String): String {
    val cleaned = displayName.orEmpty().replace('/', '_').replace('\\', '_').replace('\u0000', '_').trim()
    return if (isValidFileName(cleaned)) cleaned else fallback
}
