package com.adamfoerster.syncthing.android

import android.app.Application
import com.adamfoerster.syncthing.android.core.AndroidSyncthingCore
import com.adamfoerster.syncthing.android.core.CoreConfig
import com.adamfoerster.syncthing.app.di.sharedModules
import com.adamfoerster.syncthing.core.data.di.AppScope
import com.adamfoerster.syncthing.core.data.network.SyncthingEndpoint
import com.adamfoerster.syncthing.core.domain.repository.SyncthingCore
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.binds
import org.koin.dsl.module

class SyncthingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SyncthingApplication)
            // Later modules override earlier ones (debug's demo mode replaces the core and REST client).
            modules(sharedModules + androidModule + buildTypeModules(this@SyncthingApplication))
        }
    }
}

private val androidModule = module {
    single { CoreConfig(get()) }
    single { SyncthingEndpoint(get<CoreConfig>().baseUrl, get<CoreConfig>().apiKey) }
    single { AndroidSyncthingCore(get(), get(), get(AppScope)) } binds arrayOf(SyncthingCore::class)
}
