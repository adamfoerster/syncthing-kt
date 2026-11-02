package com.adamfoerster.meshsync.feature.conflicts.data.di

import okio.FileSystem

internal actual val systemFileSystem: FileSystem = FileSystem.SYSTEM
