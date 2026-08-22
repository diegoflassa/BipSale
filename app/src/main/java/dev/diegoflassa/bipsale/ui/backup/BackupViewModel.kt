package dev.diegoflassa.bipsale.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.diegoflassa.bipsale.R
import dev.diegoflassa.bipsale.core.domain.backup.BackupRepository
import dev.diegoflassa.bipsale.core.domain.usecase.CreateBackupUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.InspectBackupUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.RestoreBackupUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.ShareBackupUseCase
import dev.diegoflassa.bipsale.core.ui.util.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val createBackup: CreateBackupUseCase,
    private val inspectBackup: InspectBackupUseCase,
    private val restoreBackup: RestoreBackupUseCase,
    private val shareBackup: ShareBackupUseCase,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupContract.State())
    val uiState: StateFlow<BackupContract.State> = _uiState.asStateFlow()

    private val _effect = Channel<BackupContract.Effect>()
    val effect: Flow<BackupContract.Effect> = _effect.receiveAsFlow()

    fun suggestedFileName(): String = backupRepository.suggestedFileName()

    fun onIntent(intent: BackupContract.Intent) {
        when (intent) {
            is BackupContract.Intent.CreateBackup -> create(intent.destinationUri)
            is BackupContract.Intent.ShareBackup -> share()
            is BackupContract.Intent.RestoreRequested -> inspect(intent.sourceUri)
            is BackupContract.Intent.RestoreCancelled -> {
                Timber.d("[BipSale][Backup] Restore cancelled at the confirmation")
                _uiState.update { it.copy(pendingRestore = null) }
            }
            is BackupContract.Intent.RestoreConfirmed -> restore()
        }
    }

    private fun create(destinationUri: String) {
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            Timber.d("[BipSale][Backup] Writing archive")
            createBackup(destinationUri)
                .onSuccess { summary ->
                    _uiState.update { it.copy(isBusy = false, lastSummary = summary) }
                    emit(
                        BackupContract.Effect.ShowSnackbar(
                            UiText.StringResource(
                                R.string.backup_created,
                                summary.products,
                                summary.sales,
                                summary.images
                            )
                        )
                    )
                }
                .onFailure { failure(it, R.string.backup_create_failed) }
        }
    }

    private fun share() {
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            Timber.d("[BipSale][Backup] Staging archive for sharing")
            shareBackup()
                .onSuccess { staged ->
                    _uiState.update { it.copy(isBusy = false, lastSummary = staged.summary) }
                    _effect.send(
                        BackupContract.Effect.ShareFile(staged.uri, staged.fileName)
                    )
                }
                .onFailure { failure(it, R.string.backup_share_failed) }
        }
    }

    /** Reads the manifest first so the confirmation can say what is about to be overwritten. */
    private fun inspect(sourceUri: String) {
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            Timber.d("[BipSale][Backup] Inspecting archive before restore")
            inspectBackup(sourceUri)
                .onSuccess { metadata ->
                    Timber.d(
                        "[BipSale][Backup] Archive holds products=%d sales=%d items=%d images=%d",
                        metadata.products, metadata.sales, metadata.saleItems, metadata.images
                    )
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            pendingRestore = BackupContract.PendingRestore(sourceUri, metadata)
                        )
                    }
                }
                .onFailure { failure(it, R.string.backup_invalid_archive) }
        }
    }

    private fun restore() {
        val source = _uiState.value.pendingRestore?.sourceUri
        if (source == null) {
            Timber.w("[BipSale][Backup] Restore confirmed with no file selected")
            return
        }
        _uiState.update { it.copy(isBusy = true, pendingRestore = null) }
        viewModelScope.launch {
            Timber.d("[BipSale][Backup] Restoring archive")
            restoreBackup(source)
                .onSuccess { summary ->
                    _uiState.update { it.copy(isBusy = false, lastSummary = summary) }
                    emit(
                        BackupContract.Effect.ShowSnackbar(
                            UiText.StringResource(
                                R.string.backup_restored,
                                summary.products,
                                summary.sales,
                                summary.images
                            )
                        )
                    )
                }
                .onFailure { failure(it, R.string.backup_restore_failed) }
        }
    }

    private suspend fun failure(throwable: Throwable, fallbackMessage: Int) {
        if (throwable is CancellationException) throw throwable
        Timber.e(throwable, "[BipSale][Backup] Operation failed")
        _uiState.update { it.copy(isBusy = false) }
        val message = throwable.message
        _effect.send(
            BackupContract.Effect.ShowSnackbar(
                if (message.isNullOrBlank()) {
                    UiText.StringResource(fallbackMessage)
                } else {
                    UiText.DynamicString(message)
                }
            )
        )
    }

    private suspend fun emit(effect: BackupContract.Effect) {
        _effect.send(effect)
    }
}
