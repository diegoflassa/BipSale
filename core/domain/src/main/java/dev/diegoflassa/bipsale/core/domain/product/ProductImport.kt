package dev.diegoflassa.bipsale.core.domain.product

/** One row of a filled import template, already validated. */
data class ImportedProduct(
    val code: String,
    val name: String,
    val price: Double,
    val quantity: Int
)

/** Why one row was left out, so the operator can fix that row instead of the whole file. */
data class ProductImportRejection(
    /** One-based, matching the row number the spreadsheet shows. */
    val row: Int,
    val code: String,
    val reason: Reason
) {
    enum class Reason { MISSING_CODE, MISSING_NAME, INVALID_PRICE, INVALID_QUANTITY }
}

/**
 * What a read of the template produced. Rejections travel alongside the accepted rows rather than
 * aborting the import: one typo in a fifty-row catalogue must not cost the other forty-nine.
 */
data class ProductImportReport(
    val accepted: List<ImportedProduct> = emptyList(),
    val rejected: List<ProductImportRejection> = emptyList()
) {
    val isEmpty: Boolean get() = accepted.isEmpty() && rejected.isEmpty()
}

/**
 * Writes the blank template and reads a filled one.
 *
 * Both cross this seam as `String` URIs so the domain stays free of Android types; the caller gets
 * them from the system document picker.
 */
interface ProductImportRepository {
    suspend fun writeTemplate(destinationUri: String)

    suspend fun read(sourceUri: String): ProductImportReport

    fun suggestedTemplateName(): String
}
