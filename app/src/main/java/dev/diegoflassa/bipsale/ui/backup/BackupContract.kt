package dev.diegoflassa.bipsale.ui.backup

import androidx.compose.runtime.Immutable
import dev.diegoflassa.bipsale.core.domain.backup.BackupMetadata
import dev.diegoflassa.bipsale.core.domain.backup.BackupSummary
import dev.diegoflassa.bipsale.core.ui.util.UiText

class BackupContract {

    @Immutable
    data class State(
        val isBusy: Boolean = false,
        val lastSummary: BackupSummary? = null,
        /** Set while the operator is being asked to confirm a restore. */
        val pendingRestore: PendingRestore? = null
    )

    /** The chosen archive plus what it says it holds, read before anything is overwritten. */
    @Immutable
    data class PendingRestore(val sourceUri: String, val metadata: BackupMetadata)

    sealed interface Intent {
        data class CreateBackup(val destinationUri: String) : Intent
        data object ShareBackup : Intent

        /** Opens the confirmation rather than restoring — restore replaces everything. */
        data class RestoreRequested(val sourceUri: String) : Intent
        data object RestoreConfirmed : Intent
        data object RestoreCancelled : Intent
    }

    sealed interface Effect {
        data class ShowSnackbar(val message: UiText) : Effect
        data class ShareFile(val uri: String, val fileName: String) : Effect
    }
}
