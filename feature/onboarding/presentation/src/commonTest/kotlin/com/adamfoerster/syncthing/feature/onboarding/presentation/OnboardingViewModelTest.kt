package com.adamfoerster.syncthing.feature.onboarding.presentation

import app.cash.turbine.test
import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.adamfoerster.syncthing.core.domain.preferences.AppPreferences
import com.adamfoerster.syncthing.core.domain.preferences.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val preferences = FakePreferences()
    private lateinit var viewModel: OnboardingViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = OnboardingViewModel(preferences, SavedStateHandle())
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun nextAndBackStayWithinThePages() {
        viewModel.onAction(OnboardingAction.OnBackClick)
        assertThat(viewModel.state.value.page).isEqualTo(0)

        repeat(OnboardingViewModel.LAST_PAGE + 2) { viewModel.onAction(OnboardingAction.OnNextClick) }
        assertThat(viewModel.state.value.page).isEqualTo(OnboardingViewModel.LAST_PAGE)
        assertThat(viewModel.state.value.isLastPage).isTrue()

        viewModel.onAction(OnboardingAction.OnBackClick)
        assertThat(viewModel.state.value.isLastPage).isFalse()
    }

    @Test
    fun pageSurvivesProcessDeath() {
        viewModel.onAction(OnboardingAction.OnNextClick)
        viewModel.onAction(OnboardingAction.OnNextClick)
        val saved = SavedStateHandle(mapOf("page" to viewModel.state.value.page))

        assertThat(OnboardingViewModel(preferences, saved).state.value.page).isEqualTo(2)
    }

    @Test
    fun choosingMarksItDoneAndReportsTheDestination() = runTest {
        viewModel.eventFlow.test {
            viewModel.onAction(OnboardingAction.OnChoose(OnboardingDestination.ShareFolder))

            assertThat(awaitItem()).isEqualTo(OnboardingEvent.Finished(OnboardingDestination.ShareFolder))
            assertThat(preferences.onboardingCompleted.value).isTrue()
        }
    }

    @Test
    fun skippingMarksItDoneAndGoesHome() = runTest {
        viewModel.eventFlow.test {
            viewModel.onAction(OnboardingAction.OnSkipClick)

            assertThat(awaitItem()).isEqualTo(OnboardingEvent.Finished(OnboardingDestination.Home))
            assertThat(preferences.onboardingCompleted.value).isTrue()
        }
    }

    private class FakePreferences : AppPreferences {
        override val autoAcceptKnownFolders = MutableStateFlow(AppPreferences.DEFAULT_AUTO_ACCEPT_KNOWN_FOLDERS)
        override val languageTag = MutableStateFlow<String?>(null)
        override val themeMode = MutableStateFlow(ThemeMode.System)
        override val onboardingCompleted = MutableStateFlow(false)
        override fun setAutoAcceptKnownFolders(enabled: Boolean) { autoAcceptKnownFolders.value = enabled }
        override fun setLanguageTag(tag: String?) { languageTag.value = tag }
        override fun setThemeMode(mode: ThemeMode) { themeMode.value = mode }
        override fun setOnboardingCompleted(completed: Boolean) { onboardingCompleted.value = completed }
    }
}
