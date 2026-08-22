package dev.diegoflassa.bipsale.core.domain.usecase

import dev.diegoflassa.bipsale.core.domain.backup.BackupRepository
import dev.diegoflassa.bipsale.core.domain.backup.StagedBackup
import javax.inject.Inject

class ShareBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(): Result<StagedBackup> =
        runCatching { backupRepository.stageBackupForSharing() }
}
