package com.adamfoerster.meshsync.feature.devices.presentation.di

import com.adamfoerster.meshsync.feature.devices.presentation.add.AddDeviceViewModel
import com.adamfoerster.meshsync.feature.devices.presentation.list.DeviceListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val devicesPresentationModule = module {
    viewModelOf(::DeviceListViewModel)
    viewModel { (prefillId: String?, prefillName: String?) -> AddDeviceViewModel(prefillId, prefillName, get(), get(), get()) }
}
