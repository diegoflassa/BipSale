package dev.diegoflassa.bipsale.ui.dashboard

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.ui.theme.BipSaleTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardScreenContentInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val taps = mutableListOf<String>()

    private fun setContent() {
        composeRule.setContent {
            BipSaleTheme {
                DashboardScreenContent(
                    onNewSale = { taps += "sale" },
                    onManageProducts = { taps += "products" },
                    onHistory = { taps += "history" },
                    onExport = { taps += "export" },
                    onBackup = { taps += "backup" },
                    onSettings = { taps += "settings" }
                )
            }
        }
    }

    @Test
    fun everyEntryIsReachable() {
        // Six cards overflow a phone, so the last of them is only real if it can be scrolled to.
        setContent()

        for (tag in ALL_CARDS) {
            composeRule.onNodeWithTag(tag).performScrollTo().assertExists()
        }
    }

    @Test
    fun eachCardInvokesItsOwnCallback() {
        // The six cards differ only by the lambda they were handed; a copy-paste that wires two of
        // them to the same destination is invisible on screen and only shows up here.
        setContent()

        for (tag in ALL_CARDS) {
            composeRule.onNodeWithTag(tag).performScrollTo().performClick()
        }

        assertThat(taps).containsExactly(
            "sale", "products", "history", "export", "backup", "settings"
        ).inOrder()
    }

    @Test
    fun aCardFiresOnceForOneTap() {
        setContent()

        composeRule.onNodeWithTag(DashboardScreenTestTags.NEW_SALE_CARD)
            .performScrollTo()
            .performClick()

        assertThat(taps).containsExactly("sale")
    }

    private companion object {
        val ALL_CARDS = listOf(
            DashboardScreenTestTags.NEW_SALE_CARD,
            DashboardScreenTestTags.PRODUCTS_CARD,
            DashboardScreenTestTags.HISTORY_CARD,
            DashboardScreenTestTags.REPORTS_CARD,
            DashboardScreenTestTags.BACKUP_CARD,
            DashboardScreenTestTags.SETTINGS_CARD
        )
    }
}
