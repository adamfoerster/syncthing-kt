package com.adamfoerster.meshsync.android

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.adamfoerster.meshsync.core.domain.util.DataError
import com.adamfoerster.meshsync.core.domain.util.Result
import com.adamfoerster.meshsync.feature.files.domain.IncomingShare
import com.adamfoerster.meshsync.feature.files.domain.ShareImporter
import com.adamfoerster.meshsync.feature.files.domain.sharedFileName
import com.adamfoerster.meshsync.feature.files.domain.uniqueName
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Copies what the system share sheet handed over (content URIs and text) into a synced folder. */
class AndroidShareImporter(private val resolver: ContentResolver) : ShareImporter {

    override suspend fun import(share: IncomingShare, targetDirectory: String): Result<Int, DataError.Local> =
        withContext(Dispatchers.IO) {
            try {
                val directory = File(targetDirectory)
                if (!directory.isDirectory && !directory.mkdirs()) return@withContext Result.Failure(DataError.Local.NOT_FOUND)
                var copied = 0
                share.uris.forEach { raw ->
                    val uri = Uri.parse(raw)
                    val target = File(directory, uniqueName(sharedFileName(displayName(uri), uri.lastPathSegment.orEmpty().substringAfterLast('/').ifBlank { "shared-file" })) { File(directory, it).exists() })
                    val input = resolver.openInputStream(uri) ?: return@withContext Result.Failure(DataError.Local.NOT_FOUND)
                    input.use { source -> target.outputStream().use { source.copyTo(it) } }
                    copied++
                }
                share.text?.takeIf { it.isNotEmpty() }?.let { text ->
                    File(directory, uniqueName("shared-text.txt") { File(directory, it).exists() }).writeText(text)
                    copied++
                }
                Result.Success(copied)
            } catch (_: SecurityException) {
                Result.Failure(DataError.Local.PERMISSION_DENIED)
            } catch (_: IOException) {
                Result.Failure(DataError.Local.IO)
            }
        }

    private fun displayName(uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()
}
