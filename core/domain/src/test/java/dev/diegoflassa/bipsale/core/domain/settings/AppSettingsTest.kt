package dev.diegoflassa.bipsale.core.domain.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `an empty configuration names every missing field`() {
        assertThat(AppSettings.EMPTY.missingPixFields())
            .containsExactly(PixField.KEY, PixField.MERCHANT_NAME, PixField.MERCHANT_CITY)
    }

    @Test
    fun `a fully configured PIX reports nothing missing`() {
        val settings = AppSettings(
            pixKey = "12345678909",
            pixMerchantName = "Padaria do Centro",
            pixMerchantCity = "Campinas"
        )

        assertThat(settings.missingPixFields()).isEmpty()
        assertThat(settings.isPixConfigured).isTrue()
    }

    @Test
    fun `a key alone is enough to build a payload but the other fields are still reported`() {
        val settings = AppSettings(pixKey = "12345678909")

        assertThat(settings.isPixConfigured).isTrue()
        assertThat(settings.missingPixFields())
            .containsExactly(PixField.MERCHANT_NAME, PixField.MERCHANT_CITY)
    }

    @Test
    fun `whitespace is not a configured value`() {
        val settings = AppSettings(pixKey = "   ", pixMerchantName = "  ", pixMerchantCity = "")

        assertThat(settings.isPixConfigured).isFalse()
        assertThat(settings.missingPixFields()).hasSize(3)
    }
}
