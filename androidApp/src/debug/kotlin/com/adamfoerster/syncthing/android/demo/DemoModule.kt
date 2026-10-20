package com.adamfoerster.syncthing.android.demo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.adamfoerster.syncthing.core.domain.model.CoreState
import com.adamfoerster.syncthing.core.domain.model.Device
import com.adamfoerster.syncthing.core.domain.model.DeviceId
import com.adamfoerster.syncthing.core.domain.model.DeviceStatus
import com.adamfoerster.syncthing.core.domain.model.FileInfo
import com.adamfoerster.syncthing.core.domain.model.Folder
import com.adamfoerster.syncthing.core.domain.model.FolderState
import com.adamfoerster.syncthing.core.domain.model.FolderStatus
import com.adamfoerster.syncthing.core.domain.model.FolderType
import com.adamfoerster.syncthing.core.domain.model.SyncOverview
import com.adamfoerster.syncthing.core.domain.repository.SyncthingCore
import com.adamfoerster.syncthing.core.domain.repository.SyncthingRepository
import com.adamfoerster.syncthing.core.domain.util.DataError
import com.adamfoerster.syncthing.core.domain.util.EmptyResult
import com.adamfoerster.syncthing.core.domain.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.io.File
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

val demoModule = module {
    single<SyncthingCore> { DemoCore() }
    single<SyncthingRepository> { DemoRepository(DemoData.documents(androidContext()).absolutePath) }
}

/** The mockups' scenario: three devices, four folders, two conflicts in "Documents". */
object DemoData {
    val me = DeviceId("ESTEPHN-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA")
    val laptop = DeviceId("NOTEBKA-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB")
    val office = DeviceId("PCESCRT-CCCCCCC-CCCCCCC-CCCCCCC-CCCCCCC-CCCCCCC-CCCCCCC-CCCCCCC")
    val tablet = DeviceId("TABLETS-DDDDDDD-DDDDDDD-DDDDDDD-DDDDDDD-DDDDDDD-DDDDDDD-DDDDDDD")

    fun documents(context: Context) = File(context.getExternalFilesDir(null), "Documents")

    fun seedFiles(context: Context) {
        val dir = documents(context).apply { mkdirs() }
        if (dir.listFiles()?.isNotEmpty() == true) return
        File(dir, "shopping-list.txt").writeText(
            """
            Shopping list — Saturday

            Fruit
            - Apple
            - Banana

            - Whole milk (2 cartons)
            - Sandwich bread

            Cleaning
            - Dish soap
            - Laundry powder

            Remember: pay the electricity bill
            """.trimIndent() + "\n",
        )
        File(dir, "shopping-list.sync-conflict-20261019-143500-${laptop.short}.txt").writeText(
            """
            Shopping list — Saturday

            Fruit
            - Apple
            - Banana

            - Skim milk (3 cartons)
            - Whole-wheat bread

            Cleaning
            - Dish soap
            - Liquid detergent
            - Fabric softener

            Remember: pay the electricity bill
            """.trimIndent() + "\n",
        )
        writeImage(File(dir, "invitation.png"), 540, 675, Color.rgb(0, 140, 160), "A")
        writeImage(File(dir, "invitation.sync-conflict-20261019-144100-${office.short}.png"), 1080, 1350, Color.rgb(200, 40, 100), "B")
    }

    private fun writeImage(file: File, width: Int, height: Int, color: Int, label: String) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(color)
            drawText(label, width / 2f, height / 2f, Paint().apply { this.color = Color.WHITE; textSize = width / 3f; textAlign = Paint.Align.CENTER })
        }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

private class DemoCore : SyncthingCore {
    override val state: StateFlow<CoreState> = MutableStateFlow(CoreState.Running)
    override fun start() = Unit
    override fun stop() = Unit
    override fun restart() = Unit
}

private class DemoRepository(documentsPath: String) : SyncthingRepository {
    private val now = Clock.System.now()

    override val overview = MutableStateFlow<SyncOverview?>(
        SyncOverview(
            myId = DemoData.me,
            myName = "Adam’s Pixel",
            version = "v2.1.6 (demo)",
            devices = listOf(
                Device(DemoData.laptop, "Ana’s Laptop", false, DeviceStatus.CONNECTED, 100.0, now, listOf("fotos", "docs", "whats")),
                Device(DemoData.office, "Office PC", false, DeviceStatus.SYNCING, 64.0, now, listOf("fotos", "docs")),
                Device(DemoData.tablet, "Living Room Tablet", false, DeviceStatus.DISCONNECTED, null, now - 2.days, listOf("docs", "musica")),
            ),
            folders = listOf(
                folder("fotos", "Camera Photos", "/storage/emulated/0/DCIM", listOf(DemoData.laptop, DemoData.office), 4_500_000_000, 0),
                folder("docs", "Documents", documentsPath, listOf(DemoData.laptop, DemoData.office, DemoData.tablet), 327_000_000, 0),
                folder("whats", "WhatsApp Backup", "/storage/emulated/0/WhatsApp", listOf(DemoData.laptop), 1_180_000_000, 430_000_000),
                folder("musica", "Music", "/storage/emulated/0/Music", listOf(DemoData.tablet), 20_000_000_000, 0),
            ),
            pendingDevices = emptyList(),
            pendingFolders = emptyList(),
            lastSync = now - 2.minutes,
        ),
    )

    private fun folder(id: String, label: String, path: String, devices: List<DeviceId>, global: Long, need: Long) = Folder(
        id = id, label = label, path = path, type = FolderType.SEND_RECEIVE, paused = false, deviceIds = devices,
        status = FolderStatus(if (need > 0) FolderState.SYNCING else FolderState.IDLE, global, global - need, need, if (need > 0) 12 else 0, 1, null),
    )

    private val ok: EmptyResult<DataError.Remote> = Result.Success(Unit)

    override suspend fun refresh() = Unit
    override suspend fun addDevice(id: DeviceId, name: String): EmptyResult<DataError.Remote> {
        overview.update { o -> o?.copy(devices = o.devices + Device(id, name, false, DeviceStatus.DISCONNECTED, null, null, emptyList())) }
        return ok
    }
    override suspend fun removeDevice(id: DeviceId): EmptyResult<DataError.Remote> {
        overview.update { o -> o?.copy(devices = o.devices.filterNot { it.id == id }) }
        return ok
    }
    override suspend fun setDevicePaused(id: DeviceId, paused: Boolean): EmptyResult<DataError.Remote> {
        overview.update { o ->
            o?.copy(devices = o.devices.map { if (it.id == id) it.copy(paused = paused, status = if (paused) DeviceStatus.PAUSED else DeviceStatus.CONNECTED) else it })
        }
        return ok
    }
    override suspend fun renameDevice(id: DeviceId, name: String): EmptyResult<DataError.Remote> {
        overview.update { o -> o?.copy(devices = o.devices.map { if (it.id == id) it.copy(name = name) else it }) }
        return ok
    }
    override suspend fun dismissPendingDevice(id: DeviceId) = ok
    override suspend fun addFolder(id: String, label: String, path: String, type: FolderType, devices: List<DeviceId>): EmptyResult<DataError.Remote> {
        overview.update { o -> o?.copy(folders = o.folders + folder(id, label, path, devices, 0, 0)) }
        return ok
    }
    override suspend fun setFolderDevices(folderId: String, devices: List<DeviceId>): EmptyResult<DataError.Remote> {
        // Like the real core: an offer disappears once its device is in the folder.
        overview.update { o ->
            o?.copy(
                folders = o.folders.map { if (it.id == folderId) it.copy(deviceIds = devices) else it },
                pendingFolders = o.pendingFolders.filterNot { it.folderId == folderId && it.offeredBy in devices },
            )
        }
        return ok
    }
    override suspend fun setFolderPaused(folderId: String, paused: Boolean) = ok
    override suspend fun dismissPendingFolder(folderId: String, device: DeviceId) = ok
    override suspend fun setMyName(name: String): EmptyResult<DataError.Remote> {
        overview.update { it?.copy(myName = name) }
        return ok
    }
    private val ignores = mutableMapOf("docs" to listOf("*.tmp", ".DS_Store"))

    override suspend fun ignorePatterns(folderId: String): Result<List<String>, DataError.Remote> =
        Result.Success(ignores[folderId].orEmpty())
    override suspend fun setIgnorePatterns(folderId: String, lines: List<String>): EmptyResult<DataError.Remote> {
        ignores[folderId] = lines
        return ok
    }
    override suspend fun scan(folderId: String, subPath: String?) = ok
    override suspend fun fileInfo(folderId: String, path: String): Result<FileInfo, DataError.Remote> =
        Result.Success(FileInfo(path, 0, null, DemoData.me.short))
}
