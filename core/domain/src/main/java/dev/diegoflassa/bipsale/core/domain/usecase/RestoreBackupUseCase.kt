package dev.diegoflassa.bipsale.core.domain.usecase

import dev.diegoflassa.bipsale.core.domain.backup.BackupRepository
import dev.diegoflassa.bipsale.core.domain.backup.BackupSummary
import javax.inject.Inject

class RestoreBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(sourceUri: String): Result<BackupSummary> =
        runCatching { backupRepository.restoreBackup(sourceUri) }
}
