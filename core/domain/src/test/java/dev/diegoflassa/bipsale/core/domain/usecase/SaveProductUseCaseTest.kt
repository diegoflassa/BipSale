package dev.diegoflassa.bipsale.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SaveProductUseCaseTest {

    private val repository = FakeProductRepository()
    private val useCase = SaveProductUseCase(repository)

    @Test
    fun `persists a valid product`() = runTest {
        val result = useCase("CT-A-RoS", "Coturno cano alto rosa", 130.0)

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.stored).hasSize(1)
        assertThat(repository.stored.first().price).isEqualTo(130.0)
    }

    @Test
    fun `rejects a zero price instead of persisting a free product`() = runTest {
        val result = useCase("CT-A-RoS", "Coturno cano alto rosa", 0.0)

        assertThat(result.isFailure).isTrue()
        assertThat(repository.stored).isEmpty()
    }

    @Test
    fun `rejects a negative price`() = runTest {
        val result = useCase("CT-A-RoS", "Coturno cano alto rosa", -1.0)

        assertThat(result.isFailure).isTrue()
        assertThat(repository.stored).isEmpty()
    }

    @Test
    fun `rejects a blank code`() = runTest {
        assertThat(useCase("", "Coturno", 130.0).isFailure).isTrue()
        assertThat(repository.stored).isEmpty()
    }

    @Test
    fun `rejects a blank name`() = runTest {
        assertThat(useCase("CT-A-RoS", "  ", 130.0).isFailure).isTrue()
        assertThat(repository.stored).isEmpty()
    }

    @Test
    fun `carries the image file name through to the stored product`() = runTest {
        useCase("CT-A-RoS", "Coturno", 130.0, imageFileName = "ct_a_ros_123.jpg")

        assertThat(repository.stored.first().imageFileName).isEqualTo("ct_a_ros_123.jpg")
    }

    @Test
    fun `encodes code and price into the qr payload`() = runTest {
        useCase("CT-A-RoS", "Coturno", 130.0)

        assertThat(repository.stored.first().qrCode)
            .isEqualTo("bipsale://product?code=CT-A-RoS&price=130.0")
    }
}
