package com.adamfoerster.meshsync.feature.files.presentation.di

import com.adamfoerster.meshsync.feature.files.presentation.FilesViewModel
import com.adamfoerster.meshsync.feature.files.presentation.ShareTargetViewModel
import com.adamfoerster.meshsync.feature.files.domain.IncomingShareInbox
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val filesPresentationModule = module {
    single { IncomingShareInbox() }
    viewModel { ShareTargetViewModel(get(), get(), get()) }
    viewModel { (root: String) -> FilesViewModel(root, get(), get(), get()) }
}
