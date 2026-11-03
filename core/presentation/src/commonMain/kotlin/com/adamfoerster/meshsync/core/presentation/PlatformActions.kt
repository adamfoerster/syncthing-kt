package com.adamfoerster.meshsync.core.presentation

import androidx.compose.runtime.staticCompositionLocalOf

/** Things only the host platform can do, provided by the app at the root of the composition. */
interface PlatformActions {
    /** Opens the camera QR scanner; [onResult] gets the scanned text, or null if cancelled. */
    fun scanQrCode(onResult: (String?) -> Unit)

    /** Opens the system folder chooser (which can also create folders); [onResult] gets the chosen path, or null if cancelled. */
    fun pickFolder(onResult: (String?) -> Unit)

    /** Opens the system share dialog for the files at [paths] (absolute). */
    fun shareFiles(paths: List<String>)

    /** Sends the user to grant all-files access, which Syncthing needs for shared folders. */
    fun requestStorageAccess()

    /** Whether the all-files access above is currently granted; the answer changes while the app is in the background. */
    fun hasStorageAccess(): Boolean

    /** Suggested parent for new folders, e.g. `/storage/emulated/0`. */
    val defaultFolderRoot: String

    object NoOp : PlatformActions {
        override fun scanQrCode(onResult: (String?) -> Unit) = onResult(null)
        override fun pickFolder(onResult: (String?) -> Unit) = onResult(null)
        override fun shareFiles(paths: List<String>) = Unit
        override fun requestStorageAccess() = Unit
        override fun hasStorageAccess(): Boolean = true
        override val defaultFolderRoot: String = "/storage/emulated/0"
    }
}

val LocalPlatformActions = staticCompositionLocalOf<PlatformActions> { PlatformActions.NoOp }
