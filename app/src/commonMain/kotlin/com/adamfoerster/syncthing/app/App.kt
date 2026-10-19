package com.adamfoerster.syncthing.app

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.adamfoerster.syncthing.app.navigation.AddDeviceRoute
import com.adamfoerster.syncthing.app.navigation.AddFolderRoute
import com.adamfoerster.syncthing.app.navigation.DevicesRoute
import com.adamfoerster.syncthing.app.navigation.FoldersRoute
import com.adamfoerster.syncthing.app.navigation.HomeRoute
import com.adamfoerster.syncthing.app.navigation.SettingsRoute
import com.adamfoerster.syncthing.app.navigation.SharingRoute
import com.adamfoerster.syncthing.app.resources.Res
import com.adamfoerster.syncthing.app.resources.tab_devices
import com.adamfoerster.syncthing.app.resources.tab_folders
import com.adamfoerster.syncthing.app.resources.tab_home
import com.adamfoerster.syncthing.app.resources.tab_settings
import com.adamfoerster.syncthing.core.designsystem.components.NgTab
import com.adamfoerster.syncthing.core.designsystem.components.NgTabBar
import com.adamfoerster.syncthing.core.designsystem.icons.NgIcons
import com.adamfoerster.syncthing.core.designsystem.theme.SyncthingTheme
import com.adamfoerster.syncthing.core.domain.repository.SyncthingCore
import com.adamfoerster.syncthing.core.presentation.LocalPlatformActions
import com.adamfoerster.syncthing.core.presentation.PlatformActions
import com.adamfoerster.syncthing.feature.conflicts.presentation.ConflictsGraph
import com.adamfoerster.syncthing.feature.conflicts.presentation.conflictsGraph
import com.adamfoerster.syncthing.feature.devices.presentation.add.AddDeviceRoot
import com.adamfoerster.syncthing.feature.devices.presentation.list.DeviceListRoot
import com.adamfoerster.syncthing.feature.folders.presentation.add.AddFolderRoot
import com.adamfoerster.syncthing.feature.folders.presentation.add.FolderOffer
import com.adamfoerster.syncthing.feature.folders.presentation.list.FolderListRoot
import com.adamfoerster.syncthing.feature.home.presentation.HomeRoot
import com.adamfoerster.syncthing.feature.settings.presentation.SettingsRoot
import com.adamfoerster.syncthing.feature.sharing.presentation.SharingRoot
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private val tabRoutes = listOf(HomeRoute, FoldersRoute, DevicesRoute, SettingsRoute)

@Composable
fun App(platformActions: PlatformActions) {
    SyncthingTheme {
        CompositionLocalProvider(LocalPlatformActions provides platformActions) {
            AppNavHost()
        }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val core = koinInject<SyncthingCore>()
    val tabBar: @Composable () -> Unit = { MainTabBar(navController) }

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        // Tabs swap in place, like the mockups' bottom navigation.
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
    ) {
        composable<HomeRoute> {
            HomeRoot(
                tabBar = tabBar,
                onResolveConflicts = { navController.navigate(ConflictsGraph) },
                onShowSharing = { navController.navigate(SharingRoute) },
                onOpenDevices = { navController.navigateToTab(DevicesRoute) },
                onOpenFolders = { navController.navigateToTab(FoldersRoute) },
            )
        }
        composable<FoldersRoute> {
            FolderListRoot(
                tabBar = tabBar,
                onOpenConflicts = { navController.navigate(ConflictsGraph) },
                onOpenSharing = { navController.navigate(SharingRoute) },
                onAddFolder = { navController.navigate(AddFolderRoute()) },
                onAcceptOffer = { id, label, device -> navController.navigate(AddFolderRoute(id, label, device)) },
            )
        }
        composable<DevicesRoute> {
            DeviceListRoot(
                tabBar = tabBar,
                onAddDevice = { navController.navigate(AddDeviceRoute()) },
                onApprovePending = { id, name -> navController.navigate(AddDeviceRoute(id, name)) },
            )
        }
        composable<SettingsRoute> { SettingsRoot(tabBar = tabBar) }
        composable<SharingRoute> {
            SharingRoot(onBack = { navController.navigateUp() }, onStartCore = core::start)
        }
        composable<AddFolderRoute> { entry ->
            val route = entry.toRoute<AddFolderRoute>()
            val offer = if (route.offeredFolderId != null && route.offeredBy != null) {
                FolderOffer(route.offeredFolderId, route.offeredLabel ?: route.offeredFolderId, route.offeredBy)
            } else {
                null
            }
            AddFolderRoot(offer = offer, onDone = { navController.navigateUp() })
        }
        composable<AddDeviceRoute> { entry ->
            val route = entry.toRoute<AddDeviceRoute>()
            AddDeviceRoot(prefillId = route.deviceId, prefillName = route.name, onDone = { navController.navigateUp() })
        }
        conflictsGraph(navController)
    }
}

@Composable
private fun MainTabBar(navController: NavHostController) {
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val selected = tabRoutes.indexOfFirst { route -> destination?.hasRoute(route::class) == true }.coerceAtLeast(0)
    NgTabBar(
        tabs = listOf(
            NgTab(stringResource(Res.string.tab_home), NgIcons.Home),
            NgTab(stringResource(Res.string.tab_folders), NgIcons.Folder),
            NgTab(stringResource(Res.string.tab_devices), NgIcons.Devices),
            NgTab(stringResource(Res.string.tab_settings), NgIcons.Settings),
        ),
        selectedIndex = selected,
        onSelect = { index -> navController.navigateToTab(tabRoutes[index]) },
    )
}

private fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
