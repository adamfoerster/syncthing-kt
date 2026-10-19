package com.adamfoerster.syncthing.android

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.adamfoerster.syncthing.android.core.AndroidSyncthingCore
import com.adamfoerster.syncthing.app.App
import com.adamfoerster.syncthing.core.presentation.PlatformActions
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val core: AndroidSyncthingCore by inject()

    private var pendingScan: ((String?) -> Unit)? = null
    private val scanner = registerForActivityResult(ScanContract()) { result ->
        pendingScan?.invoke(result.contents)
        pendingScan = null
    }
    private val legacyStorage = registerForActivityResult(ActivityResultContracts.RequestPermission()) { core.startIfWanted() }
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val platformActions = object : PlatformActions {
        override fun scanQrCode(onResult: (String?) -> Unit) {
            pendingScan = onResult
            scanner.launch(
                ScanOptions()
                    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    .setPrompt(getString(R.string.scan_prompt))
                    .setBeepEnabled(false)
                    .setOrientationLocked(false),
            )
        }

        override fun requestStorageAccess() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))
                runCatching { startActivity(intent) }
                    .onFailure { startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
            } else {
                legacyStorage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        override val defaultFolderRoot: String = Environment.getExternalStorageDirectory().absolutePath
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && savedInstanceState == null) {
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { App(platformActions) }
    }

    override fun onResume() {
        super.onResume()
        // Also picks up the all-files permission when the user comes back from system settings.
        core.startIfWanted()
    }
}
