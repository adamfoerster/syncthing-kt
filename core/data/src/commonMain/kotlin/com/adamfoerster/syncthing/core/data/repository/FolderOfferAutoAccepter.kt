package com.adamfoerster.syncthing.core.data.repository

import com.adamfoerster.syncthing.core.domain.model.KnownFolderOffer
import com.adamfoerster.syncthing.core.domain.model.knownFolderOffers
import com.adamfoerster.syncthing.core.domain.preferences.AppPreferences
import com.adamfoerster.syncthing.core.domain.repository.SyncthingRepository
import com.adamfoerster.syncthing.core.domain.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Answers "yes" to folder offers that repeat what the user already shares: when a known device offers a
 * folder we already sync with another device, it is added to that folder instead of asking again.
 * Does nothing while [AppPreferences.autoAcceptKnownFolders] is off.
 */
class FolderOfferAutoAccepter(
    private val repository: SyncthingRepository,
    private val preferences: AppPreferences,
    private val scope: CoroutineScope,
) {
    private val _accepted = MutableSharedFlow<KnownFolderOffer>(extraBufferCapacity = 8)

    /** Emits once per folder after its new devices were added, so the UI can tell the user quietly. */
    val accepted: SharedFlow<KnownFolderOffer> = _accepted

    // Offers answered during this run, so one snapshot cannot trigger the same change twice. Entries
    // leave once the offer is gone, which lets a device that is removed and offers again be re-added.
    private val attempted = mutableSetOf<String>()

    fun start() {
        scope.launch {
            combine(repository.overview, preferences.autoAcceptKnownFolders) { overview, enabled ->
                if (enabled) overview?.knownFolderOffers().orEmpty() else emptyList()
            }.collect { offers ->
                attempted.retainAll(offers.flatMap { it.keys() }.toSet())
                offers.forEach { accept(it) }
            }
        }
    }

    private suspend fun accept(offer: KnownFolderOffer) {
        val keys = offer.keys()
        if (!attempted.addAll(keys)) return
        val members = (offer.folder.deviceIds + offer.devices.map { it.id }).distinct()
        when (repository.setFolderDevices(offer.folder.id, members)) {
            is Result.Success -> _accepted.emit(offer)
            // Forget it so the next snapshot tries again; the offer stays visible for a manual answer meanwhile.
            is Result.Failure -> attempted.removeAll(keys.toSet())
        }
    }

    private fun KnownFolderOffer.keys(): List<String> = devices.map { "${folder.id}/${it.id.value}" }
}
