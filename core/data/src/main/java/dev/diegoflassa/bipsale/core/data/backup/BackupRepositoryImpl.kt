package dev.diegoflassa.bipsale.core.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.diegoflassa.bipsale.core.data.dao.ProductDao
import dev.diegoflassa.bipsale.core.data.dao.SaleDao
import dev.diegoflassa.bipsale.core.data.database.BipSaleDatabase
import dev.diegoflassa.bipsale.core.data.model.ProductEntity
import dev.diegoflassa.bipsale.core.data.model.SaleEntity
import dev.diegoflassa.bipsale.core.data.model.SaleItemEntity
import dev.diegoflassa.bipsale.core.domain.backup.BackupMetadata
import dev.diegoflassa.bipsale.core.domain.backup.BackupRepository
import dev.diegoflassa.bipsale.core.domain.backup.BackupSummary
import dev.diegoflassa.bipsale.core.domain.backup.StagedBackup
import dev.diegoflassa.bipsale.core.domain.image.ProductImageStore
import dev.diegoflassa.bipsale.core.domain.pix.PixDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.diegoflassa.bipsale.core.data.settings.readDiscount
import dev.diegoflassa.bipsale.core.data.settings.storedType
import dev.diegoflassa.bipsale.core.data.settings.storedValue
import dev.diegoflassa.bipsale.core.domain.settings.AppSettings
import dev.diegoflassa.bipsale.core.domain.settings.SettingsRepository
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: BipSaleDatabase,
    private val productDao: ProductDao,
    private val settingsRepository: SettingsRepository,
    private val saleDao: SaleDao,
    private val productImageStore: ProductImageStore
) : BackupRepository {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    override suspend fun createBackup(destinationUri: String): BackupSummary =
        withContext(Dispatchers.IO) {
            val output = context.contentResolver.openOutputStream(Uri.parse(destinationUri))
                ?: error("Could not open $destinationUri for writing")
            output.use { writeArchive(it) }
        }

    override suspend fun stageBackupForSharing(): StagedBackup = withContext(Dispatchers.IO) {
        val shareDir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
        // One file per share, replaced each time, so the cache cannot grow without bound.
        shareDir.listFiles()?.forEach { it.delete() }

        val fileName = suggestedFileName()
        val file = File(shareDir, fileName)
        val summary = file.outputStream().use { writeArchive(it) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        Timber.d("[BipSale][Backup] Staged %s for sharing (%d bytes)", fileName, file.length())
        StagedBackup(uri = uri.toString(), fileName = fileName, summary = summary)
    }

    override suspend fun inspectBackup(sourceUri: String): BackupMetadata =
        withContext(Dispatchers.IO) {
            val document = readDocument(sourceUri)
            BackupMetadata(
                products = document.products.size,
                sales = document.sales.size,
                saleItems = document.saleItems.size,
                images = document.images.size,
                createdAt = document.createdAt,
                appVersionName = document.appVersionName,
                formatVersion = document.formatVersion,
                schemaVersion = document.schemaVersion
            )
        }

    /** Reads only the manifest and stops; the images are not worth holding just to count them. */
    private fun readDocument(sourceUri: String): BackupDocument {
        val input = context.contentResolver.openInputStream(Uri.parse(sourceUri))
            ?: error("Could not open $sourceUri for reading")

        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == DOCUMENT_ENTRY) {
                    val document: BackupDocument =
                        json.decodeFromString(zip.readBytes().decodeToString())
                    requireSupported(document)
                    return document
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        error("Archive has no $DOCUMENT_ENTRY - this is not a BipSale backup")
    }

    private fun requireSupported(document: BackupDocument) {
        require(document.formatVersion <= FORMAT_VERSION) {
            "Backup format ${document.formatVersion} is newer than this app understands " +
                "($FORMAT_VERSION). Update the app and try again."
        }
    }

    override suspend fun restoreBackup(sourceUri: String): BackupSummary =
        withContext(Dispatchers.IO) {
            val input = context.contentResolver.openInputStream(Uri.parse(sourceUri))
                ?: error("Could not open $sourceUri for reading")

            var document: BackupDocument? = null
            val images = mutableMapOf<String, ByteArray>()

            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == DOCUMENT_ENTRY ->
                            document = json.decodeFromString(zip.readBytes().decodeToString())

                        entry.name.startsWith("$IMAGES_DIR/") && !entry.isDirectory ->
                            images[File(entry.name).name] = zip.readBytes()
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val restored = document
                ?: error("Archive has no $DOCUMENT_ENTRY - this is not a BipSale backup")
            requireSupported(restored)

            // One transaction: a half-applied restore would leave sales referencing products that
            // were never written.
            database.withTransaction {
                saleDao.deleteAllSaleItems()
                saleDao.deleteAllSales()
                productDao.deleteAllProducts()

                productDao.insertProducts(restored.products.map { it.toEntity() })
                saleDao.insertSales(restored.sales.map { it.toEntity() })
                saleDao.insertSaleItems(restored.saleItems.map { it.toEntity() })
            }

            productImageStore.deleteAll()
            images.forEach { (name, bytes) -> productImageStore.writeBytes(name, bytes) }

            // Restored outside the database transaction: settings live in DataStore, and a
            // rollback there is not something the transaction could have covered anyway.
            restored.settings?.let { settingsRepository.save(it.toDomain()) }
            Timber.d(
                "[BipSale][Backup] Settings restored=%b", restored.settings != null
            )

            Timber.i(
                "[BipSale][Backup] Restored products=%d sales=%d items=%d images=%d from %s",
                restored.products.size, restored.sales.size, restored.saleItems.size,
                images.size, restored.createdAt
            )

            BackupSummary(
                products = restored.products.size,
                sales = restored.sales.size,
                saleItems = restored.saleItems.size,
                images = images.size,
                createdAt = restored.createdAt
            )
        }

    override fun suggestedFileName(): String {
        val stamp = SimpleDateFormat(FILE_STAMP_PATTERN, Locale.US).format(Date())
        return "bipsale-backup-$stamp.zip"
    }

    private suspend fun writeArchive(output: OutputStream): BackupSummary {
        val products = productDao.getAllProductsOnce()
        val sales = saleDao.getAllSalesOnce()
        val saleItems = saleDao.getAllSaleItemsOnce()
        val imageNames = productImageStore.listFileNames()

        val document = BackupDocument(
            formatVersion = FORMAT_VERSION,
            schemaVersion = SCHEMA_VERSION,
            appVersionName = appVersionName(),
            createdAt = System.currentTimeMillis(),
            products = products.map { it.toBackup() },
            sales = sales.map { it.toBackup() },
            saleItems = saleItems.map { it.toBackup() },
            images = imageNames,
            settings = settingsRepository.current().toBackup()
        )

        var writtenImages = 0
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(DOCUMENT_ENTRY))
            zip.write(json.encodeToString(document).encodeToByteArray())
            zip.closeEntry()

            for (name in imageNames) {
                val bytes = productImageStore.readBytes(name) ?: continue
                zip.putNextEntry(ZipEntry("$IMAGES_DIR/$name"))
                zip.write(bytes)
                zip.closeEntry()
                writtenImages++
            }
        }

        Timber.i(
            "[BipSale][Backup] Wrote products=%d sales=%d items=%d images=%d",
            products.size, sales.size, saleItems.size, writtenImages
        )

        return BackupSummary(
            products = products.size,
            sales = sales.size,
            saleItems = saleItems.size,
            images = writtenImages,
            createdAt = document.createdAt
        )
    }

    private fun appVersionName(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }.getOrElse { throwable ->
        Timber.e(throwable, "[BipSale][Backup] Could not read the app version name")
        ""
    }

    private companion object {
        const val FORMAT_VERSION = 1
        const val SCHEMA_VERSION = 2
        const val DOCUMENT_ENTRY = "backup.json"
        const val IMAGES_DIR = "images"
        const val SHARE_DIR = "backup_share"
        const val FILE_STAMP_PATTERN = "yyyyMMdd-HHmmss"
    }
}

private fun AppSettings.toBackup() = BackupSettings(
    pixKey = PixDefaults.KEY,
    pixMerchantName = PixDefaults.MERCHANT_NAME,
    pixMerchantCity = PixDefaults.MERCHANT_CITY,
    pixDiscountType = pixDiscount.storedType(),
    pixDiscountValue = pixDiscount.storedValue(),
    askCustomerInfo = askCustomerInfo
)

private fun BackupSettings.toDomain() = AppSettings(
    pixDiscount = readDiscount(pixDiscountType, pixDiscountValue),
    askCustomerInfo = askCustomerInfo
)

private fun ProductEntity.toBackup() = BackupProduct(
    productCode = productCode,
    productName = productName,
    price = price,
    qrCodeData = qrCodeData,
    imageFileName = imageFileName,
    lastUpdated = lastUpdated,
    quantity = quantity
)

private fun BackupProduct.toEntity() = ProductEntity(
    productCode = productCode,
    productName = productName,
    price = price,
    qrCodeData = qrCodeData,
    imageFileName = imageFileName,
    lastUpdated = lastUpdated,
    quantity = quantity
)

private fun SaleEntity.toBackup() = BackupSale(
    id = id,
    customerName = customerName,
    customerCpf = customerCpf,
    totalAmount = totalAmount,
    discountPercentage = discountPercentage,
    finalAmount = finalAmount,
    paymentMethod = paymentMethod,
    date = date,
    isSynced = isSynced
)

private fun BackupSale.toEntity() = SaleEntity(
    id = id,
    customerName = customerName,
    customerCpf = customerCpf,
    totalAmount = totalAmount,
    discountPercentage = discountPercentage,
    finalAmount = finalAmount,
    paymentMethod = paymentMethod,
    date = date,
    isSynced = isSynced
)

private fun SaleItemEntity.toBackup() = BackupSaleItem(
    id = id,
    saleId = saleId,
    productCode = productCode,
    productName = productName,
    unitPrice = unitPrice,
    quantity = quantity,
    discountType = discountType,
    discountValue = discountValue
)

private fun BackupSaleItem.toEntity() = SaleItemEntity(
    id = id,
    saleId = saleId,
    productCode = productCode,
    productName = productName,
    unitPrice = unitPrice,
    quantity = quantity,
    discountType = discountType,
    discountValue = discountValue
)
