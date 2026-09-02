package dev.diegoflassa.bipsale.feature.sales

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.domain.model.ItemDiscount
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import dev.diegoflassa.bipsale.core.ui.R as CoreUiR

/**
 * Drives the stateless `SalesScreenContent` directly — no Hilt, no ViewModel. Every case is one
 * `State` value in and one callback assertion out.
 *
 * Expected copy is read from resources rather than typed in, so the suite passes in any locale the
 * device happens to be set to.
 */
@RunWith(AndroidJUnit4::class)
class SalesScreenContentInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun string(@StringRes id: Int, vararg args: Any): String =
        if (args.isEmpty()) context.getString(id) else context.getString(id, *args)

    private val cafe = SaleItem(
        id = "line-1",
        saleId = "",
        productCode = "CF-200",
        productName = "Cafe Premium 200ml",
        unitPrice = 12.50,
        quantity = 2
    )

    private val coturno = SaleItem(
        id = "line-2",
        saleId = "",
        productCode = "CT-A-RoS",
        productName = "Coturno cano alto rosa",
        unitPrice = 130.0,
        quantity = 1,
        discount = ItemDiscount.Percentage(10.0)
    )

    private class Recorder {
        var back = 0
        var scan = 0
        var pickFromCatalog = 0
        var typeCode = 0
        var saleDiscount = 0
        var finalize = 0
        val removed = mutableListOf<String>()
        val discounted = mutableListOf<String>()
        val paymentMethods = mutableListOf<PaymentMethod>()
        val detailed = mutableListOf<String>()
        val pixAmountToggles = mutableListOf<Boolean>()
    }

    private fun render(state: SalesContract.State): Recorder {
        val recorder = Recorder()
        composeRule.setContent {
            BipSaleTheme {
                SalesScreenContent(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    onBack = { recorder.back++ },
                    onScanClick = { recorder.scan++ },
                    onPickFromCatalog = { recorder.pickFromCatalog++ },
                    onTypeCode = { recorder.typeCode++ },
                    onRemoveItem = { recorder.removed += it },
                    onDiscountItem = { recorder.discounted += it },
                    onSaleDiscountClick = { recorder.saleDiscount++ },
                    onPaymentMethodChange = { recorder.paymentMethods += it },
                    onShowDetail = { recorder.detailed += it },
                    onTogglePixAmount = { recorder.pixAmountToggles += it },
                    onFinalize = { recorder.finalize++ }
                )
            }
        }
        return recorder
    }

    @Test
    fun anEmptyCartShowsItsEmptyMessage() {
        render(SalesContract.State(customerName = "Ana Paula"))

        composeRule.onNodeWithTag(SalesScreenTestTags.EMPTY_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun anEmptyCartCannotBeFinalized() {
        render(SalesContract.State())

        composeRule.onNodeWithTag(SalesScreenTestTags.FINALIZE_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun aCartWithItemsAndAPaymentMethodCanBeFinalized() {
        render(
            SalesContract.State(
                items = listOf(cafe),
                totalAmount = 25.0,
                finalAmount = 25.0,
                paymentMethod = PaymentMethod.CASH
            )
        )

        composeRule.onNodeWithTag(SalesScreenTestTags.FINALIZE_BUTTON).assertIsEnabled()
    }

    @Test
    fun aCartWithNoPaymentMethodCannotBeFinalized() {
        render(SalesContract.State(items = listOf(cafe), totalAmount = 25.0, finalAmount = 25.0))

        composeRule.onNodeWithTag(SalesScreenTestTags.FINALIZE_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun aFinalizeInFlightDisablesTheButtonSoASaleCannotBeRungUpTwice() {
        render(
            SalesContract.State(
                items = listOf(cafe),
                totalAmount = 25.0,
                finalAmount = 25.0,
                isFinalizing = true
            )
        )

        composeRule.onNodeWithTag(SalesScreenTestTags.FINALIZE_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun theCartListsItsLines() {
        render(SalesContract.State(items = listOf(cafe, coturno)))

        composeRule.onNodeWithTag(SalesScreenTestTags.ITEM_LIST).assertIsDisplayed()
        composeRule.onNodeWithText("Cafe Premium 200ml").assertIsDisplayed()
        composeRule.onNodeWithText("Coturno cano alto rosa").assertIsDisplayed()
    }

    @Test
    fun theCustomerNameIsInTheHeader() {
        render(SalesContract.State(customerName = "Ana Paula Nogueira"))

        composeRule
            .onNodeWithText(string(R.string.sale_title_format, "Ana Paula Nogueira"))
            .assertIsDisplayed()
    }

    @Test
    fun anAnonymousSaleSaysSoInTheHeader() {
        render(SalesContract.State(isAnonymous = true, customerName = "Ana Paula"))

        composeRule.onNodeWithText(string(R.string.sale_title_anonymous)).assertIsDisplayed()
    }

    @Test
    fun theHeaderCarriesABackControl() {
        val recorder = render(SalesContract.State())

        composeRule
            .onNodeWithContentDescription(string(CoreUiR.string.common_back))
            .performClick()

        assertThat(recorder.back).isEqualTo(1)
    }

    @Test
    fun theScanActionReportsItsClick() {
        val recorder = render(SalesContract.State())

        composeRule.onNodeWithTag(SalesScreenTestTags.SCAN_BUTTON).performClick()

        assertThat(recorder.scan).isEqualTo(1)
    }

    @Test
    fun theAddMenuOffersTheCatalogue() {
        val recorder = render(SalesContract.State())

        composeRule.onNodeWithTag(SalesScreenTestTags.ADD_PRODUCT_BUTTON).performClick()
        composeRule.onNodeWithText(string(R.string.sales_pick_from_catalog)).performClick()

        assertThat(recorder.pickFromCatalog).isEqualTo(1)
    }

    @Test
    fun theAddMenuOffersAManualCode() {
        val recorder = render(SalesContract.State())

        composeRule.onNodeWithTag(SalesScreenTestTags.ADD_PRODUCT_BUTTON).performClick()
        composeRule.onNodeWithText(string(R.string.sales_type_code)).performClick()

        assertThat(recorder.typeCode).isEqualTo(1)
    }

    @Test
    fun theDiscountControlReportsItsClick() {
        val recorder = render(SalesContract.State(items = listOf(cafe)))

        composeRule.onNodeWithTag(SalesScreenTestTags.DISCOUNT_BUTTON).performClick()

        assertThat(recorder.saleDiscount).isEqualTo(1)
    }

    @Test
    fun removingALineReportsThatLinesOwnId() {
        // Two lines of the same product are indistinguishable by value; the id is what makes
        // "delete the second one" mean anything.
        val recorder = render(SalesContract.State(items = listOf(cafe, coturno)))

        composeRule
            .onAllNodesWithContentDescription(string(R.string.remove_item_description))[1]
            .performClick()

        assertThat(recorder.removed).containsExactly("line-2")
    }

    @Test
    fun discountingALineReportsThatLinesOwnId() {
        val recorder = render(SalesContract.State(items = listOf(cafe, coturno)))

        composeRule
            .onAllNodesWithContentDescription(string(R.string.sales_item_discount_action))[0]
            .performClick()

        assertThat(recorder.discounted).containsExactly("line-1")
    }

    @Test
    fun finalizingReportsItsClick() {
        val recorder = render(
            SalesContract.State(
                items = listOf(cafe),
                totalAmount = 25.0,
                finalAmount = 25.0,
                paymentMethod = PaymentMethod.CASH
            )
        )

        composeRule.onNodeWithTag(SalesScreenTestTags.FINALIZE_BUTTON).performClick()

        assertThat(recorder.finalize).isEqualTo(1)
    }

    @Test
    fun theBottomBarShowsTheSubtotalAndTheAmountCharged() {
        render(
            SalesContract.State(
                items = listOf(cafe, coturno),
                totalAmount = 155.0,
                itemDiscountAmount = 13.0,
                discountPercentage = 10.0,
                finalAmount = 127.80
            )
        )

        composeRule.onNodeWithTag(SalesScreenTestTags.SUBTOTAL_VALUE)
            .assertTextEquals(string(R.string.currency_format, 155.0))
        composeRule.onNodeWithTag(SalesScreenTestTags.TOTAL_VALUE)
            .assertTextEquals(string(R.string.currency_format, 127.80))
    }

    @Test
    fun theItemDiscountRowIsHiddenWhileNoLineIsDiscounted() {
        render(SalesContract.State(items = listOf(cafe), totalAmount = 25.0, finalAmount = 25.0))

        composeRule
            .onNodeWithText(string(R.string.sales_item_discounts_label))
            .assertDoesNotExist()
    }

    @Test
    fun theItemDiscountRowAppearsOnceALineIsDiscounted() {
        render(
            SalesContract.State(
                items = listOf(coturno),
                totalAmount = 130.0,
                itemDiscountAmount = 13.0,
                finalAmount = 117.0
            )
        )

        composeRule
            .onNodeWithText(string(R.string.sales_item_discounts_label))
            .assertIsDisplayed()
    }

    @Test
    fun aDiscountedLineShowsBothTheOriginalAndTheChargedAmount() {
        render(SalesContract.State(items = listOf(coturno)))

        composeRule.onNodeWithText(string(R.string.currency_format, 130.0)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.currency_format, 117.0)).assertIsDisplayed()
    }
}
