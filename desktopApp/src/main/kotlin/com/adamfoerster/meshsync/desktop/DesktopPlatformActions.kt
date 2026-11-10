package com.adamfoerster.meshsync.desktop

import com.adamfoerster.meshsync.core.presentation.PlatformActions
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.UIManager

/**
 * [PlatformActions] for the desktop: native folder chooser, Explorer instead of a share sheet, and no
 * camera (the device ID is typed or pasted instead). Files are never behind a permission wall here.
 */
class DesktopPlatformActions : PlatformActions {
    override fun scanQrCode(onResult: (String?) -> Unit) = onResult(null)

    override fun pickFolder(onResult: (String?) -> Unit) {
        SwingUtilities.invokeLater {
            runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }
            val chooser = JFileChooser(defaultFolderRoot).apply {
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                dialogTitle = null
            }
            val chosen = if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile.absolutePath else null
            onResult(chosen)
        }
    }

    /** There is no share sheet on Windows: show the files in Explorer, which has Share and Copy. */
    override fun shareFiles(paths: List<String>) {
        val command = explorerCommand(paths) ?: return
        runCatching { ProcessBuilder(command).start() }
    }

    override fun requestStorageAccess() = Unit

    override fun hasStorageAccess(): Boolean = true

    override val defaultFolderRoot: String = System.getProperty("user.home")

    companion object {
        /** `explorer.exe /select,<file>` for one file, the parent folder for several; null for none. */
        fun explorerCommand(paths: List<String>): List<String>? = when {
            paths.isEmpty() -> null
            paths.size == 1 -> listOf("explorer.exe", "/select,${File(paths.single()).absolutePath}")
            else -> File(paths.first()).absoluteFile.parentFile?.let { listOf("explorer.exe", it.path) }
        }
    }
}
