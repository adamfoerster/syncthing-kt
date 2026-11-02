package com.adamfoerster.meshsync.feature.sharing.presentation.di

import com.adamfoerster.meshsync.feature.sharing.presentation.SharingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sharingPresentationModule = module {
    viewModelOf(::SharingViewModel)
}
