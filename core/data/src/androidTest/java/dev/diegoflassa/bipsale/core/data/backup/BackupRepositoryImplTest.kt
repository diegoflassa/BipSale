package dev.diegoflassa.bipsale.core.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import dev.diegoflassa.bipsale.core.domain.model.ItemDiscount
import dev.diegoflassa.bipsale.core.domain.settings.AppSettings
import dev.diegoflassa.bipsale.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.data.database.BipSaleDatabase
import dev.diegoflassa.bipsale.core.data.model.ProductEntity
import dev.diegoflassa.bipsale.core.data.model.SaleEntity
import dev.diegoflassa.bipsale.core.data.model.SaleItemEntity
import dev.diegoflassa.bipsale.core.domain.image.ProductImageStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var database: BipSaleDatabase
    private lateinit var repository: BackupRepositoryImpl
    private lateinit var images: FakeImageStore
    private lateinit var settings: FakeSettingsRepository

    /** In-memory so a test run never touches the installed app's sales. */
    private class FakeImageStore : ProductImageStore {
        val files = linkedMapOf<String, ByteArray>()
        override suspend fun save(sourceUri: String, productCode: String): String = "unused"
        override suspend fun delete(fileName: String) {
            files.remove(fileName)
        }

        override fun resolvePath(fileName: String): String = fileName
        override suspend fun listFileNames(): List<String> = files.keys.toList()
        override suspend fun readBytes(fileName: String): ByteArray? = files[fileName]
        override suspend fun writeBytes(fileName: String, bytes: ByteArray) {
            files[fileName] = bytes
        }

        override suspend fun deleteAll() = files.clear()
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, BipSaleDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        images = FakeImageStore()
        settings = FakeSettingsRepository()
        repository = BackupRepositoryImpl(
            context = context,
            database = database,
            productDao = database.productDao(),
            saleDao = database.saleDao(),
            settingsRepository = settings,
            productImageStore = images
        )
    }

    /** Settings live in DataStore, so the archive's settings block is exercised through a fake. */
    private class FakeSettingsRepository : SettingsRepository {
        var saved: AppSettings? = null
        var stored: AppSettings = AppSettings.EMPTY

        override val settings: Flow<AppSettings> get() = flowOf(stored)
        override suspend fun current(): AppSettings = stored
        override suspend fun save(settings: AppSettings) {
            saved = settings
            stored = settings
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun seed() = runBlocking {
        database.productDao().insertProducts(
            listOf(
                ProductEntity("CT-A-RoS", "Coturno cano alto rosa", 130.0, "qr-1", "ct.jpg", 1L),
                ProductEntity("CAFE-200", "Café Premium 200ml", 12.5, "qr-2", null, 2L)
            )
        )
        database.saleDao().insertSales(
            listOf(
                SaleEntity("sale-1", "Ana", "12345678900", 142.5, 0.0, 142.5, "PIX", 10L, false)
            )
        )
        database.saleDao().insertSaleItems(
            listOf(
                SaleItemEntity("item-1", "sale-1", "CT-A-RoS", "Coturno", 130.0, 1),
                SaleItemEntity("item-2", "sale-1", "CAFE-200", "Café", 12.5, 1)
            )
        )
        images.files["ct.jpg"] = byteArrayOf(1, 2, 3, 4, 5)
    }

    private fun archiveFile() = File(context.cacheDir, "backup_test_${System.nanoTime()}.zip")

    @Test
    fun roundTripsProductsSalesItemsAndImages(): Unit = runBlocking {
        seed()
        val file = archiveFile()

        val written = repository.createBackup(Uri.fromFile(file).toString())
        assertThat(written.products).isEqualTo(2)
        assertThat(written.sales).isEqualTo(1)
        assertThat(written.saleItems).isEqualTo(2)
        assertThat(written.images).isEqualTo(1)

        // Wipe everything, exactly as a restore onto a different device would find it.
        database.saleDao().deleteAllSaleItems()
        database.saleDao().deleteAllSales()
        database.productDao().deleteAllProducts()
        images.deleteAll()

        val restored = repository.restoreBackup(Uri.fromFile(file).toString())

        assertThat(restored.products).isEqualTo(2)
        assertThat(restored.sales).isEqualTo(1)
        assertThat(restored.saleItems).isEqualTo(2)
        assertThat(database.productDao().getAllProductsOnce()).hasSize(2)
        assertThat(database.saleDao().getAllSalesOnce()).hasSize(1)
        assertThat(database.saleDao().getAllSaleItemsOnce()).hasSize(2)
        assertThat(images.files["ct.jpg"]).isEqualTo(byteArrayOf(1, 2, 3, 4, 5))
    }

    @Test
    fun roundTripsEveryConfiguredSetting(): Unit = runBlocking {
        // A restore that brings back products but silently resets the label grid and type sizes
        // sends the operator back to Settings to redo work the archive was supposed to hold.
        seed()
        settings.stored = AppSettings(
            pixDiscount = ItemDiscount.Percentage(7.5),
            askCustomerInfo = false,
            qrLabelColumns = 2,
            qrLabelNameTextSizePt = 15,
            qrLabelPriceTextSizePt = 19
        )
        val file = archiveFile()
        repository.createBackup(Uri.fromFile(file).toString())

        settings.stored = AppSettings.EMPTY
        repository.restoreBackup(Uri.fromFile(file).toString())

        val restored = settings.saved
        assertThat(restored).isNotNull()
        assertThat(restored!!.pixDiscount).isEqualTo(ItemDiscount.Percentage(7.5))
        assertThat(restored.askCustomerInfo).isFalse()
        assertThat(restored.qrLabelColumns).isEqualTo(2)
        assertThat(restored.qrLabelNameTextSizePt).isEqualTo(15)
        assertThat(restored.qrLabelPriceTextSizePt).isEqualTo(19)
    }

    @Test
    fun clampsSettingsThatTheArchiveCarriesOutOfRange(): Unit = runBlocking {
        // The archive is a file the operator can hand around and edit. An absurd type size must
        // not reach the print path.
        seed()
        settings.stored = AppSettings(
            qrLabelColumns = AppSettings.MAX_QR_LABEL_COLUMNS,
            qrLabelNameTextSizePt = AppSettings.MAX_QR_LABEL_TEXT_SIZE_PT,
            qrLabelPriceTextSizePt = AppSettings.MIN_QR_LABEL_TEXT_SIZE_PT
        )
        val file = archiveFile()
        repository.createBackup(Uri.fromFile(file).toString())

        settings.stored = AppSettings.EMPTY
        repository.restoreBackup(Uri.fromFile(file).toString())

        val restored = settings.saved!!
        val allowed =
            AppSettings.MIN_QR_LABEL_TEXT_SIZE_PT..AppSettings.MAX_QR_LABEL_TEXT_SIZE_PT
        assertThat(allowed).contains(restored.qrLabelNameTextSizePt)
        assertThat(allowed).contains(restored.qrLabelPriceTextSizePt)
    }

    @Test
    fun preservesProductFieldsExactly(): Unit = runBlocking {
        seed()
        val file = archiveFile()
        repository.createBackup(Uri.fromFile(file).toString())
        database.productDao().deleteAllProducts()

        repository.restoreBackup(Uri.fromFile(file).toString())

        val product = database.productDao().getProductByCode("CT-A-RoS")
        assertThat(product).isNotNull()
        assertThat(product!!.productName).isEqualTo("Coturno cano alto rosa")
        assertThat(product.price).isEqualTo(130.0)
        assertThat(product.qrCodeData).isEqualTo("qr-1")
        assertThat(product.imageFileName).isEqualTo("ct.jpg")
    }

    @Test
    fun restoreReplacesRatherThanMergesExistingRows(): Unit = runBlocking {
        seed()
        val file = archiveFile()
        repository.createBackup(Uri.fromFile(file).toString())

        database.productDao().insertProducts(
            listOf(ProductEntity("EXTRA", "Cadastrado depois", 9.9, null, null, 3L))
        )

        repository.restoreBackup(Uri.fromFile(file).toString())

        // "Restore" means the archive wins; a leftover row would be data the operator did not ask
        // to keep and cannot tell apart from restored data afterwards.
        assertThat(database.productDao().getProductByCode("EXTRA")).isNull()
        assertThat(database.productDao().getAllProductsOnce()).hasSize(2)
    }

    @Test
    fun dropsImagesThatTheArchiveDoesNotCarry(): Unit = runBlocking {
        seed()
        val file = archiveFile()
        repository.createBackup(Uri.fromFile(file).toString())

        images.files["stale.jpg"] = byteArrayOf(9, 9)

        repository.restoreBackup(Uri.fromFile(file).toString())

        assertThat(images.files.keys).containsExactly("ct.jpg")
    }

    @Test
    fun rejectsAnArchiveThatIsNotABipSaleBackup(): Unit = runBlocking {
        val file = archiveFile()
        file.writeBytes("not a zip".encodeToByteArray())

        val result = runCatching { repository.restoreBackup(Uri.fromFile(file).toString()) }

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun backsUpAnEmptyDatabaseWithoutFailing(): Unit = runBlocking {
        val file = archiveFile()

        val summary = repository.createBackup(Uri.fromFile(file).toString())

        assertThat(summary.products).isEqualTo(0)
        assertThat(summary.sales).isEqualTo(0)
        assertThat(file.length()).isGreaterThan(0L)
    }

    @Test
    fun suggestsAZipFileName(): Unit {
        assertThat(repository.suggestedFileName()).endsWith(".zip")
        assertThat(repository.suggestedFileName()).startsWith("bipsale-backup-")
    }
}
