package com.adamfoerster.syncthing.core.domain

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.adamfoerster.syncthing.core.domain.model.Device
import com.adamfoerster.syncthing.core.domain.model.DeviceId
import com.adamfoerster.syncthing.core.domain.model.DeviceStatus
import com.adamfoerster.syncthing.core.domain.model.Folder
import com.adamfoerster.syncthing.core.domain.model.FolderStatus
import com.adamfoerster.syncthing.core.domain.model.FolderType
import com.adamfoerster.syncthing.core.domain.model.PendingFolder
import com.adamfoerster.syncthing.core.domain.model.SyncOverview
import com.adamfoerster.syncthing.core.domain.model.knownFolderOffers
import com.adamfoerster.syncthing.core.domain.preferences.AppLanguages
import kotlin.test.Test

class KnownFolderOffersTest {
    private val me = DeviceId("ME")
    private val device2 = DeviceId("D2")
    private val device3 = DeviceId("D3")
    private val stranger = DeviceId("XX")

    private fun device(id: DeviceId) = Device(id, "name-${id.value}", false, DeviceStatus.CONNECTED, null, null, emptyList())

    private fun folder(id: String, type: FolderType = FolderType.SEND_RECEIVE, members: List<DeviceId> = listOf(me, device2)) =
        Folder(id, id, "/$id", type, false, members, FolderStatus.Unknown)

    private fun offer(folderId: String, from: DeviceId) = PendingFolder(folderId, folderId, from, "name-${from.value}", null)

    private fun overview(folders: List<Folder>, offers: List<PendingFolder>) = SyncOverview(
        myId = me, myName = "me", version = "", devices = listOf(device(device2), device(device3)),
        folders = folders, pendingDevices = emptyList(), pendingFolders = offers, lastSync = null,
    )

    @Test
    fun aKnownDeviceOfferingAFolderWeAlreadyShareIsAccepted() {
        val result = overview(listOf(folder("docs")), listOf(offer("docs", device3))).knownFolderOffers()
        assertThat(result.map { it.folder.id }).containsExactly("docs")
        assertThat(result.single().devices.map { it.id }).containsExactly(device3)
    }

    @Test
    fun offersForFoldersWeDoNotHaveStayPending() {
        assertThat(overview(listOf(folder("docs")), listOf(offer("photos", device3))).knownFolderOffers()).isEmpty()
    }

    @Test
    fun offersFromDevicesWeNeverAddedStayPending() {
        assertThat(overview(listOf(folder("docs")), listOf(offer("docs", stranger))).knownFolderOffers()).isEmpty()
    }

    @Test
    fun foldersThatAreNotSendAndReceiveStayPending() {
        val folders = FolderType.entries.filter { it != FolderType.SEND_RECEIVE }.map { folder("f-${it.name}", it) }
        val offers = folders.map { offer(it.id, device3) }
        assertThat(overview(folders, offers).knownFolderOffers()).isEmpty()
    }

    @Test
    fun aDeviceAlreadyInTheFolderIsIgnored() {
        val docs = folder("docs", members = listOf(me, device2, device3))
        assertThat(overview(listOf(docs), listOf(offer("docs", device3))).knownFolderOffers()).isEmpty()
    }

    @Test
    fun severalDevicesOfferingTheSameFolderAreGroupedIntoOneChange() {
        val docs = folder("docs", members = listOf(me))
        val offers = listOf(offer("docs", device2), offer("docs", device3), offer("docs", device3))
        val result = overview(listOf(docs), offers).knownFolderOffers()
        assertThat(result.size).isEqualTo(1)
        assertThat(result.single().devices.map { it.id }).containsExactly(device2, device3)
    }

    @Test
    fun onlyTranslatedLanguagesAreAcceptedAsAnOverride() {
        assertThat(AppLanguages.sanitize("pt")).isEqualTo("pt")
        assertThat(AppLanguages.sanitize("ja")).isNull()
        assertThat(AppLanguages.sanitize(null)).isNull()
    }
}
