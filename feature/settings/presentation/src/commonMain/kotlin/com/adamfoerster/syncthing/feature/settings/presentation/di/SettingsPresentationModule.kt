package com.adamfoerster.syncthing.feature.settings.presentation.di

import com.adamfoerster.syncthing.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsPresentationModule = module {
    viewModelOf(::SettingsViewModel)
}
