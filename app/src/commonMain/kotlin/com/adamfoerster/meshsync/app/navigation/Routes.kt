package com.adamfoerster.meshsync.app.navigation

import kotlinx.serialization.Serializable

/** First-run introduction; shown once, before the tabs. */
@Serializable
data object OnboardingRoute

@Serializable
data object HomeRoute

@Serializable
data object FoldersRoute

@Serializable
data object DevicesRoute

@Serializable
data object SettingsRoute

@Serializable
data object SharingRoute

/** All three set when accepting a folder another device offered. */
@Serializable
data class AddFolderRoute(
    val offeredFolderId: String? = null,
    val offeredLabel: String? = null,
    val offeredBy: String? = null,
    /** Set when syncing a folder picked in the file manager: pre-fills the folder path. */
    val path: String? = null,
)

/** The ignored-files editor of one folder. */
@Serializable
data class IgnoreRoute(val folderId: String)

/** Set when approving a device that asked to connect. */
@Serializable
data class AddDeviceRoute(
    val deviceId: String? = null,
    val name: String? = null,
)

/** The file manager, starting at the device's shared storage. */
@Serializable
data object FilesRoute
