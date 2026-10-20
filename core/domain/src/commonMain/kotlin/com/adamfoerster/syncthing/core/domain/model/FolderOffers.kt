package com.adamfoerster.syncthing.core.domain.model

/** A device that should be added to a folder we already share, because it offered that same folder. */
data class KnownFolderOffer(
    val folder: Folder,
    val devices: List<Device>,
)

/**
 * Offers that only repeat something the user already decided: the folder ID exists here as a regular
 * send-and-receive folder, and the offering device is already one of our devices but not yet in that
 * folder. Offers for unknown folders, unknown devices or other folder types still need a human answer.
 */
fun SyncOverview.knownFolderOffers(): List<KnownFolderOffer> =
    pendingFolders
        .mapNotNull { offer ->
            val folder = folders.firstOrNull { it.id == offer.folderId } ?: return@mapNotNull null
            val device = devices.firstOrNull { it.id == offer.offeredBy } ?: return@mapNotNull null
            if (folder.type != FolderType.SEND_RECEIVE || device.id in folder.deviceIds) return@mapNotNull null
            folder to device
        }
        .groupBy({ it.first }, { it.second })
        .map { (folder, devices) -> KnownFolderOffer(folder, devices.distinctBy { it.id }) }
