package com.adamfoerster.syncthing.android

import android.content.Context
import com.adamfoerster.syncthing.android.demo.DemoData
import com.adamfoerster.syncthing.android.demo.demoModule
import org.koin.core.module.Module
import java.io.File

/**
 * Debug builds can run without the Go core: create `files/demo` and restart the app
 * (`adb shell run-as com.adamfoerster.syncthing.debug touch files/demo`). The REST side is faked,
 * but conflicts are real files on disk, so detection and resolution run for real.
 */
internal fun buildTypeModules(context: Context): List<Module> {
    if (!File(context.filesDir, "demo").exists()) return emptyList()
    DemoData.seedFiles(context)
    return listOf(demoModule)
}
