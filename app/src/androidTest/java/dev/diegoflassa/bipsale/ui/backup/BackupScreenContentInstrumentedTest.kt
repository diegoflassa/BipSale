package dev.diegoflassa.bipsale.ui.backup

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.domain.backup.BackupSummary
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupScreenContentInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val summary = BackupSummary(
        products = 42,
        sales = 318,
        saleItems = 927,
        images = 37,
        createdAt = 1_787_400_000_000
    )

    private fun setContent(state: BackupContract.State) {
        composeRule.setContent {
            BipSaleTheme {
                BackupScreenContent(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    onBack = {},
                    onIntent = {},
                    onPickDestination = {},
                    onPickSource = {}
                )
            }
        }
    }

    @Test
    fun theSummaryCardFillsTheContentWidth() {
        // The card used to wrap to its widest line, leaving it visibly narrower than the buttons
        // stacked above it. The create button is already full-width, so it is the reference.
        setContent(BackupContract.State(lastSummary = summary))

        val button = composeRule.onNodeWithTag(BackupScreenTestTags.CREATE_BUTTON)
            .getUnclippedBoundsInRoot()
        val card = composeRule.onNodeWithTag(BackupScreenTestTags.SUMMARY)
            .getUnclippedBoundsInRoot()

        val buttonWidth = (button.right - button.left).value
        val cardWidth = (card.right - card.left).value
        assertThat(cardWidth).isWithin(TOLERANCE_DP).of(buttonWidth)
    }

    @Test
    fun theSummaryCardIsShownOnceABackupHasRun() {
        setContent(BackupContract.State(lastSummary = summary))

        composeRule.onNodeWithTag(BackupScreenTestTags.SUMMARY).assertIsDisplayed()
    }

    @Test
    fun noSummaryIsShownBeforeAnythingHasRun() {
        setContent(BackupContract.State())

        composeRule.onNodeWithTag(BackupScreenTestTags.SUMMARY).assertDoesNotExist()
    }

    @Test
    fun theSpinnerShowsWhileABackupIsRunning() {
        setContent(BackupContract.State(isBusy = true))

        composeRule.onNodeWithTag(BackupScreenTestTags.BUSY).assertIsDisplayed()
    }

    private companion object {
        /** Layout rounds to whole pixels, so an exact dp match is not a fair assertion. */
        const val TOLERANCE_DP = 1f
    }
}
