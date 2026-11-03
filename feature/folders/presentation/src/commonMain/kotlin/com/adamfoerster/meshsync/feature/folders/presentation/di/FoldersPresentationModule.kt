package com.adamfoerster.meshsync.feature.folders.presentation.di

import com.adamfoerster.meshsync.feature.folders.presentation.add.AddFolderViewModel
import com.adamfoerster.meshsync.feature.folders.presentation.add.FolderOffer
import com.adamfoerster.meshsync.feature.folders.presentation.ignore.IgnoreViewModel
import com.adamfoerster.meshsync.feature.folders.presentation.list.FolderListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val foldersPresentationModule = module {
    viewModelOf(::FolderListViewModel)
    viewModel { (offer: FolderOffer?, initialPath: String?, defaultRoot: String) -> AddFolderViewModel(offer, initialPath, defaultRoot, get(), get(), get()) }
    viewModel { (folderId: String) -> IgnoreViewModel(folderId, get(), get()) }
}
