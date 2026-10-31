package com.adamfoerster.syncthing.core.data

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.adamfoerster.syncthing.core.data.dto.DeviceConfigDto
import com.adamfoerster.syncthing.core.data.dto.IgnoredFolderDto
import com.adamfoerster.syncthing.core.data.mapper.toDeclinedOffers
import com.adamfoerster.syncthing.core.domain.model.DeclinedFolderOffer
import com.adamfoerster.syncthing.core.domain.model.DeviceId
import kotlin.test.Test
import kotlin.time.Instant

class DeclinedOffersMapperTest {

    private val id = "AAAAAAA-BBBBBBB-CCCCCCC-DDDDDDD-EEEEEEE-FFFFFFF-GGGGGGG-HHHHHHH"

    @Test
    fun ignoredFoldersBecomeDeclinedOffersFromThatDevice() {
        val dto = DeviceConfigDto(
            deviceId = id,
            name = "Notebook",
            ignoredFolders = listOf(
                IgnoredFolderDto("fotos", "Fotos", "2026-10-01T12:00:00Z"),
                IgnoredFolderDto("docs", "", "0001-01-01T00:00:00Z"),
            ),
        )

        assertThat(dto.toDeclinedOffers("Notebook")).containsExactly(
            DeclinedFolderOffer("fotos", "Fotos", DeviceId(id), "Notebook", Instant.parse("2026-10-01T12:00:00Z")),
            DeclinedFolderOffer("docs", "docs", DeviceId(id), "Notebook", null),
        )
    }

    @Test
    fun aDeviceWithoutIgnoredFoldersOffersNothing() {
        assertThat(DeviceConfigDto(deviceId = id).toDeclinedOffers("Notebook")).isEmpty()
    }
}
