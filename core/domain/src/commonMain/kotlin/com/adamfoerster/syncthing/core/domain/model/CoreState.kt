package com.adamfoerster.syncthing.core.domain.model

/** Lifecycle of the bundled Syncthing core process. */
sealed interface CoreState {
    data object Stopped : CoreState
    data object Starting : CoreState
    data object Running : CoreState

    /** The native binary is not packaged in this build (see `:syncthing-native`). */
    data object BinaryMissing : CoreState

    /** All-files access is required so Syncthing (and conflict resolution) can reach shared folders. */
    data object StoragePermissionMissing : CoreState
    data class Failed(val message: String) : CoreState
}
