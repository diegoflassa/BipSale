package dev.diegoflassa.bipsale.feature.history

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the stateless `HistoryScreenContent` directly — no Hilt, no ViewModel. Every case is one
 * `State` value in and one callback assertion out.
 *
 * Expected copy is read from resources rather than typed in, so the suite passes in any locale the
 * device happens to be set to.
 */
@RunWith(AndroidJUnit4::class)
class HistoryScreenContentInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun string(@StringRes id: Int, vararg args: Any): String =
        if (args.isEmpty()) context.getString(id) else context.getString(id, *args)

    private val saleNamed = Sale(
        id = "sale-1",
        customerName = "Maria Aparecida Souza",
        customerCpf = "123.456.789-00",
        totalAmount = 89.90,
        discountPercentage = 0.0,
        finalAmount = 89.90,
        paymentMethod = PaymentMethod.PIX,
        date = 1752000000000L,
    )

    private val saleAnonymous = Sale(
        id = "sale-2",
        customerName = "",
        customerCpf = "",
        totalAmount = 45.00,
        discountPercentage = 10.0,
        finalAmount = 40.50,
        paymentMethod = PaymentMethod.CASH,
        date = 1752003600000L,
    )

    private class Recorder {
        var back = 0
        val clicked = mutableListOf<String>()
        var exportSelected = 0
        val intents = mutableListOf<HistoryContract.Intent>()
    }

    private fun render(state: HistoryContract.State): Recorder {
        val recorder = Recorder()
        composeRule.setContent {
            BipSaleTheme {
                HistoryScreenContent(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    onBack = { recorder.back++ },
                    onSaleClick = { recorder.clicked += it },
                    onExportSelected = { recorder.exportSelected++ },
                    onIntent = { recorder.intents += it }
                )
            }
        }
        return recorder
    }

    @Test
    fun anEmptyListShowsTheEmptyMessage() {
        render(HistoryContract.State())

        composeRule.onNodeWithTag(HistoryScreenTestTags.EMPTY).assertIsDisplayed()
    }

    @Test
    fun aPopulatedListRendersEachRow() {
        render(HistoryContract.State(sales = listOf(saleNamed, saleAnonymous)))

        composeRule.onNodeWithTag(HistoryScreenTestTags.LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(HistoryScreenTestTags.saleRow("sale-1")).assertIsDisplayed()
        composeRule.onNodeWithTag(HistoryScreenTestTags.saleRow("sale-2")).assertIsDisplayed()
    }

    @Test
    fun theSearchFieldIsDisplayedAndAcceptsInput() {
        val recorder = render(HistoryContract.State(sales = listOf(saleNamed)))

        composeRule.onNodeWithTag(HistoryScreenTestTags.SEARCH_FIELD)
            .assertIsDisplayed()
            .performTextInput("Maria")

        assertThat(recorder.intents.filterIsInstance<HistoryContract.Intent.SearchSales>())
            .isNotEmpty()
    }

    @Test
    fun tappingARowNavigatesToItsDetail() {
        val recorder = render(HistoryContract.State(sales = listOf(saleNamed, saleAnonymous)))

        composeRule.onNodeWithTag(HistoryScreenTestTags.saleRow("sale-1")).performClick()

        assertThat(recorder.clicked).containsExactly("sale-1")
    }

    @Test
    fun longPressingARowEntersSelectionMode() {
        val recorder = render(HistoryContract.State(sales = listOf(saleNamed, saleAnonymous)))

        composeRule.onNodeWithTag(HistoryScreenTestTags.saleRow("sale-1"))
            .performTouchInput { longClick() }

        assertThat(recorder.intents.filterIsInstance<HistoryContract.Intent.ToggleSaleSelection>())
            .hasSize(1)
    }

    @Test
    fun theExportButtonAppearsOnlyInSelectionMode() {
        render(HistoryContract.State(sales = listOf(saleNamed)))

        composeRule.onNodeWithTag(HistoryScreenTestTags.EXPORT_SELECTED_BUTTON)
            .assertDoesNotExist()
    }

    @Test
    fun theExportButtonIsVisibleWhenARowIsSelected() {
        val recorder = render(
            HistoryContract.State(
                sales = listOf(saleNamed, saleAnonymous),
                selectedSaleIds = setOf("sale-1")
            )
        )

        composeRule.onNodeWithTag(HistoryScreenTestTags.EXPORT_SELECTED_BUTTON)
            .assertIsDisplayed()
            .performClick()

        assertThat(recorder.exportSelected).isEqualTo(1)
    }

    @Test
    fun clearingSelectionReportsTheIntent() {
        val recorder = render(
            HistoryContract.State(
                sales = listOf(saleNamed),
                selectedSaleIds = setOf("sale-1")
            )
        )

        composeRule.onNodeWithTag(HistoryScreenTestTags.CLEAR_SELECTION_BUTTON).performClick()

        assertThat(recorder.intents.filterIsInstance<HistoryContract.Intent.ClearSelection>())
            .hasSize(1)
    }

    @Test
    fun theLoadingIndicatorShowsWhileLoading() {
        render(HistoryContract.State(isLoading = true))

        composeRule.onNodeWithTag(HistoryScreenTestTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun theLoadingIndicatorShowsWhileExporting() {
        render(
            HistoryContract.State(
                sales = listOf(saleNamed),
                isExporting = true
            )
        )

        composeRule.onNodeWithTag(HistoryScreenTestTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun aNamedSaleShowsTheCustomerName() {
        render(HistoryContract.State(sales = listOf(saleNamed)))

        composeRule.onNodeWithText("Maria Aparecida Souza").assertIsDisplayed()
    }

    @Test
    fun anAnonymousSaleShowsThePlaceholderLabel() {
        render(HistoryContract.State(sales = listOf(saleAnonymous)))

        composeRule.onNodeWithText(string(R.string.history_anonymous_customer))
            .assertIsDisplayed()
    }

    @Test
    fun theSelectionCountAppearsInTheHeaderDuringSelection() {
        render(
            HistoryContract.State(
                sales = listOf(saleNamed, saleAnonymous),
                selectedSaleIds = setOf("sale-1")
            )
        )

        composeRule.onNodeWithText(string(R.string.history_selected_count, 1))
            .assertIsDisplayed()
    }
}
