package com.adamfoerster.meshsync.feature.settings.presentation.di

import com.adamfoerster.meshsync.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsPresentationModule = module {
    viewModelOf(::SettingsViewModel)
}
