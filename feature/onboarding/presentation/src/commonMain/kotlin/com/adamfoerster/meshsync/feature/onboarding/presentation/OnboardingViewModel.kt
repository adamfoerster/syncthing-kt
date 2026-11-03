package com.adamfoerster.meshsync.feature.onboarding.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.meshsync.core.domain.preferences.AppPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Where the introduction sends the user once it ends. */
enum class OnboardingDestination {
    /** Straight to the tabs: the user chose to decide later. */
    Home,

    /** Create a folder and offer it to other devices. */
    ShareFolder,

    /** Connect to the device that has the files, which will then offer them. */
    ReceiveFolder,
}

data class OnboardingState(val page: Int = 0, val storageGranted: Boolean = false) {
    val isLastPage: Boolean get() = page >= OnboardingViewModel.LAST_PAGE
    val isPermissionPage: Boolean get() = page == OnboardingViewModel.PERMISSION_PAGE
}

sealed interface OnboardingAction {
    data object OnNextClick : OnboardingAction
    data object OnBackClick : OnboardingAction
    data object OnSkipClick : OnboardingAction

    /** The platform's current answer to "may we read and write shared folders?", re-checked on every resume. */
    data class OnStorageAccessChecked(val granted: Boolean) : OnboardingAction
    data class OnChoose(val destination: OnboardingDestination) : OnboardingAction
}

sealed interface OnboardingEvent {
    data class Finished(val destination: OnboardingDestination) : OnboardingEvent
}

class OnboardingViewModel(
    private val preferences: AppPreferences,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    // The page survives process death, so a rotation or a kill does not send the user back to the start.
    private val _state = kotlinx.coroutines.flow.MutableStateFlow(OnboardingState(savedState.get<Int>(KEY_PAGE)?.coerceIn(0, LAST_PAGE) ?: 0))
    val state: StateFlow<OnboardingState> = _state

    private val events = Channel<OnboardingEvent>()
    val eventFlow: Flow<OnboardingEvent> = events.receiveAsFlow()

    fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.OnNextClick -> goTo(_state.value.page + 1)
            OnboardingAction.OnBackClick -> goTo(_state.value.page - 1)
            OnboardingAction.OnSkipClick -> finish(OnboardingDestination.Home)
            is OnboardingAction.OnStorageAccessChecked -> _state.value = _state.value.copy(storageGranted = action.granted)
            is OnboardingAction.OnChoose -> finish(action.destination)
        }
    }

    private fun goTo(page: Int) {
        val clamped = page.coerceIn(0, LAST_PAGE)
        savedState[KEY_PAGE] = clamped
        _state.value = _state.value.copy(page = clamped)
    }

    private fun finish(destination: OnboardingDestination) {
        preferences.setOnboardingCompleted(true)
        viewModelScope.launch { events.send(OnboardingEvent.Finished(destination)) }
    }

    companion object {
        /**
         * Three explanation pages (0–2), the all-files permission (3) and the final question (4).
         * The permission comes before the choice because both paths need it: without it the core
         * cannot start, so pairing with another device would fail right after onboarding.
         */
        const val PERMISSION_PAGE = 3
        const val LAST_PAGE = 4
        private const val KEY_PAGE = "page"
    }
}
