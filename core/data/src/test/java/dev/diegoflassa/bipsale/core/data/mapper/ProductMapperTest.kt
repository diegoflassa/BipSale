package dev.diegoflassa.bipsale.core.data.mapper

import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.data.model.ProductEntity
import dev.diegoflassa.bipsale.core.domain.model.Product
import org.junit.Test

class ProductMapperTest {

    private val product = Product(
        code = "CT-A-RoS",
        name = "Coturno cano alto rosa",
        price = 130.0,
        qrCode = "bipsale://product?code=CT-A-RoS&price=130.0",
        imageFileName = "ct_a_ros_1.jpg"
    )

    @Test
    fun `a product survives the round trip`() {
        assertThat(product.toEntity().toDomain()).isEqualTo(product)
    }

    @Test
    fun `a product without an image or a qr payload survives the round trip`() {
        val bare = product.copy(qrCode = null, imageFileName = null)

        assertThat(bare.toEntity().toDomain()).isEqualTo(bare)
    }

    @Test
    fun `writing a product stamps when it was last touched`() {
        val before = System.currentTimeMillis()

        val entity = product.toEntity()

        assertThat(entity.lastUpdated).isAtLeast(before)
    }

    @Test
    fun `the domain model carries no storage timestamp`() {
        // lastUpdated is a storage concern; a round trip must not invent one on the domain side.
        val entity = ProductEntity(
            productCode = "CT-A-RoS",
            productName = "Coturno cano alto rosa",
            price = 130.0,
            qrCodeData = null,
            imageFileName = null,
            lastUpdated = 42
        )

        assertThat(entity.toDomain()).isEqualTo(
            Product("CT-A-RoS", "Coturno cano alto rosa", 130.0, null, null)
        )
    }
}
