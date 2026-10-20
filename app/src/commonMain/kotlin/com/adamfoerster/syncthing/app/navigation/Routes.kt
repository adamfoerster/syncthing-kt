package com.adamfoerster.syncthing.app.navigation

import kotlinx.serialization.Serializable

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
