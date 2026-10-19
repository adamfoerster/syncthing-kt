package com.adamfoerster.syncthing.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Pending conflict counts per folder ID. Lives in core because Home and Folders show it,
 * while the conflicts feature owns detection and resolution.
 */
interface ConflictSummary {
    val countsByFolder: Flow<Map<String, Int>>
}
