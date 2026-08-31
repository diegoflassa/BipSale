package dev.diegoflassa.bipsale.feature.products

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.domain.model.Product
import dev.diegoflassa.bipsale.core.domain.repository.ProductRepository
import dev.diegoflassa.bipsale.core.domain.usecase.DeleteProductUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.GetProductUseCase
import dev.diegoflassa.bipsale.core.domain.product.ImportedProduct
import dev.diegoflassa.bipsale.core.domain.product.ProductImportRejection
import dev.diegoflassa.bipsale.core.domain.product.ProductImportReport
import dev.diegoflassa.bipsale.core.domain.product.ProductImportRepository
import dev.diegoflassa.bipsale.core.domain.usecase.GetProductsUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.ImportProductsUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.SaveProductImageUseCase
import dev.diegoflassa.bipsale.core.domain.settings.AppSettings
import dev.diegoflassa.bipsale.core.domain.settings.SettingsRepository
import dev.diegoflassa.bipsale.core.domain.usecase.SaveProductUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val coturno = Product(
        code = "CT-A-RoS",
        name = "Coturno cano alto rosa",
        price = 130.0,
        qrCode = "bipsale://product?code=CT-A-RoS&price=130.0",
        imageFileName = "ct_a_ros_1.jpg"
    )
    private val cafe = Product("CF-200", "Cafe Premium 200ml", 12.50, null)

    private class FakeProductRepository(
        initial: List<Product> = emptyList(),
        private val failWith: Throwable? = null,
        private val listSource: Flow<List<Product>>? = null
    ) : ProductRepository {
        val products = MutableStateFlow(initial)
        val deleted = mutableListOf<Product>()

        override fun getAllProducts(): Flow<List<Product>> = listSource ?: products

        override suspend fun getProductByCode(code: String): Product? {
            failWith?.let { throw it }
            return products.value.firstOrNull { it.code == code }
        }

        override suspend fun insertProduct(product: Product) {
            failWith?.let { throw it }
            products.value = products.value.filterNot { it.code == product.code } + product
        }

        override suspend fun updateProduct(product: Product) = Unit

        override suspend fun deleteProduct(product: Product) {
            failWith?.let { throw it }
            deleted += product
            products.value = products.value - product
        }
    }

    private class FakeProductImportRepository(
        private val report: ProductImportReport = ProductImportReport()
    ) : ProductImportRepository {
        var writtenTemplateTo: String? = null

        override suspend fun writeTemplate(destinationUri: String) {
            writtenTemplateTo = destinationUri
        }

        override suspend fun read(sourceUri: String): ProductImportReport = report
        override fun suggestedTemplateName(): String = "modelo.xlsx"
    }

    private class FakeSettingsRepository : SettingsRepository {
        override val settings: Flow<AppSettings> = flow { emit(AppSettings.EMPTY) }
        override suspend fun current(): AppSettings = AppSettings.EMPTY
        override suspend fun save(settings: AppSettings) = Unit
    }

    private fun viewModel(
        repository: FakeProductRepository = FakeProductRepository(),
        imageStore: FakeProductImageStore = FakeProductImageStore(),
        importRepository: FakeProductImportRepository = FakeProductImportRepository(),
        settingsRepository: FakeSettingsRepository = FakeSettingsRepository()
    ) = ProductViewModel(
        getProducts = GetProductsUseCase(repository),
        getProduct = GetProductUseCase(repository),
        saveProduct = SaveProductUseCase(repository),
        importProducts = ImportProductsUseCase(importRepository, repository),
        deleteProduct = DeleteProductUseCase(repository, imageStore),
        saveProductImage = SaveProductImageUseCase(imageStore),
        productImageStore = imageStore,
        settingsRepository = settingsRepository
    )

    @Test
    fun `the list loads on creation and stops the spinner`() = runTest {
        val vm = viewModel(FakeProductRepository(listOf(coturno, cafe)))
        advanceUntilIdle()

        assertThat(vm.uiState.value.products.map { it.code }).containsExactly("CT-A-RoS", "CF-200")
        assertThat(vm.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `prices reach the list already formatted`() = runTest {
        val vm = viewModel(FakeProductRepository(listOf(cafe)))
        advanceUntilIdle()

        val formatted = vm.uiState.value.products.single().priceFormatted
        assertThat(formatted).contains("12,50")
    }

    @Test
    fun `an image file name reaches the list as a resolvable path`() = runTest {
        val vm = viewModel(FakeProductRepository(listOf(coturno)))
        advanceUntilIdle()

        assertThat(vm.uiState.value.products.single().imagePath)
            .isEqualTo("/data/product_images/ct_a_ros_1.jpg")
    }

    @Test
    fun `a product without an image has no path`() = runTest {
        val vm = viewModel(FakeProductRepository(listOf(cafe)))
        advanceUntilIdle()

        assertThat(vm.uiState.value.products.single().imagePath).isNull()
    }

    @Test
    fun `a list failure surfaces as an error and stops the spinner`() = runTest {
        val repository = FakeProductRepository(
            listSource = flow { throw IllegalStateException("database is closed") }
        )
        val vm = viewModel(repository)
        advanceUntilIdle()

        assertThat(vm.uiState.value.isLoading).isFalse()
        assertThat(vm.uiState.value.errorMessage).isNotNull()
    }

    @Test
    fun `editing a code and a price makes the form saveable`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onIntent(ProductContract.Intent.CodeChanged("CT-A-RoS"))
        vm.onIntent(ProductContract.Intent.NameChanged("Coturno cano alto rosa"))
        vm.onIntent(ProductContract.Intent.PriceChanged("130,00"))

        assertThat(vm.uiState.value.editor.canSave).isTrue()
    }

    @Test
    fun `a price typed with a comma is accepted`() = runTest {
        val repository = FakeProductRepository()
        val vm = viewModel(repository)
        advanceUntilIdle()
        vm.onIntent(ProductContract.Intent.CodeChanged("CF-200"))
        vm.onIntent(ProductContract.Intent.NameChanged("Cafe Premium 200ml"))
        vm.onIntent(ProductContract.Intent.PriceChanged("12,50"))

        vm.onIntent(ProductContract.Intent.SaveProduct)
        advanceUntilIdle()

        assertThat(repository.products.value.single().price).isEqualTo(12.50)
    }

    @Test
    fun `an unparseable price is refused before it reaches storage`() = runTest {
        val repository = FakeProductRepository()
        val vm = viewModel(repository)
        advanceUntilIdle()
        vm.onIntent(ProductContract.Intent.CodeChanged("CF-200"))
        vm.onIntent(ProductContract.Intent.NameChanged("Cafe Premium 200ml"))
        vm.onIntent(ProductContract.Intent.PriceChanged("de graca"))

        vm.effect.test {
            vm.onIntent(ProductContract.Intent.SaveProduct)
            advanceUntilIdle()

            assertThat(awaitItem()).isInstanceOf(ProductContract.Effect.ShowSnackbar::class.java)
        }
        assertThat(repository.products.value).isEmpty()
    }

    @Test
    fun `a zero price is refused rather than saved as a free product`() = runTest {
        val repository = FakeProductRepository()
        val vm = viewModel(repository)
        advanceUntilIdle()
        vm.onIntent(ProductContract.Intent.CodeChanged("CF-200"))
        vm.onIntent(ProductContract.Intent.NameChanged("Cafe Premium 200ml"))
        vm.onIntent(ProductContract.Intent.PriceChanged("0"))

        vm.onIntent(ProductContract.Intent.SaveProduct)
        advanceUntilIdle()

        assertThat(repository.products.value).isEmpty()
    }

    @Test
    fun `a successful save navigates back`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onIntent(ProductContract.Intent.CodeChanged("CF-200"))
        vm.onIntent(ProductContract.Intent.NameChanged("Cafe Premium 200ml"))
        vm.onIntent(ProductContract.Intent.PriceChanged("12,50"))

        vm.effect.test {
            vm.onIntent(ProductContract.Intent.SaveProduct)
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(ProductContract.Effect.NavigationBack)
        }
    }

    @Test
    fun `a failed save leaves the form editable instead of hanging on saving`() = runTest {
        val repository = FakeProductRepository(failWith = IllegalStateException("disk full"))
        val vm = viewModel(repository)
        advanceUntilIdle()
        vm.onIntent(ProductContract.Intent.CodeChanged("CF-200"))
        vm.onIntent(ProductContract.Intent.NameChanged("Cafe Premium 200ml"))
        vm.onIntent(ProductContract.Intent.PriceChanged("12,50"))

        vm.onIntent(ProductContract.Intent.SaveProduct)
        advanceUntilIdle()

        assertThat(vm.uiState.value.editor.isSaving).isFalse()
    }

    @Test
    fun `loading a product fills the editor from storage`() = runTest {
        val vm = viewModel(FakeProductRepository(listOf(coturno)))
        advanceUntilIdle()

        vm.onIntent(ProductContract.Intent.LoadProduct("CT-A-RoS"))
        advanceUntilIdle()

        val editor = vm.uiState.value.editor
        assertThat(editor.code).isEqualTo("CT-A-RoS")
        assertThat(editor.name).isEqualTo("Coturno cano alto rosa")
        assertThat(editor.imagePath).isEqualTo("/data/product_images/ct_a_ros_1.jpg")
    }

    @Test
    fun `loading an unknown code leaves the editor untouched`() = runTest {
        val vm = viewModel(FakeProductRepository(listOf(coturno)))
        advanceUntilIdle()

        vm.onIntent(ProductContract.Intent.LoadProduct("NOPE"))
        advanceUntilIdle()

        assertThat(vm.uiState.value.editor.code).isEmpty()
    }

    @Test
    fun `picking an image stores it and drops the one it replaced`() = runTest {
        val imageStore = FakeProductImageStore()
        val vm = viewModel(FakeProductRepository(listOf(coturno)), imageStore)
        advanceUntilIdle()
        vm.onIntent(ProductContract.Intent.LoadProduct("CT-A-RoS"))
        advanceUntilIdle()

        vm.onIntent(ProductContract.Intent.ImagePicked("content://pick/2"))
        advanceUntilIdle()

        assertThat(imageStore.savedFromUri).isEqualTo("content://pick/2")
        assertThat(imageStore.deleted).contains("ct_a_ros_1.jpg")
        assertThat(vm.uiState.value.editor.imageFileName).isNotEqualTo("ct_a_ros_1.jpg")
    }

    @Test
    fun `a failed image import reports it instead of looking like nothing happened`() = runTest {
        val imageStore = FakeProductImageStore(failOnSave = IllegalStateException("no such file"))
        val vm = viewModel(imageStore = imageStore)
        advanceUntilIdle()

        vm.effect.test {
            vm.onIntent(ProductContract.Intent.ImagePicked("content://pick/1"))
            advanceUntilIdle()

            assertThat(awaitItem()).isInstanceOf(ProductContract.Effect.ShowSnackbar::class.java)
        }
    }

    @Test
    fun `deleting a product removes it and its image`() = runTest {
        val imageStore = FakeProductImageStore()
        val repository = FakeProductRepository(listOf(coturno))
        val vm = viewModel(repository, imageStore)
        advanceUntilIdle()

        vm.onIntent(ProductContract.Intent.DeleteProduct("CT-A-RoS"))
        advanceUntilIdle()

        assertThat(repository.deleted).containsExactly(coturno)
        assertThat(imageStore.deleted).contains("ct_a_ros_1.jpg")
    }

    @Test
    fun `deleting an unknown code does nothing`() = runTest {
        val repository = FakeProductRepository(listOf(coturno))
        val vm = viewModel(repository)
        advanceUntilIdle()

        vm.onIntent(ProductContract.Intent.DeleteProduct("NOPE"))
        advanceUntilIdle()

        assertThat(repository.deleted).isEmpty()
    }

    @Test
    fun `selection toggles and clears`() = runTest {
        val vm = viewModel(FakeProductRepository(listOf(coturno, cafe)))
        advanceUntilIdle()

        vm.onIntent(ProductContract.Intent.ToggleProductSelection("CT-A-RoS"))
        assertThat(vm.uiState.value.selectedProductCodes).containsExactly("CT-A-RoS")

        vm.onIntent(ProductContract.Intent.ToggleProductSelection("CT-A-RoS"))
        assertThat(vm.uiState.value.selectedProductCodes).isEmpty()

        vm.onIntent(ProductContract.Intent.ToggleProductSelection("CF-200"))
        vm.onIntent(ProductContract.Intent.ClearSelection)
        assertThat(vm.uiState.value.selectedProductCodes).isEmpty()
    }

    @Test
    fun `printing everything asks for one label per product`() = runTest {
        val vm = viewModel(FakeProductRepository(listOf(coturno, cafe)))
        advanceUntilIdle()

        vm.effect.test {
            vm.onIntent(ProductContract.Intent.PrintAllQrCodes)
            advanceUntilIdle()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(ProductContract.Effect.PrintLabels::class.java)
            assertThat((effect as ProductContract.Effect.PrintLabels).labels).hasSize(2)
            assertThat(effect.requestedColumns).isEqualTo(4)
        }
    }

    @Test
    fun `printing a selection asks only for what is selected`() = runTest {
        val vm = viewModel(FakeProductRepository(listOf(coturno, cafe)))
        advanceUntilIdle()
        vm.onIntent(ProductContract.Intent.ToggleProductSelection("CF-200"))

        vm.effect.test {
            vm.onIntent(ProductContract.Intent.PrintSelectedQrCodes)
            advanceUntilIdle()

            val effect = awaitItem() as ProductContract.Effect.PrintLabels
            assertThat(effect.labels.single().productName).isEqualTo("Cafe Premium 200ml")
        }
    }

    @Test
    fun `printing nothing warns instead of sending an empty document`() = runTest {
        val vm = viewModel(FakeProductRepository())
        advanceUntilIdle()

        vm.effect.test {
            vm.onIntent(ProductContract.Intent.PrintAllQrCodes)
            advanceUntilIdle()

            assertThat(awaitItem()).isInstanceOf(ProductContract.Effect.ShowSnackbar::class.java)
        }
    }

    @Test
    fun `the editor label carries the qr payload the product will print`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onIntent(ProductContract.Intent.CodeChanged("CF-200"))
        vm.onIntent(ProductContract.Intent.NameChanged("Cafe Premium 200ml"))
        vm.onIntent(ProductContract.Intent.PriceChanged("12,50"))

        assertThat(vm.uiState.value.editor.label?.qrData)
            .isEqualTo("bipsale://product?code=CF-200&price=12.5")
    }

    @Test
    fun `an incomplete editor has no label and cannot be saved`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onIntent(ProductContract.Intent.CodeChanged("CF-200"))

        assertThat(vm.uiState.value.editor.label).isNull()
        assertThat(vm.uiState.value.editor.canSave).isFalse()
    }

    @Test
    fun `the label preview opens and closes`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onIntent(ProductContract.Intent.ShowLabelPreview)
        assertThat(vm.uiState.value.editor.isLabelPreviewVisible).isTrue()

        vm.onIntent(ProductContract.Intent.HideLabelPreview)
        assertThat(vm.uiState.value.editor.isLabelPreviewVisible).isFalse()
    }

    @Test
    fun `asking for the template asks for a destination before writing anything`() = runTest {
        val importRepository = FakeProductImportRepository()
        val vm = viewModel(importRepository = importRepository)
        advanceUntilIdle()

        vm.effect.test {
            vm.onIntent(ProductContract.Intent.DownloadTemplateRequested)
            advanceUntilIdle()

            val effect = awaitItem()
            assertThat(effect)
                .isInstanceOf(ProductContract.Effect.PickTemplateDestination::class.java)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(importRepository.writtenTemplateTo).isNull()
    }

    @Test
    fun `a chosen destination is where the template is written`() = runTest {
        val importRepository = FakeProductImportRepository()
        val vm = viewModel(importRepository = importRepository)
        advanceUntilIdle()

        vm.effect.test {
            vm.onIntent(
                ProductContract.Intent.TemplateDestinationChosen("content://docs/modelo.xlsx")
            )
            advanceUntilIdle()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(importRepository.writtenTemplateTo).isEqualTo("content://docs/modelo.xlsx")
    }

    @Test
    fun `an import registers every accepted row`() = runTest {
        val repository = FakeProductRepository()
        val vm = viewModel(
            repository = repository,
            importRepository = FakeProductImportRepository(
                ProductImportReport(
                    accepted = listOf(
                        ImportedProduct("CF-200", "Cafe Premium 200ml", 12.50, 8),
                        ImportedProduct("PR-100", "Prancheta oficio", 100.0, 3)
                    )
                )
            )
        )
        advanceUntilIdle()

        vm.effect.test {
            vm.onIntent(ProductContract.Intent.ImportSourceChosen("content://docs/produtos.xlsx"))
            advanceUntilIdle()
            assertThat(awaitItem()).isInstanceOf(ProductContract.Effect.ShowSnackbar::class.java)
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(vm.uiState.value.products.map { it.code })
            .containsExactly("CF-200", "PR-100")
        assertThat(vm.uiState.value.products.first { it.code == "CF-200" }.quantity).isEqualTo(8)
    }

    @Test
    fun `an import with rejected rows still registers the good ones`() = runTest {
        val repository = FakeProductRepository()
        val vm = viewModel(
            repository = repository,
            importRepository = FakeProductImportRepository(
                ProductImportReport(
                    accepted = listOf(ImportedProduct("CF-200", "Cafe", 12.50, 1)),
                    rejected = listOf(
                        ProductImportRejection(
                            3,
                            "PR-100",
                            ProductImportRejection.Reason.INVALID_PRICE
                        )
                    )
                )
            )
        )
        advanceUntilIdle()

        vm.effect.test {
            vm.onIntent(ProductContract.Intent.ImportSourceChosen("content://docs/produtos.xlsx"))
            advanceUntilIdle()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(vm.uiState.value.products.map { it.code }).containsExactly("CF-200")
    }

    @Test
    fun `a sheet with nothing usable reports it rather than claiming a success`() = runTest {
        val repository = FakeProductRepository()
        val vm = viewModel(
            repository = repository,
            importRepository = FakeProductImportRepository(ProductImportReport())
        )
        advanceUntilIdle()

        vm.effect.test {
            vm.onIntent(ProductContract.Intent.ImportSourceChosen("content://docs/produtos.xlsx"))
            advanceUntilIdle()
            assertThat(awaitItem()).isInstanceOf(ProductContract.Effect.ShowSnackbar::class.java)
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(vm.uiState.value.products).isEmpty()
    }

    @Test
    fun `stock reaches the list and drives how many labels a print run makes`() = runTest {
        val vm = viewModel(FakeProductRepository(listOf(coturno.copy(quantity = 3))))
        advanceUntilIdle()

        assertThat(vm.uiState.value.products.single().quantity).isEqualTo(3)
        assertThat(vm.uiState.value.allLabels).hasSize(3)
    }

    @Test
    fun `a product with no stock still gets one label rather than none`() = runTest {
        val vm = viewModel(FakeProductRepository(listOf(coturno.copy(quantity = 0))))
        advanceUntilIdle()

        assertThat(vm.uiState.value.allLabels).hasSize(1)
    }
}
