package com.adamfoerster.syncthing.core.domain.model

data class Folder(
    val id: String,
    val label: String,
    val path: String,
    val type: FolderType,
    val paused: Boolean,
    val deviceIds: List<DeviceId>,
    val status: FolderStatus,
)

enum class FolderType(val apiValue: String) {
    SEND_RECEIVE("sendreceive"),
    SEND_ONLY("sendonly"),
    RECEIVE_ONLY("receiveonly"),
    RECEIVE_ENCRYPTED("receiveencrypted");

    companion object {
        fun fromApi(value: String): FolderType = entries.firstOrNull { it.apiValue == value } ?: SEND_RECEIVE
    }
}

data class FolderStatus(
    val state: FolderState,
    val globalBytes: Long,
    val localBytes: Long,
    val needBytes: Long,
    val needFiles: Long,
    /** Bumps whenever the local index changes — a cheap "something changed on disk" signal. */
    val sequence: Long,
    val error: String?,
) {
    /** 0..100 */
    val completion: Double
        get() = if (globalBytes <= 0L) 100.0 else (globalBytes - needBytes).coerceAtLeast(0) * 100.0 / globalBytes

    companion object {
        val Unknown = FolderStatus(FolderState.UNKNOWN, 0, 0, 0, 0, 0, null)
    }
}

enum class FolderState {
    IDLE,
    SCANNING,
    SYNCING,
    ERROR,
    PAUSED,
    UNKNOWN,
}
