package com.adamfoerster.syncthing.feature.conflicts.data.di

import com.adamfoerster.syncthing.core.data.di.AppScope
import com.adamfoerster.syncthing.core.domain.repository.ConflictSummary
import com.adamfoerster.syncthing.feature.conflicts.data.FileSystemConflictRepository
import com.adamfoerster.syncthing.feature.conflicts.domain.ConflictRepository
import okio.FileSystem
import org.koin.dsl.binds
import org.koin.dsl.module

internal expect val systemFileSystem: FileSystem

val conflictsDataModule = module {
    single { FileSystemConflictRepository(systemFileSystem, get(), get(AppScope)) } binds
        arrayOf(ConflictRepository::class, ConflictSummary::class)
}
