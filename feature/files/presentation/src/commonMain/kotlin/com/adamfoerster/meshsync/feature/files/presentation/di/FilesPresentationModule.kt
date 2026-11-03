package com.adamfoerster.meshsync.feature.files.presentation.di

import com.adamfoerster.meshsync.feature.files.presentation.FilesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val filesPresentationModule = module {
    viewModel { (root: String) -> FilesViewModel(root, get(), get(), get()) }
}
