package dev.diegoflassa.bipsale.ui.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.domain.settings.AppSettings
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenContentInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<SettingsContract.Intent>()

    private fun setContent(state: SettingsContract.State) {
        composeRule.setContent {
            BipSaleTheme {
                SettingsScreenContent(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    onBack = {},
                    onIntent = { intents += it }
                )
            }
        }
    }

    private fun loaded(
        namePt: Int = AppSettings.DEFAULT_QR_LABEL_NAME_TEXT_SIZE_PT,
        pricePt: Int = AppSettings.DEFAULT_QR_LABEL_PRICE_TEXT_SIZE_PT
    ) = SettingsContract.State(
        isLoading = false,
        qrLabelNameTextSizePt = namePt,
        qrLabelPriceTextSizePt = pricePt
    )

    @Test
    fun bothTypeSizesAreShown() {
        setContent(loaded())

        composeRule.onNodeWithTag(SettingsScreenTestTags.NAME_TEXT_SIZE_VALUE)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsScreenTestTags.PRICE_TEXT_SIZE_VALUE)
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theNameStepperChangesOnlyTheNameSize() {
        // The two steppers are the same composable wired to different intents; swapping the two
        // lambdas looks identical on screen.
        setContent(loaded())

        composeRule.onNodeWithTag(SettingsScreenTestTags.NAME_TEXT_SIZE_INCREASE)
            .performScrollTo().performClick()

        assertThat(intents).containsExactly(
            SettingsContract.Intent.QrLabelNameTextSizeChanged(
                AppSettings.DEFAULT_QR_LABEL_NAME_TEXT_SIZE_PT + 1
            )
        )
    }

    @Test
    fun thePriceStepperChangesOnlyThePriceSize() {
        setContent(loaded())

        composeRule.onNodeWithTag(SettingsScreenTestTags.PRICE_TEXT_SIZE_DECREASE)
            .performScrollTo().performClick()

        assertThat(intents).containsExactly(
            SettingsContract.Intent.QrLabelPriceTextSizeChanged(
                AppSettings.DEFAULT_QR_LABEL_PRICE_TEXT_SIZE_PT - 1
            )
        )
    }

    @Test
    fun aSizeAtItsMaximumCannotBeIncreasedFurther() {
        setContent(loaded(namePt = AppSettings.MAX_QR_LABEL_TEXT_SIZE_PT))

        composeRule.onNodeWithTag(SettingsScreenTestTags.NAME_TEXT_SIZE_INCREASE)
            .performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun aSizeAtItsMinimumCannotBeDecreasedFurther() {
        setContent(loaded(pricePt = AppSettings.MIN_QR_LABEL_TEXT_SIZE_PT))

        composeRule.onNodeWithTag(SettingsScreenTestTags.PRICE_TEXT_SIZE_DECREASE)
            .performScrollTo().assertIsNotEnabled()
    }
}
