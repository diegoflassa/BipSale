package dev.diegoflassa.bipsale.core.data.backup

import dev.diegoflassa.bipsale.core.domain.settings.AppSettings
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
    val images: List<String>,
    /**
     * Everything the operator configured. Defaulted, so an archive written before settings were
     * part of the format still restores — it simply carries none.
     */
    val settings: BackupSettings? = null
)

/**
 * The configurable half of the app, so a restore brings back a working terminal, not just data.
 *
 * Every field is defaulted, so an archive written before a setting existed still restores and that
 * setting simply comes back at its default. The PIX identity fields are write-only history: the key
 * became a build constant, so they are still written for older readers but no longer restored.
 */
@Serializable
data class BackupSettings(
    val pixKey: String = "",
    val pixMerchantName: String = "",
    val pixMerchantCity: String = "",
    val pixDiscountType: String = "NONE",
    val pixDiscountValue: Double = 0.0,
    val askCustomerInfo: Boolean = true,
    val qrLabelColumns: Int = AppSettings.DEFAULT_QR_LABEL_COLUMNS,
    val qrLabelNameTextSizePt: Int = AppSettings.DEFAULT_QR_LABEL_NAME_TEXT_SIZE_PT,
    val qrLabelPriceTextSizePt: Int = AppSettings.DEFAULT_QR_LABEL_PRICE_TEXT_SIZE_PT
)

@Serializable
data class BackupProduct(
    val productCode: String,
    val productName: String,
    val price: Double,
    val qrCodeData: String? = null,
    val imageFileName: String? = null,
    val lastUpdated: Long,
    /** Defaulted so an archive written before stock control existed still restores. */
    val quantity: Int = 0
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
