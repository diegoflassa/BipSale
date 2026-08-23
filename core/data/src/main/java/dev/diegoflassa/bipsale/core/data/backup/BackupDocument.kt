package dev.diegoflassa.bipsale.core.data.backup

import kotlinx.serialization.Serializable

/**
 * The JSON payload inside a backup archive.
 *
 * Deliberately not a copy of the SQLite file: a raw database restored onto a build whose schema has
 * moved on makes Room abort on the identity hash rather than open it, which would turn every older
 * backup into a landmine. A described document can be read, checked, and migrated.
 */
@Serializable
data class BackupDocument(
    val formatVersion: Int,
    val schemaVersion: Int,
    val appVersionName: String,
    val createdAt: Long,
    val products: List<BackupProduct>,
    val sales: List<BackupSale>,
    val saleItems: List<BackupSaleItem>,
    /** Image file names carried under `images/` in the archive, in the order they were written. */
    val images: List<String>
)

@Serializable
data class BackupProduct(
    val productCode: String,
    val productName: String,
    val price: Double,
    val qrCodeData: String? = null,
    val imageFileName: String? = null,
    val lastUpdated: Long
)

@Serializable
data class BackupSale(
    val id: String,
    val customerName: String? = null,
    val customerCpf: String? = null,
    val totalAmount: Double,
    val discountPercentage: Double,
    val finalAmount: Double,
    val paymentMethod: String,
    val date: Long,
    val isSynced: Boolean = false
)

@Serializable
data class BackupSaleItem(
    val id: String,
    val saleId: String,
    val productCode: String,
    val productName: String,
    val unitPrice: Double,
    val quantity: Int,
    /** Defaulted so an archive written before per-line discounts existed still restores. */
    val discountType: String = "NONE",
    val discountValue: Double = 0.0
)
