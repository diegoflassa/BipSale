package dev.diegoflassa.bipsale.core.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The harness `CORE_RULES` §13 requires: it walks a database created at the oldest shipped version
 * through every registered migration and checks the rows are still there afterwards.
 *
 * While only one version exists this reduces to proving the exported schema matches the entities —
 * which is already the guard that catches an entity edited without a version bump. The moment a
 * version 2 lands, the same test starts doing the real work with no changes here.
 */
@RunWith(AndroidJUnit4::class)
class BipSaleDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BipSaleDatabase::class.java
    )

    @Test
    fun everyShippedVersionMigratesToTheCurrentOneWithItsRowsIntact() {
        helper.createDatabase(TEST_DB, FIRST_VERSION).use { db ->
            db.execSQL(
                "INSERT INTO products " +
                    "(productCode, productName, price, qrCodeData, imageFileName, " +
                    "lastUpdated, quantity) " +
                    "VALUES ('CT-A-RoS', 'Coturno cano alto rosa', 130.0, " +
                    "'bipsale://product?code=CT-A-RoS&price=130.0', NULL, 1787400000000, 4)"
            )
            db.execSQL(
                "INSERT INTO sales " +
                    "(id, customerName, customerCpf, totalAmount, discountPercentage, " +
                    "finalAmount, paymentMethod, date, isSynced) " +
                    "VALUES ('sale-1', 'Maria', '12345678909', 130.0, 0.0, 130.0, " +
                    "'PIX', 1787400000000, 0)"
            )
            db.execSQL(
                "INSERT INTO sale_items " +
                    "(id, saleId, productCode, productName, unitPrice, quantity, " +
                    "discountType, discountValue) " +
                    "VALUES ('item-1', 'sale-1', 'CT-A-RoS', 'Coturno cano alto rosa', " +
                    "130.0, 1, 'NONE', 0.0)"
            )
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            BipSaleDatabase.VERSION,
            true,
            *BipSaleDatabase.MIGRATIONS
        )

        migrated.use { db ->
            assertThat(db.countOf("products")).isEqualTo(1)
            assertThat(db.countOf("sales")).isEqualTo(1)
            assertThat(db.countOf("sale_items")).isEqualTo(1)

            // A sale that survives with its amount changed is worse than one that fails to open.
            db.query("SELECT finalAmount FROM sales WHERE id = 'sale-1'").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getDouble(0)).isEqualTo(130.0)
            }
        }
    }

    @Test
    fun theCurrentVersionHasAnExportedSchemaCommitted() {
        // createDatabase can only build a version whose schema JSON is in the test assets, so a
        // version bump that forgets to commit the export fails right here.
        helper.createDatabase(TEST_DB, BipSaleDatabase.VERSION).close()
    }

    @Test
    fun oneMigrationIsRegisteredForEveryVersionStep() {
        // Room needs a migration for each step from 1 to the current version. Counting them here
        // catches a bumped version whose migration was never added to the array.
        assertThat(BipSaleDatabase.MIGRATIONS).hasLength(BipSaleDatabase.VERSION - FIRST_VERSION)
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.countOf(table: String): Int =
        query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private companion object {
        const val TEST_DB = "bipsale-migration-test"
        const val FIRST_VERSION = 1
    }
}
