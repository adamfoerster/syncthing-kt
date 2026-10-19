package com.adamfoerster.syncthing.feature.sharing.presentation.di

import com.adamfoerster.syncthing.feature.sharing.presentation.SharingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sharingPresentationModule = module {
    viewModelOf(::SharingViewModel)
}
