package dev.diegoflassa.bipsale.feature.products

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.qrcode.LabelData
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddEditProductContentInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun string(@StringRes id: Int): String = context.getString(id)

    private val filledEditor = ProductContract.Editor(
        code = "CT-A-RoS",
        name = "Coturno cano alto rosa",
        priceInput = "130,00",
        label = LabelData(
            qrData = "bipsale://product?code=CT-A-RoS&price=130.0",
            productName = "Coturno cano alto rosa",
            priceFormatted = "R$ 130,00"
        )
    )

    private fun render(
        editor: ProductContract.Editor,
        isEdit: Boolean = false
    ): Pair<MutableList<ProductContract.Intent>, IntArray> {
        val intents = mutableListOf<ProductContract.Intent>()
        val backCount = IntArray(1)
        composeRule.setContent {
            BipSaleTheme {
                AddEditProductContent(
                    editor = editor,
                    isEdit = isEdit,
                    snackbarHostState = SnackbarHostState(),
                    onBack = { backCount[0]++ },
                    onIntent = { intents += it }
                )
            }
        }
        return intents to backCount
    }

    @Test
    fun theNewProductTitleShowsWhenNotEditing() {
        render(ProductContract.Editor())

        composeRule.onNodeWithText(string(R.string.products_new_product)).assertIsDisplayed()
    }

    @Test
    fun theEditTitleShowsWhenEditing() {
        render(filledEditor, isEdit = true)

        composeRule.onNodeWithText(string(R.string.products_edit_product)).assertIsDisplayed()
    }

    @Test
    fun theHeaderCarriesABackControl() {
        val (_, back) = render(ProductContract.Editor())

        composeRule.onNodeWithTag(AddEditProductScreenTestTags.BACK_BUTTON).performClick()

        assertThat(back[0]).isEqualTo(1)
    }

    @Test
    fun anEmptyFormCannotBeSaved() {
        render(ProductContract.Editor())

        composeRule.onNodeWithTag(AddEditProductScreenTestTags.SAVE_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun aCompleteFormCanBeSaved() {
        render(filledEditor)

        composeRule.onNodeWithTag(AddEditProductScreenTestTags.SAVE_BUTTON).assertIsEnabled()
    }

    @Test
    fun aFormWithAnUnparseablePriceCannotBeSaved() {
        // No label means the price did not parse, and a product with no price must not persist.
        render(filledEditor.copy(priceInput = "de graca", label = null))

        composeRule.onNodeWithTag(AddEditProductScreenTestTags.SAVE_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun savingEmitsTheSaveIntent() {
        val (intents, _) = render(filledEditor)

        // The form scrolls; the button is below the fold on a phone once the label preview shows.
        composeRule.onNodeWithTag(AddEditProductScreenTestTags.SAVE_BUTTON)
            .performScrollTo()
            .performClick()

        assertThat(intents).contains(ProductContract.Intent.SaveProduct)
    }

    @Test
    fun typingACodeEmitsIt() {
        val (intents, _) = render(ProductContract.Editor())

        composeRule.onNodeWithTag(AddEditProductScreenTestTags.CODE_FIELD)
            .performTextReplacement("CF-200")

        assertThat(intents).contains(ProductContract.Intent.CodeChanged("CF-200"))
    }

    @Test
    fun typingANameEmitsIt() {
        val (intents, _) = render(ProductContract.Editor())

        composeRule.onNodeWithTag(AddEditProductScreenTestTags.NAME_FIELD)
            .performTextReplacement("Cafe Premium 200ml")

        assertThat(intents).contains(ProductContract.Intent.NameChanged("Cafe Premium 200ml"))
    }

    @Test
    fun typingAPriceEmitsIt() {
        val (intents, _) = render(ProductContract.Editor())

        composeRule.onNodeWithTag(AddEditProductScreenTestTags.PRICE_FIELD)
            .performTextReplacement("12,50")

        assertThat(intents).contains(ProductContract.Intent.PriceChanged("12,50"))
    }

    @Test
    fun theLabelPreviewOnlyAppearsOnceTheFormCanProduceOne() {
        render(ProductContract.Editor())

        composeRule.onNodeWithTag(AddEditProductScreenTestTags.LABEL_PREVIEW).assertDoesNotExist()
    }

    @Test
    fun theLabelPreviewAppearsForACompleteForm() {
        render(filledEditor)

        composeRule.onNodeWithTag(AddEditProductScreenTestTags.LABEL_PREVIEW).assertIsDisplayed()
    }

    @Test
    fun printingEmitsThePrintIntent() {
        val (intents, _) = render(filledEditor)

        composeRule.onNodeWithTag(AddEditProductScreenTestTags.PRINT_BUTTON)
            .performScrollTo()
            .performClick()

        assertThat(intents).contains(ProductContract.Intent.PrintEditorLabel)
    }

    @Test
    fun tappingTheLabelAsksForTheRealSizeView() {
        val (intents, _) = render(filledEditor)

        composeRule.onNodeWithTag(AddEditProductScreenTestTags.LABEL_PREVIEW)
            .performScrollTo()
            .performClick()

        assertThat(intents).contains(ProductContract.Intent.ShowLabelPreview)
    }

    @Test
    fun theRealSizeDialogShowsWhenAsked() {
        render(filledEditor.copy(isLabelPreviewVisible = true))

        composeRule.onNodeWithTag(AddEditProductScreenTestTags.REAL_SIZE_DIALOG).assertIsDisplayed()
    }

    @Test
    fun closingTheRealSizeDialogEmitsHide() {
        val (intents, _) = render(filledEditor.copy(isLabelPreviewVisible = true))

        composeRule.onNodeWithTag(AddEditProductScreenTestTags.REAL_SIZE_CLOSE).performClick()

        assertThat(intents).contains(ProductContract.Intent.HideLabelPreview)
    }

    @Test
    fun aSaveInFlightBlocksASecondSave() {
        render(filledEditor.copy(isSaving = true))

        composeRule.onNodeWithTag(AddEditProductScreenTestTags.SAVE_BUTTON).assertIsNotEnabled()
    }
}
