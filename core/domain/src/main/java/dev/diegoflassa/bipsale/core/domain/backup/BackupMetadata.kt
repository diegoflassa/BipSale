package dev.diegoflassa.bipsale.core.domain.backup

/**
 * What an archive says about itself, read without applying any of it.
 *
 * Restore replaces every product, sale and image on the device, so the operator gets to see what
 * they are about to swap in — and what it will cost them — before they confirm.
 */
data class BackupMetadata(
    val products: Int,
    val sales: Int,
    val saleItems: Int,
    val images: Int,
    val createdAt: Long,
    val appVersionName: String,
    val formatVersion: Int,
    val schemaVersion: Int
)
