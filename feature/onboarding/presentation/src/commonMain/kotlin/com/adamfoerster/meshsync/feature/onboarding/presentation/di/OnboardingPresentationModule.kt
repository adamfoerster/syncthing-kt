package com.adamfoerster.meshsync.feature.onboarding.presentation.di

import com.adamfoerster.meshsync.feature.onboarding.presentation.OnboardingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingPresentationModule = module {
    viewModelOf(::OnboardingViewModel)
}
