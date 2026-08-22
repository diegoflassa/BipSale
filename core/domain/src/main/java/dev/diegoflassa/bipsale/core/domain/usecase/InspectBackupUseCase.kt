package dev.diegoflassa.bipsale.core.domain.usecase

import dev.diegoflassa.bipsale.core.domain.backup.BackupMetadata
import dev.diegoflassa.bipsale.core.domain.backup.BackupRepository
import javax.inject.Inject

class InspectBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(sourceUri: String): Result<BackupMetadata> =
        runCatching { backupRepository.inspectBackup(sourceUri) }
}
