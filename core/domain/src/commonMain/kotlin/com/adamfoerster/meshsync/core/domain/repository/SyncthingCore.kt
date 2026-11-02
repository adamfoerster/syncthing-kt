package com.adamfoerster.meshsync.core.domain.repository

import com.adamfoerster.meshsync.core.domain.model.CoreState
import kotlinx.coroutines.flow.StateFlow

/** Controls the Syncthing core process. Implemented by the platform (Android service). */
interface SyncthingCore {
    val state: StateFlow<CoreState>
    fun start()
    fun stop()
    fun restart()
}
