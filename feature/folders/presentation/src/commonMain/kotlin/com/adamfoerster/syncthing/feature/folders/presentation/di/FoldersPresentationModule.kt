package com.adamfoerster.syncthing.feature.folders.presentation.di

import com.adamfoerster.syncthing.feature.folders.presentation.add.AddFolderViewModel
import com.adamfoerster.syncthing.feature.folders.presentation.add.FolderOffer
import com.adamfoerster.syncthing.feature.folders.presentation.list.FolderListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val foldersPresentationModule = module {
    viewModelOf(::FolderListViewModel)
    viewModel { (offer: FolderOffer?, defaultRoot: String) -> AddFolderViewModel(offer, defaultRoot, get(), get()) }
}
