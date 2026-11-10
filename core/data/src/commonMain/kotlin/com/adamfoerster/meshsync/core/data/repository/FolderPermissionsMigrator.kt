package com.adamfoerster.meshsync.core.data.repository

import com.adamfoerster.meshsync.core.data.network.SyncthingApi
import com.adamfoerster.meshsync.core.domain.util.Result
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Turns `ignorePerms` on for every folder that has it off.
 *
 * Android shared storage reports every file as `0660`, which differs from what other platforms
 * record. Without `ignorePerms` the phone publishes a metadata-only version for each file it
 * receives, and that version races with the next edit made elsewhere, producing `.sync-conflict`
 * copies nobody edited. Each folder is tried once per process, so a refused patch is not retried
 * in a tight loop.
 */
class FolderPermissionsMigrator(private val api: SyncthingApi) {

    private val attempted = mutableSetOf<String>()

    /** Patches the folders that need it; returns their ids. Failures are skipped silently. */
    suspend fun run(): List<String> {
        val config = (api.config() as? Result.Success)?.data ?: return emptyList()
        val pending = config.folders.filter { !it.ignorePerms && it.id !in attempted }
        return pending.mapNotNull { folder ->
            attempted += folder.id
            val patched = api.patchFolder(folder.id, buildJsonObject { put("ignorePerms", true) })
            folder.id.takeIf { patched is Result.Success }
        }
    }
}
