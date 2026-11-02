package com.adamfoerster.meshsync.app.di

import com.adamfoerster.meshsync.core.data.di.coreDataModule
import com.adamfoerster.meshsync.feature.conflicts.data.di.conflictsDataModule
import com.adamfoerster.meshsync.feature.conflicts.presentation.di.conflictsPresentationModule
import com.adamfoerster.meshsync.feature.devices.presentation.di.devicesPresentationModule
import com.adamfoerster.meshsync.feature.folders.presentation.di.foldersPresentationModule
import com.adamfoerster.meshsync.feature.home.presentation.di.homePresentationModule
import com.adamfoerster.meshsync.feature.onboarding.presentation.di.onboardingPresentationModule
import com.adamfoerster.meshsync.feature.settings.presentation.di.settingsPresentationModule
import com.adamfoerster.meshsync.feature.sharing.presentation.di.sharingPresentationModule

/** Everything but the platform module, which provides SyncthingCore and SyncthingEndpoint. */
val sharedModules = listOf(
    coreDataModule,
    conflictsDataModule,
    conflictsPresentationModule,
    onboardingPresentationModule,
    homePresentationModule,
    sharingPresentationModule,
    foldersPresentationModule,
    devicesPresentationModule,
    settingsPresentationModule,
)
