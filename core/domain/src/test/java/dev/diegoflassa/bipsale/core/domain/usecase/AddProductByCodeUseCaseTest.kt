package dev.diegoflassa.bipsale.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.domain.model.Product
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AddProductByCodeUseCaseTest {

    private val registered = Product(
        code = "CT-A-RoS",
        name = "Coturno cano alto rosa",
        price = 130.0,
        qrCode = null
    )

    private fun useCase(vararg products: Product) =
        AddProductByCodeUseCase(FakeProductRepository(products.toList()))

    @Test
    fun `adds the registered product as a single line`() = runTest {
        val item = useCase(registered)("CT-A-RoS").getOrThrow()

        assertThat(item.productCode).isEqualTo("CT-A-RoS")
        assertThat(item.productName).isEqualTo("Coturno cano alto rosa")
        assertThat(item.unitPrice).isEqualTo(130.0)
        assertThat(item.quantity).isEqualTo(1)
    }

    @Test
    fun `ignores surrounding whitespace from a typed code`() = runTest {
        assertThat(useCase(registered)("  CT-A-RoS  ").isSuccess).isTrue()
    }

    @Test
    fun `refuses an unknown code instead of ringing up a zero-priced line`() = runTest {
        val result = useCase(registered)("NOT-A-PRODUCT")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NoSuchElementException::class.java)
    }

    @Test
    fun `refuses a blank code`() = runTest {
        assertThat(useCase(registered)("   ").isFailure).isTrue()
    }

    @Test
    fun `surfaces a repository failure`() = runTest {
        val failing = AddProductByCodeUseCase(
            FakeProductRepository(failWith = IllegalStateException("database closed"))
        )

        assertThat(failing("CT-A-RoS").isFailure).isTrue()
    }
}
