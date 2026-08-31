package dev.diegoflassa.bipsale.feature.sales

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.domain.model.ItemDiscount
import dev.diegoflassa.bipsale.core.domain.image.ProductImageStore
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.settings.AppSettings
import dev.diegoflassa.bipsale.core.domain.settings.SettingsRepository
import dev.diegoflassa.bipsale.core.domain.model.Product
import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.domain.repository.ProductRepository
import dev.diegoflassa.bipsale.core.domain.repository.SaleRepository
import dev.diegoflassa.bipsale.core.domain.usecase.AddProductByCodeUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.AddProductByQrUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.FinalizeSaleUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.GetProductsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SalesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeProductRepository(private val products: List<Product>) : ProductRepository {
        override fun getAllProducts(): Flow<List<Product>> = flowOf(products)
        override suspend fun getProductByCode(code: String): Product? =
            products.firstOrNull { it.code == code }

        override suspend fun insertProduct(product: Product) = Unit
        override suspend fun updateProduct(product: Product) = Unit
        override suspend fun deleteProduct(product: Product) = Unit
    }

    private class FakeSaleRepository(private val failWith: Throwable? = null) : SaleRepository {
        val inserted = mutableListOf<Sale>()
        override fun getAllSales(): Flow<List<Sale>> = flowOf(inserted.toList())
        override suspend fun getSaleById(saleId: String): Sale? = null
        override suspend fun insertFullSale(sale: Sale) {
            failWith?.let { throw it }
            inserted += sale
        }

        override fun searchSales(query: String): Flow<List<Sale>> = flowOf(emptyList())
        override fun getSalesByDateRange(startDate: Long, endDate: Long): Flow<List<Sale>> =
            flowOf(emptyList())
    }

    private val catalog = listOf(
        Product("CF-200", "Cafe Premium 200ml", 12.50, null),
        Product("CH-064", "Chocolate meio amargo 90g", 64.90, null),
        Product("CT-A-RoS", "Coturno cano alto rosa", 130.0, null),
        Product("PR-100", "Prancheta oficio", 100.0, null)
    )

    private val saleRepository = FakeSaleRepository()

    private class FakeSettingsRepository(private val stored: AppSettings) : SettingsRepository {
        override val settings: Flow<AppSettings> = flowOf(stored)
        override suspend fun current(): AppSettings = stored
        override suspend fun save(settings: AppSettings) = Unit
    }

    private class FakeProductImageStore : ProductImageStore {
        override suspend fun save(sourceUri: String, productCode: String): String = ""
        override suspend fun delete(fileName: String) = Unit
        override fun resolvePath(fileName: String): String = "/images/$fileName"
        override suspend fun listFileNames(): List<String> = emptyList()
        override suspend fun readBytes(fileName: String): ByteArray? = null
        override suspend fun writeBytes(fileName: String, bytes: ByteArray) = Unit
        override suspend fun deleteAll() = Unit
    }

    private fun viewModel(
        products: List<Product> = catalog,
        sales: FakeSaleRepository = saleRepository,
        settings: AppSettings = AppSettings.EMPTY
    ): SalesViewModel {
        val productRepository = FakeProductRepository(products)
        return SalesViewModel(
            finalizeSaleUseCase = FinalizeSaleUseCase(sales),
            addProductByQrUseCase = AddProductByQrUseCase(productRepository),
            addProductByCodeUseCase = AddProductByCodeUseCase(productRepository),
            getProductsUseCase = GetProductsUseCase(productRepository),
            settingsRepository = FakeSettingsRepository(settings),
            productImageStore = FakeProductImageStore()
        )
    }

    /**
     * Seeds the cart the way the screen does — one intent per line, no test-only entry point.
     * A payment method comes with it, since without one the cart is deliberately not finalizable.
     */
    private fun TestScope.cartOf(
        vm: SalesViewModel,
        vararg codes: String,
        method: PaymentMethod? = PaymentMethod.CASH
    ) {
        codes.forEach { vm.onIntent(SalesContract.Intent.AddProductByCode(it)) }
        method?.let { vm.onIntent(SalesContract.Intent.SelectPaymentMethod(it)) }
        advanceUntilIdle()
    }

    @Test
    fun `an empty cart totals nothing`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.totalAmount).isEqualTo(0.0)
        assertThat(vm.uiState.value.finalAmount).isEqualTo(0.0)
        assertThat(vm.uiState.value.canFinalize).isFalse()
    }

    @Test
    fun `a scanned product lands in the cart and moves the total`() = runTest {
        val vm = viewModel()

        vm.onIntent(
            SalesContract.Intent.AddProductByQr("bipsale://product?code=CT-A-RoS&price=130.0")
        )
        advanceUntilIdle()

        assertThat(vm.uiState.value.items).hasSize(1)
        assertThat(vm.uiState.value.totalAmount).isEqualTo(130.0)
        assertThat(vm.uiState.value.finalAmount).isEqualTo(130.0)
    }

    @Test
    fun `a cart with no payment method chosen cannot be finalized`() = runTest {
        val vm = viewModel()
        cartOf(vm, "PR-100", method = null)

        assertThat(vm.uiState.value.paymentMethod).isNull()
        assertThat(vm.uiState.value.canFinalize).isFalse()

        vm.onIntent(SalesContract.Intent.FinalizeSale)
        advanceUntilIdle()

        assertThat(saleRepository.inserted).isEmpty()
    }

    @Test
    fun `choosing a payment method is what makes the cart finalizable`() = runTest {
        val vm = viewModel()
        cartOf(vm, "PR-100", method = null)

        vm.onIntent(SalesContract.Intent.SelectPaymentMethod(PaymentMethod.CASH))
        advanceUntilIdle()

        assertThat(vm.uiState.value.canFinalize).isTrue()
    }

    @Test
    fun `picking PIX applies the configured default discount`() = runTest {
        val vm = viewModel(
            settings = AppSettings(
                pixDiscount = ItemDiscount.Percentage(10.0)
            )
        )
        cartOf(vm, "PR-100", method = null)

        vm.onIntent(SalesContract.Intent.SelectPaymentMethod(PaymentMethod.PIX))
        advanceUntilIdle()

        assertThat(vm.uiState.value.discountPercentage).isEqualTo(10.0)
        assertThat(vm.uiState.value.finalAmount).isEqualTo(90.0)
    }

    @Test
    fun `a discount the operator typed is not overwritten by the PIX default`() = runTest {
        val vm = viewModel(
            settings = AppSettings(
                pixDiscount = ItemDiscount.Percentage(10.0)
            )
        )
        cartOf(vm, "PR-100", method = null)
        vm.onIntent(SalesContract.Intent.UpdateDiscount(25.0))
        advanceUntilIdle()

        vm.onIntent(SalesContract.Intent.SelectPaymentMethod(PaymentMethod.PIX))
        advanceUntilIdle()

        assertThat(vm.uiState.value.discountPercentage).isEqualTo(25.0)
    }

    @Test
    fun `picking PIX produces a payload carrying the total`() = runTest {
        val vm = viewModel()
        cartOf(vm, "PR-100", method = null)

        vm.onIntent(SalesContract.Intent.SelectPaymentMethod(PaymentMethod.PIX))
        advanceUntilIdle()

        val payload = vm.uiState.value.pixPayload
        assertThat(payload).isNotNull()
        assertThat(payload).contains("br.gov.bcb.pix")
        assertThat(payload).contains("100.00")
    }

    @Test
    fun `the PIX payload follows the total when the cart changes`() = runTest {
        val vm = viewModel()
        cartOf(vm, "PR-100", method = PaymentMethod.PIX)

        vm.onIntent(SalesContract.Intent.AddProductByCode("CF-200"))
        advanceUntilIdle()

        assertThat(vm.uiState.value.pixPayload).contains("112.50")
    }

    @Test
    fun `leaving PIX clears the payload rather than leaving it on screen`() = runTest {
        val vm = viewModel()
        cartOf(vm, "PR-100", method = PaymentMethod.PIX)

        vm.onIntent(SalesContract.Intent.SelectPaymentMethod(PaymentMethod.CASH))
        advanceUntilIdle()

        assertThat(vm.uiState.value.pixPayload).isNull()
    }

    @Test
    fun `toggling to no-fixed-value produces a payload without an amount`() = runTest {
        val vm = viewModel()
        cartOf(vm, "PR-100", method = PaymentMethod.PIX)

        vm.onIntent(SalesContract.Intent.TogglePixAmount(carriesAmount = false))
        advanceUntilIdle()

        val payload = vm.uiState.value.pixPayload
        assertThat(payload).isNotNull()
        assertThat(payload).contains("br.gov.bcb.pix")
        assertThat(payload).doesNotContain("100.00")
    }

    @Test
    fun `toggling back to fixed-value restores the amount in the payload`() = runTest {
        val vm = viewModel()
        cartOf(vm, "PR-100", method = PaymentMethod.PIX)

        vm.onIntent(SalesContract.Intent.TogglePixAmount(carriesAmount = false))
        advanceUntilIdle()
        vm.onIntent(SalesContract.Intent.TogglePixAmount(carriesAmount = true))
        advanceUntilIdle()

        val payload = vm.uiState.value.pixPayload
        assertThat(payload).isNotNull()
        assertThat(payload).contains("100.00")
    }

    @Test
    fun `leaving PIX strips the auto-applied discount`() = runTest {
        val vm = viewModel(
            settings = AppSettings(pixDiscount = ItemDiscount.Percentage(10.0))
        )
        cartOf(vm, "PR-100", method = PaymentMethod.PIX)
        assertThat(vm.uiState.value.discountPercentage).isEqualTo(10.0)

        vm.onIntent(SalesContract.Intent.SelectPaymentMethod(PaymentMethod.CASH))
        advanceUntilIdle()

        assertThat(vm.uiState.value.discountPercentage).isEqualTo(0.0)
        assertThat(vm.uiState.value.finalAmount).isEqualTo(100.0)
    }

    @Test
    fun `a manually typed discount survives switching away from PIX`() = runTest {
        val vm = viewModel(
            settings = AppSettings(pixDiscount = ItemDiscount.Percentage(10.0))
        )
        cartOf(vm, "PR-100", method = PaymentMethod.PIX)
        vm.onIntent(SalesContract.Intent.UpdateDiscount(25.0))
        advanceUntilIdle()

        vm.onIntent(SalesContract.Intent.SelectPaymentMethod(PaymentMethod.CASH))
        advanceUntilIdle()

        assertThat(vm.uiState.value.discountPercentage).isEqualTo(25.0)
    }

    @Test
    fun `re-selecting PIX reapplies the default discount after it was stripped`() = runTest {
        val vm = viewModel(
            settings = AppSettings(pixDiscount = ItemDiscount.Percentage(10.0))
        )
        cartOf(vm, "PR-100", method = PaymentMethod.PIX)
        vm.onIntent(SalesContract.Intent.SelectPaymentMethod(PaymentMethod.CASH))
        advanceUntilIdle()
        assertThat(vm.uiState.value.discountPercentage).isEqualTo(0.0)

        vm.onIntent(SalesContract.Intent.SelectPaymentMethod(PaymentMethod.PIX))
        advanceUntilIdle()

        assertThat(vm.uiState.value.discountPercentage).isEqualTo(10.0)
    }

    @Test
    fun `an unreadable scan reports an error and leaves the cart alone`() = runTest {
        val vm = viewModel()

        vm.effect.test {
            vm.onIntent(SalesContract.Intent.AddProductByQr("not a qr payload"))
            advanceUntilIdle()

            assertThat(awaitItem()).isInstanceOf(SalesContract.Effect.ShowError::class.java)
        }
        assertThat(vm.uiState.value.items).isEmpty()
    }

    @Test
    fun `removing a line recomputes the total`() = runTest {
        val vm = viewModel()
        cartOf(vm, "CF-200", "CH-064")

        vm.onIntent(SalesContract.Intent.RemoveItem(vm.uiState.value.items.first().id))
        advanceUntilIdle()

        assertThat(vm.uiState.value.items).hasSize(1)
        assertThat(vm.uiState.value.totalAmount).isEqualTo(64.90)
    }

    @Test
    fun `removing one of two identical lines leaves the other`() = runTest {
        val vm = viewModel()
        cartOf(vm, "CF-200", "CF-200")
        val survivor = vm.uiState.value.items.last().id

        vm.onIntent(SalesContract.Intent.RemoveItem(vm.uiState.value.items.first().id))
        advanceUntilIdle()

        assertThat(vm.uiState.value.items.map { it.id }).containsExactly(survivor)
        assertThat(vm.uiState.value.totalAmount).isEqualTo(12.50)
    }

    @Test
    fun `a sale discount lowers the amount charged but not the subtotal`() = runTest {
        val vm = viewModel()
        cartOf(vm, "CF-200", "CF-200", "CH-064")

        vm.onIntent(SalesContract.Intent.UpdateDiscount(10.0))
        advanceUntilIdle()

        assertThat(vm.uiState.value.totalAmount).isEqualTo(89.90)
        assertThat(vm.uiState.value.finalAmount).isEqualTo(80.91)
    }

    @Test
    fun `a discount outside zero to one hundred is refused, not clamped silently`() = runTest {
        val vm = viewModel()
        cartOf(vm, "PR-100")

        vm.effect.test {
            vm.onIntent(SalesContract.Intent.UpdateDiscount(150.0))
            advanceUntilIdle()

            assertThat(awaitItem()).isInstanceOf(SalesContract.Effect.ShowError::class.java)
        }
        assertThat(vm.uiState.value.discountPercentage).isEqualTo(0.0)
        assertThat(vm.uiState.value.finalAmount).isEqualTo(100.0)
    }

    @Test
    fun `a fixed per-line discount comes off before the sale discount`() = runTest {
        val vm = viewModel()
        cartOf(vm, "PR-100")
        val lineId = vm.uiState.value.items.single().id

        vm.onIntent(SalesContract.Intent.UpdateItemDiscount(lineId, ItemDiscount.Amount(10.0)))
        vm.onIntent(SalesContract.Intent.UpdateDiscount(10.0))
        advanceUntilIdle()

        assertThat(vm.uiState.value.totalAmount).isEqualTo(100.0)
        assertThat(vm.uiState.value.itemDiscountAmount).isEqualTo(10.0)
        assertThat(vm.uiState.value.finalAmount).isEqualTo(81.0)
    }

    @Test
    fun `a percentage per-line discount only touches its own line`() = runTest {
        val vm = viewModel()
        cartOf(vm, "PR-100", "CF-200")
        val discounted = vm.uiState.value.items.first { it.productCode == "PR-100" }.id

        vm.onIntent(
            SalesContract.Intent.UpdateItemDiscount(discounted, ItemDiscount.Percentage(25.0))
        )
        advanceUntilIdle()

        assertThat(vm.uiState.value.totalAmount).isEqualTo(112.50)
        assertThat(vm.uiState.value.itemDiscountAmount).isEqualTo(25.0)
        assertThat(vm.uiState.value.finalAmount).isEqualTo(87.50)
    }

    @Test
    fun `discounting a line that is no longer in the cart changes nothing`() = runTest {
        val vm = viewModel()
        cartOf(vm, "PR-100")

        vm.onIntent(
            SalesContract.Intent.UpdateItemDiscount("not-a-line", ItemDiscount.Amount(50.0))
        )
        advanceUntilIdle()

        assertThat(vm.uiState.value.finalAmount).isEqualTo(100.0)
    }

    @Test
    fun `finalizing persists the total the cart was showing`() = runTest {
        val vm = viewModel()
        cartOf(vm, "CF-200", "CF-200", "CH-064")
        vm.onIntent(SalesContract.Intent.UpdateDiscount(10.0))
        advanceUntilIdle()

        vm.onIntent(SalesContract.Intent.FinalizeSale)
        advanceUntilIdle()

        val sale = saleRepository.inserted.single()
        assertThat(sale.totalAmount).isEqualTo(vm.uiState.value.totalAmount)
        assertThat(sale.finalAmount).isEqualTo(vm.uiState.value.finalAmount)
        assertThat(sale.finalAmount).isEqualTo(80.91)
    }

    @Test
    fun `per-line discounts survive into the persisted sale`() = runTest {
        val vm = viewModel()
        cartOf(vm, "PR-100")
        val lineId = vm.uiState.value.items.single().id
        vm.onIntent(SalesContract.Intent.UpdateItemDiscount(lineId, ItemDiscount.Percentage(20.0)))
        advanceUntilIdle()

        vm.onIntent(SalesContract.Intent.FinalizeSale)
        advanceUntilIdle()

        val sale = saleRepository.inserted.single()
        assertThat(sale.items.single().discount).isEqualTo(ItemDiscount.Percentage(20.0))
        assertThat(sale.finalAmount).isEqualTo(80.0)
    }

    @Test
    fun `an anonymous sale carries no customer identity`() = runTest {
        val vm = viewModel()
        vm.onIntent(SalesContract.Intent.UpdateCustomerInfo("Ana Paula", "123.456.789-00", true))
        cartOf(vm, "PR-100")

        vm.onIntent(SalesContract.Intent.FinalizeSale)
        advanceUntilIdle()

        val sale = saleRepository.inserted.single()
        assertThat(sale.customerName).isEmpty()
        assertThat(sale.customerCpf).isEmpty()
    }

    @Test
    fun `a named sale keeps the customer it was rung up for`() = runTest {
        val vm = viewModel()
        vm.onIntent(SalesContract.Intent.UpdateCustomerInfo("Ana Paula", "123.456.789-00", false))
        cartOf(vm, "PR-100")

        vm.onIntent(SalesContract.Intent.FinalizeSale)
        advanceUntilIdle()

        assertThat(saleRepository.inserted.single().customerName).isEqualTo("Ana Paula")
    }

    @Test
    fun `a failed write reports an error and never claims the sale finished`() = runTest {
        val failing = FakeSaleRepository(IllegalStateException("disk full"))
        val vm = viewModel(sales = failing)
        cartOf(vm, "PR-100")

        vm.effect.test {
            vm.onIntent(SalesContract.Intent.FinalizeSale)
            advanceUntilIdle()

            assertThat(awaitItem()).isInstanceOf(SalesContract.Effect.ShowError::class.java)
        }
        assertThat(vm.uiState.value.isSaleFinished).isFalse()
        assertThat(vm.uiState.value.isFinalizing).isFalse()
        assertThat(vm.uiState.value.canFinalize).isTrue()
    }

    @Test
    fun `finalizing an empty cart is refused`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.effect.test {
            vm.onIntent(SalesContract.Intent.FinalizeSale)
            advanceUntilIdle()

            assertThat(awaitItem()).isInstanceOf(SalesContract.Effect.ShowError::class.java)
        }
        assertThat(saleRepository.inserted).isEmpty()
    }

    @Test
    fun `typing a registered code adds it to the cart`() = runTest {
        val vm = viewModel()

        vm.onIntent(SalesContract.Intent.AddProductByCode("CF-200"))
        advanceUntilIdle()

        assertThat(vm.uiState.value.items.single().productCode).isEqualTo("CF-200")
        assertThat(vm.uiState.value.totalAmount).isEqualTo(12.50)
    }

    @Test
    fun `typing an unknown code reports an error and adds nothing`() = runTest {
        val vm = viewModel()

        vm.effect.test {
            vm.onIntent(SalesContract.Intent.AddProductByCode("NOPE"))
            advanceUntilIdle()

            assertThat(awaitItem()).isInstanceOf(SalesContract.Effect.ShowError::class.java)
        }
        assertThat(vm.uiState.value.items).isEmpty()
    }

    @Test
    fun `the catalogue is available for picking a product without a scan`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.catalog.map { it.code })
            .containsExactly("CF-200", "CH-064", "CT-A-RoS", "PR-100")
    }

    @Test
    fun `the selected payment method reaches the persisted sale`() = runTest {
        val vm = viewModel()
        cartOf(vm, "PR-100")
        vm.onIntent(SalesContract.Intent.SelectPaymentMethod(PaymentMethod.DEBIT_CARD))
        advanceUntilIdle()

        vm.onIntent(SalesContract.Intent.FinalizeSale)
        advanceUntilIdle()

        assertThat(saleRepository.inserted.single().paymentMethod)
            .isEqualTo(PaymentMethod.DEBIT_CARD)
    }
}
