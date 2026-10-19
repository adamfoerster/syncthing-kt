package com.adamfoerster.syncthing.core.data.di

import com.adamfoerster.syncthing.core.data.network.HttpClientFactory
import com.adamfoerster.syncthing.core.data.network.SyncthingApi
import com.adamfoerster.syncthing.core.data.repository.KtorSyncthingRepository
import com.adamfoerster.syncthing.core.domain.repository.SyncthingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Process-wide scope for work that outlives screens (polling, conflict scans). */
val AppScope = named("AppScope")

/** Expects a [com.adamfoerster.syncthing.core.data.network.SyncthingEndpoint] and a SyncthingCore from the platform module. */
val coreDataModule = module {
    single(AppScope) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { HttpClientFactory.create(get()) }
    single { SyncthingApi(get()) }
    single<SyncthingRepository> { KtorSyncthingRepository(get(), get(), get(AppScope)) }
}
