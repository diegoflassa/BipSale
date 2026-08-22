package dev.diegoflassa.bipsale.core.domain.backup

/**
 * Reads and writes the whole dataset as a single portable archive.
 *
 * URIs cross this boundary as `String` so the domain stays free of Android types; the caller gets
 * them from the system document picker, which is also what puts Google Drive in reach without the
 * app holding any Drive credentials.
 */
interface BackupRepository {
    suspend fun createBackup(destinationUri: String): BackupSummary

    /** Reads the archive's manifest without applying it, so a restore can be confirmed informed. */
    suspend fun inspectBackup(sourceUri: String): BackupMetadata

    /** Replaces everything currently stored. Destructive by design — confirm before calling. */
    suspend fun restoreBackup(sourceUri: String): BackupSummary

    /** Writes an archive to app-private cache and returns a URI other apps may read. */
    suspend fun stageBackupForSharing(): StagedBackup

    fun suggestedFileName(): String
}

data class StagedBackup(val uri: String, val fileName: String, val summary: BackupSummary)
