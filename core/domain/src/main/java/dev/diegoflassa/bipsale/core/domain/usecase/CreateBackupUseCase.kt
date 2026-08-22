package dev.diegoflassa.bipsale.core.domain.usecase

import dev.diegoflassa.bipsale.core.domain.backup.BackupRepository
import dev.diegoflassa.bipsale.core.domain.backup.BackupSummary
import javax.inject.Inject

class CreateBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(destinationUri: String): Result<BackupSummary> =
        runCatching { backupRepository.createBackup(destinationUri) }
}
