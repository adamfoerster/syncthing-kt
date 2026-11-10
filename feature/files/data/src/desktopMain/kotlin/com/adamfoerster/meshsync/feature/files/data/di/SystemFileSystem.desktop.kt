package com.adamfoerster.meshsync.feature.files.data.di

import okio.FileSystem

internal actual val systemFileSystem: FileSystem = FileSystem.SYSTEM
