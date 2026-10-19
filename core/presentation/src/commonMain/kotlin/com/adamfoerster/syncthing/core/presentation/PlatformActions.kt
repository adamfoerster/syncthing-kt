package com.adamfoerster.syncthing.core.presentation

import androidx.compose.runtime.staticCompositionLocalOf

/** Things only the host platform can do, provided by the app at the root of the composition. */
interface PlatformActions {
    /** Opens the camera QR scanner; [onResult] gets the scanned text, or null if cancelled. */
    fun scanQrCode(onResult: (String?) -> Unit)

    /** Sends the user to grant all-files access, which Syncthing needs for shared folders. */
    fun requestStorageAccess()

    /** Suggested parent for new folders, e.g. `/storage/emulated/0`. */
    val defaultFolderRoot: String

    object NoOp : PlatformActions {
        override fun scanQrCode(onResult: (String?) -> Unit) = onResult(null)
        override fun requestStorageAccess() = Unit
        override val defaultFolderRoot: String = "/storage/emulated/0"
    }
}

val LocalPlatformActions = staticCompositionLocalOf<PlatformActions> { PlatformActions.NoOp }
