package com.adamfoerster.syncthing.core.domain.repository

import com.adamfoerster.syncthing.core.domain.model.DeviceId
import com.adamfoerster.syncthing.core.domain.model.FileInfo
import com.adamfoerster.syncthing.core.domain.model.FolderType
import com.adamfoerster.syncthing.core.domain.model.SyncOverview
import com.adamfoerster.syncthing.core.domain.util.DataError
import com.adamfoerster.syncthing.core.domain.util.EmptyResult
import com.adamfoerster.syncthing.core.domain.util.Result
import kotlinx.coroutines.flow.StateFlow

interface SyncthingRepository {
    /** Live snapshot; null until the core answers for the first time. */
    val overview: StateFlow<SyncOverview?>

    suspend fun refresh()

    suspend fun addDevice(id: DeviceId, name: String): EmptyResult<DataError.Remote>
    suspend fun removeDevice(id: DeviceId): EmptyResult<DataError.Remote>
    suspend fun setDevicePaused(id: DeviceId, paused: Boolean): EmptyResult<DataError.Remote>
    suspend fun dismissPendingDevice(id: DeviceId): EmptyResult<DataError.Remote>

    suspend fun addFolder(
        id: String,
        label: String,
        path: String,
        type: FolderType,
        devices: List<DeviceId>,
    ): EmptyResult<DataError.Remote>

    suspend fun setFolderDevices(folderId: String, devices: List<DeviceId>): EmptyResult<DataError.Remote>
    suspend fun setFolderPaused(folderId: String, paused: Boolean): EmptyResult<DataError.Remote>
    suspend fun dismissPendingFolder(folderId: String, device: DeviceId): EmptyResult<DataError.Remote>

    suspend fun setMyName(name: String): EmptyResult<DataError.Remote>

    /** Asks the core to rescan [subPath] (or the whole folder) right away. */
    suspend fun scan(folderId: String, subPath: String? = null): EmptyResult<DataError.Remote>

    /** The folder's ignore rules (its `.stignore`), one entry per line; empty when there are none. */
    suspend fun ignorePatterns(folderId: String): Result<List<String>, DataError.Remote>

    /** Replaces the folder's ignore rules; the core rescans on its own to apply them. */
    suspend fun setIgnorePatterns(folderId: String, lines: List<String>): EmptyResult<DataError.Remote>

    suspend fun fileInfo(folderId: String, path: String): Result<FileInfo, DataError.Remote>
}
