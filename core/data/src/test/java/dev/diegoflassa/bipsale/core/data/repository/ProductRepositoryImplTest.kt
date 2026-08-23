package dev.diegoflassa.bipsale.core.data.repository

import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.data.model.ProductEntity
import dev.diegoflassa.bipsale.core.domain.model.Product
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ProductRepositoryImplTest {

    private val storedEntity = ProductEntity(
        productCode = "CT-A-RoS",
        productName = "Coturno cano alto rosa",
        price = 130.0,
        qrCodeData = "bipsale://product?code=CT-A-RoS&price=130.0",
        imageFileName = "ct_a_ros_1.jpg",
        lastUpdated = 1_700_000_000_000
    )

    private val product = Product(
        code = "CT-A-RoS",
        name = "Coturno cano alto rosa",
        price = 130.0,
        qrCode = "bipsale://product?code=CT-A-RoS&price=130.0",
        imageFileName = "ct_a_ros_1.jpg"
    )

    private val boom = IllegalStateException("database is closed")

    private fun repository(dao: FakeProductDao) = ProductRepositoryImpl(dao)

    @Test
    fun `maps stored rows onto domain products`() = runTest {
        val products = repository(FakeProductDao(listOf(storedEntity))).getAllProducts().first()

        assertThat(products).containsExactly(product)
    }

    @Test
    fun `reads a product back by its code`() = runTest {
        val found = repository(FakeProductDao(listOf(storedEntity))).getProductByCode("CT-A-RoS")

        assertThat(found).isEqualTo(product)
    }

    @Test
    fun `returns null for a code that is not registered`() = runTest {
        val found = repository(FakeProductDao(listOf(storedEntity))).getProductByCode("NOPE")

        assertThat(found).isNull()
    }

    @Test
    fun `swallows a read failure so a lookup cannot take the screen down`() = runTest {
        // The caller treats null as "not registered"; a scan of an unknown code is routine, and a
        // failed read must not crash the sales screen mid-checkout.
        val found = repository(FakeProductDao(failWith = boom)).getProductByCode("CT-A-RoS")

        assertThat(found).isNull()
    }

    @Test
    fun `persists an inserted product`() = runTest {
        val dao = FakeProductDao()

        repository(dao).insertProduct(product)

        assertThat(dao.rows.single().productCode).isEqualTo("CT-A-RoS")
        assertThat(dao.rows.single().price).isEqualTo(130.0)
    }

    @Test
    fun `rethrows an insert failure instead of reporting a save that never happened`() = runTest {
        val thrown = runCatching {
            repository(FakeProductDao(failWith = boom)).insertProduct(product)
        }.exceptionOrNull()

        assertThat(thrown).isSameInstanceAs(boom)
    }

    @Test
    fun `rethrows an update failure`() = runTest {
        val thrown = runCatching {
            repository(FakeProductDao(failWith = boom)).updateProduct(product)
        }.exceptionOrNull()

        assertThat(thrown).isSameInstanceAs(boom)
    }

    @Test
    fun `rethrows a delete failure instead of reporting a product as removed`() = runTest {
        val thrown = runCatching {
            repository(FakeProductDao(failWith = boom)).deleteProduct(product)
        }.exceptionOrNull()

        assertThat(thrown).isSameInstanceAs(boom)
    }

    @Test
    fun `removes a deleted product from storage`() = runTest {
        val dao = FakeProductDao(listOf(storedEntity))

        repository(dao).deleteProduct(product)

        assertThat(dao.rows).isEmpty()
    }
}
