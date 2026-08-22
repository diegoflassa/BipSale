package dev.diegoflassa.bipsale.core.domain.backup

/** What a backup archive turned out to hold, for confirming a write or a restore. */
data class BackupSummary(
    val products: Int,
    val sales: Int,
    val saleItems: Int,
    val images: Int,
    val createdAt: Long
)
