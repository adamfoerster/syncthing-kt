package com.adamfoerster.meshsync.feature.home.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.adamfoerster.meshsync.core.domain.model.Device
import com.adamfoerster.meshsync.core.domain.model.DeviceId
import com.adamfoerster.meshsync.core.domain.model.DeviceStatus
import com.adamfoerster.meshsync.core.domain.model.Folder
import com.adamfoerster.meshsync.core.domain.model.FolderState
import com.adamfoerster.meshsync.core.domain.model.FolderStatus
import com.adamfoerster.meshsync.core.domain.model.FolderType
import kotlin.test.Test

class SyncPercentTest {

    private fun folder(need: Long, global: Long = 1_000, state: FolderState = FolderState.IDLE) = Folder(
        id = "f", label = "f", path = "/f", type = FolderType.SEND_RECEIVE, paused = false, deviceIds = emptyList(),
        status = FolderStatus(state, global, global - need, need, 0, 0, null),
    )

    private fun device(status: DeviceStatus, completion: Double?) = Device(
        id = DeviceId("D"), name = "d", paused = false, status = status, completion = completion,
        lastSeen = null, folderIds = emptyList(),
    )

    @Test
    fun localFoldersStillReceivingUseTheirOwnProgress() {
        assertThat(syncPercentOf(listOf(folder(need = 250)), emptyList())).isEqualTo(75)
    }

    @Test
    fun aPeerStuckAtNinetyNineShowsNinetyNineNotZero() {
        val percent = syncPercentOf(listOf(folder(need = 0)), listOf(device(DeviceStatus.SYNCING, 99.99644)))

        assertThat(percent).isEqualTo(99)
    }

    @Test
    fun theSlowestPeerWins() {
        val devices = listOf(device(DeviceStatus.SYNCING, 80.0), device(DeviceStatus.SYNCING, 40.5))

        assertThat(syncPercentOf(listOf(folder(need = 0)), devices)).isEqualTo(40)
    }

    @Test
    fun nothingMovingMeansNoPercent() {
        val devices = listOf(device(DeviceStatus.CONNECTED, 100.0), device(DeviceStatus.DISCONNECTED, null))

        assertThat(syncPercentOf(listOf(folder(need = 0)), devices)).isNull()
    }
}
