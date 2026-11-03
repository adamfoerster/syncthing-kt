package com.adamfoerster.meshsync.feature.files.data.di

import com.adamfoerster.meshsync.feature.files.data.OkioFileManager
import com.adamfoerster.meshsync.feature.files.domain.FileManager
import okio.FileSystem
import org.koin.dsl.module

internal expect val systemFileSystem: FileSystem

val filesDataModule = module {
    single<FileManager> { OkioFileManager(systemFileSystem) }
}
