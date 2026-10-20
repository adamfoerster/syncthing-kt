package com.adamfoerster.syncthing.android

import android.app.Application
import android.widget.Toast
import com.adamfoerster.syncthing.android.core.AndroidSyncthingCore
import com.adamfoerster.syncthing.android.core.CoreConfig
import com.adamfoerster.syncthing.app.di.sharedModules
import com.adamfoerster.syncthing.core.data.di.AppScope
import com.adamfoerster.syncthing.core.data.network.SyncthingEndpoint
import com.adamfoerster.syncthing.core.data.repository.FolderOfferAutoAccepter
import com.adamfoerster.syncthing.core.domain.preferences.AppPreferences
import com.adamfoerster.syncthing.core.domain.repository.SyncthingCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin
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
        startAutoAccept()
    }

    /** Adds known devices to folders we already share, and says so quietly in a toast. */
    private fun startAutoAccept() {
        val accepter = getKoin().get<FolderOfferAutoAccepter>()
        accepter.start()
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            accepter.accepted.collect { accepted ->
                // Application context is not wrapped by the activity, so apply the chosen language here.
                val localized = LocaleSupport.wrap(this@SyncthingApplication, AndroidAppPreferences.storedLanguageTag(this@SyncthingApplication))
                val text = localized.getString(R.string.folder_auto_accepted, accepted.devices.joinToString { it.name }, accepted.folder.label)
                Toast.makeText(this@SyncthingApplication, text, Toast.LENGTH_LONG).show()
            }
        }
    }
}

private val androidModule = module {
    single<AppPreferences> { AndroidAppPreferences(get()) }
    single { CoreConfig(get()) }
    single { SyncthingEndpoint(get<CoreConfig>().baseUrl, get<CoreConfig>().apiKey) }
    single { AndroidSyncthingCore(get(), get(), get(AppScope)) } binds arrayOf(SyncthingCore::class)
}
