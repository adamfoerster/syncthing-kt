package com.adamfoerster.meshsync.android

import android.content.Context
import org.koin.core.module.Module

/** Release builds have no extra modules. */
internal fun buildTypeModules(context: Context): List<Module> = emptyList()
