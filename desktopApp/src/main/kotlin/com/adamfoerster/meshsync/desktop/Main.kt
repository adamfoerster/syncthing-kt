package com.adamfoerster.meshsync.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberWindowState
import com.adamfoerster.meshsync.app.App
import com.adamfoerster.meshsync.app.di.sharedModules
import com.adamfoerster.meshsync.core.data.di.AppScope
import com.adamfoerster.meshsync.core.data.network.SyncthingEndpoint
import com.adamfoerster.meshsync.core.data.repository.FolderOfferAutoAccepter
import com.adamfoerster.meshsync.core.domain.preferences.AppPreferences
import com.adamfoerster.meshsync.core.domain.repository.SyncthingCore
import com.adamfoerster.meshsync.desktop.core.CoreMessages
import com.adamfoerster.meshsync.desktop.core.DesktopCoreConfig
import com.adamfoerster.meshsync.desktop.core.DesktopSyncthingCore
import com.adamfoerster.meshsync.desktop.resources.Res
import com.adamfoerster.meshsync.desktop.resources.core_exit
import com.adamfoerster.meshsync.desktop.resources.core_start_timeout
import com.adamfoerster.meshsync.desktop.resources.tray_autostart
import com.adamfoerster.meshsync.desktop.resources.tray_open
import com.adamfoerster.meshsync.desktop.resources.tray_pause
import com.adamfoerster.meshsync.desktop.resources.tray_quit
import com.adamfoerster.meshsync.desktop.resources.tray_resume
import com.adamfoerster.meshsync.desktop.resources.tray_status_failed
import com.adamfoerster.meshsync.desktop.resources.tray_status_missing
import com.adamfoerster.meshsync.desktop.resources.tray_status_paused
import com.adamfoerster.meshsync.desktop.resources.tray_status_running
import com.adamfoerster.meshsync.desktop.resources.tray_status_starting
import com.adamfoerster.meshsync.desktop.resources.window_title
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.startKoin
import org.koin.dsl.binds
import org.koin.dsl.module
import java.util.Locale

fun main(args: Array<String>) {
    val dataDir = DesktopPaths.dataDir()
    val showRequests = mutableStateOf(0)
    val instance = SingleInstance(dataDir) { showRequests.value += 1 }
    // A second launch just asks the running app to show itself.
    if (!instance.acquire()) return

    val systemLocale = Locale.getDefault()
    val config = DesktopCoreConfig(dataDir)
    val preferences = DesktopAppPreferences(dataDir)
    applyLanguage(preferences.languageTag.value, systemLocale)

    val trayAvailable = isTraySupported
    val startMinimized = WindowsAutostart.MINIMIZED_FLAG in args && trayAvailable
    var windowVisible by mutableStateOf(!startMinimized)

    val autostart: Autostart = WindowsAutostart.currentLauncher()?.let(::WindowsAutostart) ?: NoAutostart
    // Not marked as done for development runs (NoAutostart), so the first packaged run still applies it.
    if (autostart !== NoAutostart && !preferences.autostartInitialized) {
        // First run: start with Windows, which the tray menu lets the user turn off.
        autostart.setEnabled(true)
        preferences.autostartInitialized = true
    }

    // Texts are looked up when the failure happens, so they follow the language chosen in Settings.
    val messages = CoreMessages(
        exited = { code -> runBlocking { getString(Res.string.core_exit, code.toString()) } },
        startTimeout = runBlocking { getString(Res.string.core_start_timeout) },
    )
    val koin = startKoin {
        modules(
            sharedModules + module {
                single<AppPreferences> { preferences }
                single { SyncthingEndpoint(config.baseUrl, config.apiKey) }
                single { DesktopSyncthingCore(DesktopPaths.syncthingBinary(), config, get(AppScope), messages = messages) } binds
                    arrayOf(SyncthingCore::class)
            },
        )
    }.koin
    val core = koin.get<DesktopSyncthingCore>()
    koin.get<FolderOfferAutoAccepter>().start()

    // If the JVM goes away without the Quit item (logoff, taskkill), still stop the core cleanly.
    Runtime.getRuntime().addShutdownHook(Thread { core.shutdownAndWait() })
    core.startIfWanted()

    val platformActions = DesktopPlatformActions()

    application {
        val scope = rememberCoroutineScope()
        val coreState by core.state.collectAsState()
        val language by preferences.languageTag.collectAsState()
        val themeMode by preferences.themeMode.collectAsState()
        val status = TrayStatus.of(coreState)
        val windowState = rememberWindowState(size = DpSize(1100.dp, 780.dp))
        var autostartOn by remember { mutableStateOf(autostart.isEnabled()) }

        fun quit() {
            scope.launch(Dispatchers.IO) {
                core.shutdownAndWait()
                instance.close()
                exitApplication()
            }
        }

        LaunchedEffect(showRequests.value) {
            if (showRequests.value > 0) windowVisible = true
        }

        if (trayAvailable) {
            val tooltip = stringResource(
                when (status) {
                    TrayStatus.Running -> Res.string.tray_status_running
                    TrayStatus.Starting -> Res.string.tray_status_starting
                    TrayStatus.Paused -> Res.string.tray_status_paused
                    TrayStatus.Failed -> Res.string.tray_status_failed
                    TrayStatus.Missing -> Res.string.tray_status_missing
                },
            )
            Tray(
                icon = remember(status) { StatusIcon(status) },
                tooltip = tooltip,
                onAction = { windowVisible = true },
                menu = {
                    Item(stringResource(Res.string.tray_open), onClick = { windowVisible = true })
                    Item(
                        stringResource(if (status.canPause) Res.string.tray_pause else Res.string.tray_resume),
                        onClick = { if (status.canPause) core.stop() else core.start() },
                    )
                    CheckboxItem(
                        stringResource(Res.string.tray_autostart),
                        checked = autostartOn,
                        onCheckedChange = { enabled ->
                            autostart.setEnabled(enabled)
                            autostartOn = enabled
                        },
                    )
                    Separator()
                    Item(stringResource(Res.string.tray_quit), onClick = ::quit)
                },
            )
        }

        // Re-created when the language changes, because Compose resources read the JVM default locale.
        key(language) {
            remember { applyLanguage(language, systemLocale) }
            Window(
                onCloseRequest = { if (trayAvailable) windowVisible = false else quit() },
                visible = windowVisible,
                title = stringResource(Res.string.window_title),
                icon = remember(status) { StatusIcon(status) },
                state = windowState,
            ) {
                App(platformActions, themeMode.isDark(isSystemInDarkTheme()))
            }
        }
    }
}

/** Sets the JVM default locale, which Compose resources use; null follows the system language. */
private fun applyLanguage(tag: String?, systemLocale: Locale) {
    Locale.setDefault(if (tag == null) systemLocale else Locale.forLanguageTag(tag))
}
