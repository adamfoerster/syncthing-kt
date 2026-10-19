package com.adamfoerster.syncthing.feature.conflicts.data.di

import okio.FileSystem

internal actual val systemFileSystem: FileSystem = FileSystem.SYSTEM
