package com.adamfoerster.syncthing.app.di

import com.adamfoerster.syncthing.core.data.di.coreDataModule
import com.adamfoerster.syncthing.feature.conflicts.data.di.conflictsDataModule
import com.adamfoerster.syncthing.feature.conflicts.presentation.di.conflictsPresentationModule
import com.adamfoerster.syncthing.feature.devices.presentation.di.devicesPresentationModule
import com.adamfoerster.syncthing.feature.folders.presentation.di.foldersPresentationModule
import com.adamfoerster.syncthing.feature.home.presentation.di.homePresentationModule
import com.adamfoerster.syncthing.feature.settings.presentation.di.settingsPresentationModule
import com.adamfoerster.syncthing.feature.sharing.presentation.di.sharingPresentationModule

/** Everything but the platform module, which provides SyncthingCore and SyncthingEndpoint. */
val sharedModules = listOf(
    coreDataModule,
    conflictsDataModule,
    conflictsPresentationModule,
    homePresentationModule,
    sharingPresentationModule,
    foldersPresentationModule,
    devicesPresentationModule,
    settingsPresentationModule,
)
