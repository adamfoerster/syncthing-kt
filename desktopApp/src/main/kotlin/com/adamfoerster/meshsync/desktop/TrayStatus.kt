package com.adamfoerster.meshsync.desktop

import com.adamfoerster.meshsync.core.domain.model.CoreState

/**
 * What the tray icon says. The icon shape differs per status and the tooltip spells it out, so the
 * state never depends on colour alone.
 */
enum class TrayStatus {
    Running,
    Starting,
    Paused,
    Failed,
    Missing,
    ;

    /** Whether the tray menu should offer "Pause" (true) or "Resume" (false). */
    val canPause: Boolean get() = this == Running || this == Starting

    companion object {
        fun of(state: CoreState): TrayStatus = when (state) {
            CoreState.Running -> Running
            CoreState.Starting -> Starting
            CoreState.Stopped -> Paused
            CoreState.BinaryMissing -> Missing
            // Not reachable on the desktop (no storage permission), but the model has it.
            CoreState.StoragePermissionMissing -> Failed
            is CoreState.Failed -> Failed
        }
    }
}
