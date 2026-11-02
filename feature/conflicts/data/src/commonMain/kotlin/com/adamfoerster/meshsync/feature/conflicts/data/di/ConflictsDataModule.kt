package com.adamfoerster.meshsync.feature.conflicts.data.di

import com.adamfoerster.meshsync.core.data.di.AppScope
import com.adamfoerster.meshsync.core.domain.repository.ConflictSummary
import com.adamfoerster.meshsync.feature.conflicts.data.FileSystemConflictRepository
import com.adamfoerster.meshsync.feature.conflicts.domain.ConflictRepository
import okio.FileSystem
import org.koin.dsl.binds
import org.koin.dsl.module

internal expect val systemFileSystem: FileSystem

val conflictsDataModule = module {
    single { FileSystemConflictRepository(systemFileSystem, get(), get(AppScope)) } binds
        arrayOf(ConflictRepository::class, ConflictSummary::class)
}
