package com.adamfoerster.syncthing.feature.conflicts.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.adamfoerster.syncthing.feature.conflicts.domain.model.ConflictKind
import com.adamfoerster.syncthing.feature.conflicts.presentation.binary.BinaryConflictRoot
import com.adamfoerster.syncthing.feature.conflicts.presentation.list.ConflictListRoot
import com.adamfoerster.syncthing.feature.conflicts.presentation.text.TextConflictRoot
import kotlinx.serialization.Serializable

@Serializable
data object ConflictsGraph

@Serializable
data object ConflictListRoute

@Serializable
data class TextConflictRoute(val conflictId: String)

@Serializable
data class BinaryConflictRoute(val conflictId: String)

fun NavGraphBuilder.conflictsGraph(navController: NavController) {
    navigation<ConflictsGraph>(startDestination = ConflictListRoute) {
        composable<ConflictListRoute> {
            ConflictListRoot(
                onBack = { navController.navigateUp() },
                onOpenConflict = { id, kind ->
                    navController.navigate(if (kind == ConflictKind.TEXT) TextConflictRoute(id) else BinaryConflictRoute(id))
                },
            )
        }
        composable<TextConflictRoute> { entry ->
            TextConflictRoot(entry.toRoute<TextConflictRoute>().conflictId, onBack = { navController.navigateUp() })
        }
        composable<BinaryConflictRoute> { entry ->
            BinaryConflictRoot(entry.toRoute<BinaryConflictRoute>().conflictId, onBack = { navController.navigateUp() })
        }
    }
}
