package com.adamfoerster.meshsync.desktop

import com.adamfoerster.meshsync.core.presentation.PlatformActions
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.UIManager

/**
 * [PlatformActions] for the desktop: native folder chooser, Explorer/Finder instead of a share sheet, and no
 * camera (the device ID is typed or pasted instead). Files are never behind a permission wall here.
 */
class DesktopPlatformActions(private val os: HostOs = HostOs.current()) : PlatformActions {
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

    /** No share sheet here: show the files in Explorer or Finder, which have Share and Copy. */
    override fun shareFiles(paths: List<String>) {
        val command = revealCommand(paths, os) ?: return
        runCatching { ProcessBuilder(command).start() }
    }

    override fun requestStorageAccess() = Unit

    override fun hasStorageAccess(): Boolean = true

    override val defaultFolderRoot: String = System.getProperty("user.home")

    companion object {
        /**
         * Selects one file in the file manager, or opens the parent folder for several; null for none.
         * Windows: `explorer.exe /select,<file>`; macOS: `open -R <file>` / `open <folder>`.
         */
        fun revealCommand(paths: List<String>, os: HostOs): List<String>? {
            if (paths.isEmpty()) return null
            val opener = if (os == HostOs.MacOs) "open" else "explorer.exe"
            if (paths.size == 1) {
                val file = File(paths.single()).absolutePath
                return if (os == HostOs.MacOs) listOf(opener, "-R", file) else listOf(opener, "/select,$file")
            }
            return File(paths.first()).absoluteFile.parentFile?.let { listOf(opener, it.path) }
        }
    }
}
