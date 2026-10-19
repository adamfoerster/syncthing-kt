package com.adamfoerster.syncthing.feature.conflicts.presentation.di

import com.adamfoerster.syncthing.feature.conflicts.presentation.binary.BinaryConflictViewModel
import com.adamfoerster.syncthing.feature.conflicts.presentation.list.ConflictListViewModel
import com.adamfoerster.syncthing.feature.conflicts.presentation.text.TextConflictViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val conflictsPresentationModule = module {
    viewModelOf(::ConflictListViewModel)
    viewModel { (conflictId: String) -> TextConflictViewModel(conflictId, get(), get()) }
    viewModel { (conflictId: String) -> BinaryConflictViewModel(conflictId, get(), get()) }
}
