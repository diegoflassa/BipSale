package dev.diegoflassa.bipsale.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.domain.model.Product
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AddProductByQrUseCaseTest {

    private val registered = Product(
        code = "CT-A-RoS",
        name = "Coturno cano alto rosa",
        price = 130.0,
        qrCode = "bipsale://product?code=CT-A-RoS&price=130.0"
    )

    private fun useCase(vararg products: Product) =
        AddProductByQrUseCase(FakeProductRepository(products.toList()))

    @Test
    fun `reads the registered product behind a scanned code`() = runTest {
        val item = useCase(registered)("bipsale://product?code=CT-A-RoS&price=130.0").getOrThrow()

        assertThat(item.productCode).isEqualTo("CT-A-RoS")
        assertThat(item.productName).isEqualTo("Coturno cano alto rosa")
        assertThat(item.unitPrice).isEqualTo(130.0)
        assertThat(item.quantity).isEqualTo(1)
    }

    @Test
    fun `trusts the price printed on the label over the registered one`() = runTest {
        // A label already in the customer's hands is the price the shop advertised.
        val item = useCase(registered)("bipsale://product?code=CT-A-RoS&price=99.9").getOrThrow()

        assertThat(item.unitPrice).isEqualTo(99.9)
    }

    @Test
    fun `falls back to the registered price when the label carries none`() = runTest {
        val item = useCase(registered)("bipsale://product?code=CT-A-RoS").getOrThrow()

        assertThat(item.unitPrice).isEqualTo(130.0)
    }

    @Test
    fun `names an unregistered code after itself rather than dropping the scan`() = runTest {
        val item = useCase()("bipsale://product?code=UNKNOWN-1&price=42.0").getOrThrow()

        assertThat(item.productName).isEqualTo("UNKNOWN-1")
        assertThat(item.unitPrice).isEqualTo(42.0)
    }

    @Test
    fun `rejects a payload with no code parameter`() = runTest {
        assertThat(useCase()("bipsale://product?price=10.0").isFailure).isTrue()
    }

    @Test
    fun `rejects a payload that is not a query at all`() = runTest {
        assertThat(useCase()("just some text").isFailure).isTrue()
    }

    @Test
    fun `ignores an unparseable price and uses the registered one`() = runTest {
        val item = useCase(registered)("bipsale://product?code=CT-A-RoS&price=abc").getOrThrow()

        assertThat(item.unitPrice).isEqualTo(130.0)
    }

    @Test
    fun `surfaces a repository failure instead of adding a free line`() = runTest {
        val failing = AddProductByQrUseCase(
            FakeProductRepository(failWith = IllegalStateException("database closed"))
        )

        assertThat(failing("bipsale://product?code=CT-A-RoS").isFailure).isTrue()
    }
}
