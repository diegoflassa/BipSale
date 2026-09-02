package dev.diegoflassa.bipsale.feature.products

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.qrcode.LabelData
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import dev.diegoflassa.bipsale.core.ui.util.UiText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import dev.diegoflassa.bipsale.core.ui.R as CoreUiR

@RunWith(AndroidJUnit4::class)
class ProductListContentInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun string(@StringRes id: Int): String = context.getString(id)

    private fun product(code: String, name: String, price: String, quantity: Int = 3) =
        ProductContract.ProductUiModel(
            code = code,
            name = name,
            priceFormatted = price,
            imagePath = null,
            label = LabelData("bipsale://product?code=$code&price=1.0", name, price),
            quantity = quantity
        )

    private val coturno = product("CT-A-RoS", "Coturno cano alto rosa", "R$ 130,00")
    private val cafe = product("CF-200", "Cafe Premium 200ml", "R$ 12,50")

    private class Recorder {
        var add = 0
        var back = 0
        val edited = mutableListOf<String>()
        val intents = mutableListOf<ProductContract.Intent>()
    }

    private fun render(state: ProductContract.State): Recorder {
        val recorder = Recorder()
        composeRule.setContent {
            BipSaleTheme {
                ProductListContent(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    onAddProduct = { recorder.add++ },
                    onEditProduct = { recorder.edited += it },
                    onBack = { recorder.back++ },
                    onIntent = { recorder.intents += it }
                )
            }
        }
        return recorder
    }

    @Test
    fun theLoadingStateShowsASpinner() {
        render(ProductContract.State(isLoading = true))

        composeRule.onNodeWithTag(ProductListScreenTestTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun anEmptyCatalogueSaysSo() {
        render(ProductContract.State(isLoading = false))

        composeRule.onNodeWithTag(ProductListScreenTestTags.EMPTY).assertIsDisplayed()
    }

    @Test
    fun anErrorIsShown() {
        render(
            ProductContract.State(
                isLoading = false,
                errorMessage = UiText.DynamicString("Falha ao carregar")
            )
        )

        composeRule.onNodeWithTag(ProductListScreenTestTags.ERROR).assertIsDisplayed()
    }

    @Test
    fun productsAreListed() {
        render(ProductContract.State(isLoading = false, products = listOf(coturno, cafe)))

        composeRule.onNodeWithTag(ProductListScreenTestTags.LIST).assertIsDisplayed()
        composeRule.onNodeWithText("Coturno cano alto rosa").assertIsDisplayed()
        composeRule.onNodeWithText("Cafe Premium 200ml").assertIsDisplayed()
    }

    @Test
    fun theHeaderCarriesABackControl() {
        val recorder = render(ProductContract.State(isLoading = false, products = listOf(coturno)))

        composeRule
            .onNodeWithContentDescription(string(CoreUiR.string.common_back))
            .performClick()

        assertThat(recorder.back).isEqualTo(1)
    }

    @Test
    fun theAddButtonReportsItsClick() {
        val recorder = render(ProductContract.State(isLoading = false, products = listOf(coturno)))

        composeRule.onNodeWithTag(ProductListScreenTestTags.ADD_BUTTON).performClick()

        assertThat(recorder.add).isEqualTo(1)
    }

    @Test
    fun tappingARowOpensThatProduct() {
        val recorder = render(ProductContract.State(isLoading = false, products = listOf(coturno, cafe)))

        composeRule.onNodeWithTag(ProductListScreenTestTags.productRow("CF-200")).performClick()

        assertThat(recorder.edited).containsExactly("CF-200")
    }

    @Test
    fun deletingARowEmitsThatProductsDelete() {
        val recorder = render(ProductContract.State(isLoading = false, products = listOf(coturno, cafe)))

        composeRule.onNodeWithTag(ProductListScreenTestTags.deleteButton("CF-200")).performClick()

        assertThat(recorder.intents).contains(ProductContract.Intent.DeleteProduct("CF-200"))
    }

    @Test
    fun longPressingARowStartsSelection() {
        val recorder = render(ProductContract.State(isLoading = false, products = listOf(coturno)))

        composeRule
            .onNodeWithTag(ProductListScreenTestTags.productRow("CT-A-RoS"))
            .performTouchInput { longClick() }

        assertThat(recorder.intents)
            .contains(ProductContract.Intent.ToggleProductSelection("CT-A-RoS"))
    }

    @Test
    fun theCheckboxOnASelectedRowDeselectsIt() {
        // The checkbox only exists once the row is selected; before that the slot holds delete.
        val recorder = render(
            ProductContract.State(
                isLoading = false,
                products = listOf(coturno),
                selectedProductCodes = setOf("CT-A-RoS")
            )
        )

        composeRule
            .onNodeWithTag(ProductListScreenTestTags.productCheckbox("CT-A-RoS"))
            .performClick()

        assertThat(recorder.intents)
            .contains(ProductContract.Intent.ToggleProductSelection("CT-A-RoS"))
    }

    @Test
    fun tappingARowWhileSelectingTogglesItRatherThanOpeningIt() {
        val recorder = render(
            ProductContract.State(
                isLoading = false,
                products = listOf(coturno, cafe),
                selectedProductCodes = setOf("CT-A-RoS")
            )
        )

        composeRule.onNodeWithTag(ProductListScreenTestTags.productRow("CF-200")).performClick()

        assertThat(recorder.intents)
            .contains(ProductContract.Intent.ToggleProductSelection("CF-200"))
        assertThat(recorder.edited).isEmpty()
    }

    @Test
    fun selectionModeReplacesTheBackArrowWithAClearControl() {
        val recorder = render(
            ProductContract.State(
                isLoading = false,
                products = listOf(coturno, cafe),
                selectedProductCodes = setOf("CT-A-RoS")
            )
        )

        composeRule.onNodeWithTag(ProductListScreenTestTags.CLEAR_SELECTION_BUTTON).performClick()

        assertThat(recorder.intents).contains(ProductContract.Intent.ClearSelection)
        assertThat(recorder.back).isEqualTo(0)
    }

    @Test
    fun selectionModeOffersPrintingJustTheSelection() {
        val recorder = render(
            ProductContract.State(
                isLoading = false,
                products = listOf(coturno, cafe),
                selectedProductCodes = setOf("CT-A-RoS")
            )
        )

        composeRule.onNodeWithTag(ProductListScreenTestTags.PRINT_SELECTED_BUTTON).performClick()

        assertThat(recorder.intents).contains(ProductContract.Intent.PrintSelectedQrCodes)
    }

    @Test
    fun printingEverythingIsOfferedWhenNothingIsSelected() {
        val recorder = render(ProductContract.State(isLoading = false, products = listOf(coturno)))

        composeRule.onNodeWithTag(ProductListScreenTestTags.PRINT_ALL_BUTTON).performClick()

        assertThat(recorder.intents).contains(ProductContract.Intent.PrintAllQrCodes)
    }

    @Test
    fun noOverwriteWarningIsShownUntilAnImportIsAskedFor() {
        render(ProductContract.State(isLoading = false, products = listOf(coturno)))

        composeRule.onNodeWithTag(ProductListScreenTestTags.IMPORT_CONFIRM_DIALOG)
            .assertDoesNotExist()
    }

    @Test
    fun theOverwriteWarningIsShownBeforeTheFilePicker() {
        render(
            ProductContract.State(
                isLoading = false,
                products = listOf(coturno),
                isImportConfirmVisible = true
            )
        )

        composeRule.onNodeWithTag(ProductListScreenTestTags.IMPORT_CONFIRM_DIALOG)
            .assertIsDisplayed()
    }

    @Test
    fun confirmingTheWarningIsWhatRequestsTheFile() {
        val recorder = render(
            ProductContract.State(isLoading = false, isImportConfirmVisible = true)
        )

        composeRule.onNodeWithTag(ProductListScreenTestTags.IMPORT_CONFIRM_ACCEPT).performClick()

        assertThat(recorder.intents).contains(ProductContract.Intent.ImportConfirmed)
    }

    @Test
    fun cancellingTheWarningImportsNothing() {
        val recorder = render(
            ProductContract.State(isLoading = false, isImportConfirmVisible = true)
        )

        composeRule.onNodeWithTag(ProductListScreenTestTags.IMPORT_CONFIRM_CANCEL).performClick()

        assertThat(recorder.intents).contains(ProductContract.Intent.ImportDismissed)
        assertThat(recorder.intents).doesNotContain(ProductContract.Intent.ImportConfirmed)
    }
}
