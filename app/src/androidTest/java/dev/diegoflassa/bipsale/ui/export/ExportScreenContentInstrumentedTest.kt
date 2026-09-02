package dev.diegoflassa.bipsale.ui.export

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportScreenContentInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        saleCount: Int = 42,
        isExporting: Boolean = false,
        onExport: () -> Unit = {},
        onBack: () -> Unit = {}
    ) {
        composeRule.setContent {
            BipSaleTheme {
                ExportScreenContent(
                    saleCount = saleCount,
                    isExporting = isExporting,
                    onBack = onBack,
                    onExport = onExport,
                    snackbarHostState = SnackbarHostState()
                )
            }
        }
    }

    @Test
    fun theExportButtonIsShownOnAnIdleScreen() {
        setContent()

        composeRule.onNodeWithTag(ExportScreenTestTags.GENERATE_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(ExportScreenTestTags.SALE_COUNT).assertIsDisplayed()
    }

    @Test
    fun tappingExportInvokesTheCallbackOnce() {
        var exports = 0
        setContent(onExport = { exports++ })

        composeRule.onNodeWithTag(ExportScreenTestTags.GENERATE_BUTTON).performClick()

        assertThat(exports).isEqualTo(1)
    }

    @Test
    fun theExportButtonIsDisabledWhileAnExportIsRunning() {
        // An export that can be started twice writes the second file over the first, and the
        // operator only ever sees one of them.
        setContent(isExporting = true)

        composeRule.onNodeWithTag(ExportScreenTestTags.GENERATE_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun theExportButtonIsEnabledOnceTheExportFinishes() {
        setContent(isExporting = false)

        composeRule.onNodeWithTag(ExportScreenTestTags.GENERATE_BUTTON).assertIsEnabled()
    }

    @Test
    fun aDisabledExportButtonDoesNotInvokeTheCallback() {
        var exports = 0
        setContent(isExporting = true, onExport = { exports++ })

        composeRule.onNodeWithTag(ExportScreenTestTags.GENERATE_BUTTON).performClick()

        assertThat(exports).isEqualTo(0)
    }

    @Test
    fun theSaleCountIsRenderedForAnEmptyHistoryToo() {
        setContent(saleCount = 0)

        composeRule.onNodeWithTag(ExportScreenTestTags.SALE_COUNT).assertIsDisplayed()
    }
}
