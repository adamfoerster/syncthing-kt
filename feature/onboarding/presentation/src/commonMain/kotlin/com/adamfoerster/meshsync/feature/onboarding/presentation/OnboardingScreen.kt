package com.adamfoerster.meshsync.feature.onboarding.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.meshsync.core.presentation.LocalPlatformActions
import com.adamfoerster.meshsync.core.designsystem.components.NgActionBar
import com.adamfoerster.meshsync.core.designsystem.components.NgButton
import com.adamfoerster.meshsync.core.designsystem.components.NgButtonStyle
import com.adamfoerster.meshsync.core.designsystem.components.NgPanel
import com.adamfoerster.meshsync.core.designsystem.components.NgScreen
import com.adamfoerster.meshsync.core.designsystem.components.NgTopBar
import com.adamfoerster.meshsync.core.designsystem.icons.NgIcons
import com.adamfoerster.meshsync.core.designsystem.theme.NgTheme
import com.adamfoerster.meshsync.core.designsystem.theme.SyncthingTheme
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.Res
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_back
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_choice_body
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_choice_later
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_choice_receive
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_choice_receive_hint
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_choice_share
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_choice_share_hint
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_choice_title
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_next
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_page_count
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_p1_body
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_p1_title
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_p2_body
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_p2_title
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_p3_body
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_p3_title
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_skip
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_storage_body
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_storage_grant
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_storage_granted
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_storage_hint
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_storage_title
import com.adamfoerster.meshsync.feature.onboarding.presentation.resources.onboarding_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingRoot(
    onFinished: (OnboardingDestination) -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is OnboardingEvent.Finished -> onFinished(event.destination)
            }
        }
    }
    val platform = LocalPlatformActions.current
    // The grant happens in system settings, so the answer is re-read each time the user comes back.
    LifecycleResumeEffect(platform) {
        viewModel.onAction(OnboardingAction.OnStorageAccessChecked(platform.hasStorageAccess()))
        onPauseOrDispose { }
    }
    OnboardingScreen(state = state, onAction = viewModel::onAction, onGrantStorage = platform::requestStorageAccess)
}

private class Page(val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: StringResource, val body: StringResource)

private val pages = listOf(
    Page(NgIcons.Transfer, Res.string.onboarding_p1_title, Res.string.onboarding_p1_body),
    Page(NgIcons.Devices, Res.string.onboarding_p2_title, Res.string.onboarding_p2_body),
    Page(NgIcons.Folder, Res.string.onboarding_p3_title, Res.string.onboarding_p3_body),
)

@Composable
fun OnboardingScreen(state: OnboardingState, onAction: (OnboardingAction) -> Unit, onGrantStorage: () -> Unit = {}) {
    val c = NgTheme.colors
    NgScreen(
        topBar = {
            NgTopBar(
                title = stringResource(Res.string.onboarding_title),
                onBack = if (state.page > 0) ({ onAction(OnboardingAction.OnBackClick) }) else null,
                actions = {
                    if (!state.isLastPage) {
                        NgButton(
                            text = stringResource(Res.string.onboarding_skip),
                            onClick = { onAction(OnboardingAction.OnSkipClick) },
                            style = NgButtonStyle.Tertiary,
                        )
                    }
                },
            )
        },
        bottomBar = if (state.isLastPage) null else ({
            NgActionBar {
                NgButton(
                    text = stringResource(Res.string.onboarding_next),
                    onClick = { onAction(OnboardingAction.OnNextClick) },
                    style = NgButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }),
    ) {
        Text(
            stringResource(Res.string.onboarding_page_count, state.page + 1, OnboardingViewModel.LAST_PAGE + 1),
            style = NgTheme.type.monoSmall,
            color = c.text2,
        )
        val page = pages.getOrNull(state.page)
        if (page != null) {
            Icon(page.icon, null, tint = c.cyan, modifier = Modifier.size(48.dp))
            Text(stringResource(page.title), style = NgTheme.type.h2, color = c.text)
            Text(stringResource(page.body), style = NgTheme.type.body, color = c.text2)
        } else if (state.isPermissionPage) {
            Icon(NgIcons.Folder, null, tint = c.cyan, modifier = Modifier.size(48.dp))
            Text(stringResource(Res.string.onboarding_storage_title), style = NgTheme.type.h2, color = c.text)
            Text(stringResource(Res.string.onboarding_storage_body), style = NgTheme.type.body, color = c.text2)
            if (state.storageGranted) {
                // Text plus glyph, never colour alone.
                Text("✓ " + stringResource(Res.string.onboarding_storage_granted), style = NgTheme.type.body, color = c.text)
            } else {
                NgButton(
                    text = stringResource(Res.string.onboarding_storage_grant),
                    onClick = onGrantStorage,
                    style = NgButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(Res.string.onboarding_storage_hint), style = NgTheme.type.sub, color = c.text2)
            }
        } else {
            Text(stringResource(Res.string.onboarding_choice_title), style = NgTheme.type.h2, color = c.text)
            Text(stringResource(Res.string.onboarding_choice_body), style = NgTheme.type.body, color = c.text2)
            ChoicePanel(
                icon = NgIcons.Folder,
                label = stringResource(Res.string.onboarding_choice_share),
                hint = stringResource(Res.string.onboarding_choice_share_hint),
                onClick = { onAction(OnboardingAction.OnChoose(OnboardingDestination.ShareFolder)) },
            )
            ChoicePanel(
                icon = NgIcons.Transfer,
                label = stringResource(Res.string.onboarding_choice_receive),
                hint = stringResource(Res.string.onboarding_choice_receive_hint),
                onClick = { onAction(OnboardingAction.OnChoose(OnboardingDestination.ReceiveFolder)) },
            )
            NgButton(
                text = stringResource(Res.string.onboarding_choice_later),
                onClick = { onAction(OnboardingAction.OnChoose(OnboardingDestination.Home)) },
                style = NgButtonStyle.Tertiary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ChoicePanel(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, hint: String, onClick: () -> Unit) {
    val c = NgTheme.colors
    NgPanel(Modifier.fillMaxWidth(), onClick = onClick, onClickLabel = label) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = c.cyan, modifier = Modifier.size(28.dp))
            Text(label, style = NgTheme.type.h3, color = c.text)
        }
        Text(hint, style = NgTheme.type.sub, color = c.text2)
    }
}

@Preview
@Composable
private fun OnboardingScreenPreview() {
    SyncthingTheme { OnboardingScreen(OnboardingState(page = 0), onAction = {}) }
}

@Preview
@Composable
private fun OnboardingChoicePreview() {
    SyncthingTheme { OnboardingScreen(OnboardingState(page = OnboardingViewModel.LAST_PAGE), onAction = {}) }
}
