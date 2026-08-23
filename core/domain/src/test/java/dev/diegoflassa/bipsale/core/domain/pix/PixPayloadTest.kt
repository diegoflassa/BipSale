package dev.diegoflassa.bipsale.core.domain.pix

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PixPayloadTest {

    private fun payload(
        key: String = "12345678909",
        name: String = "BipSale",
        city: String = "Sao Paulo",
        amount: Double? = null
    ) = PixPayload.build(key, name, city, amount)

    /** Reads a field's value out of a payload, so assertions do not depend on offsets. */
    private fun valueOf(payload: String, id: String): String? {
        var index = 0
        while (index + 4 <= payload.length) {
            val fieldId = payload.substring(index, index + 2)
            val length = payload.substring(index + 2, index + 4).toInt()
            val value = payload.substring(index + 4, index + 4 + length)
            if (fieldId == id) return value
            index += 4 + length
        }
        return null
    }

    @Test
    fun `the payload opens with the format indicator`() {
        assertThat(payload()).startsWith("000201")
    }

    @Test
    fun `the pix key travels under the bcb domain`() {
        val account = valueOf(payload(key = "diego@example.com"), "26")

        assertThat(account).isEqualTo("0014br.gov.bcb.pix0117diego@example.com")
    }

    @Test
    fun `an amount is written with two decimals and a dot`() {
        assertThat(valueOf(payload(amount = 1234.5), "54")).isEqualTo("1234.50")
    }

    @Test
    fun `no amount means the customer types it rather than paying zero`() {
        assertThat(valueOf(payload(amount = null), "54")).isNull()
    }

    @Test
    fun `a zero or negative amount is left out rather than written as zero`() {
        assertThat(valueOf(payload(amount = 0.0), "54")).isNull()
        assertThat(valueOf(payload(amount = -5.0), "54")).isNull()
    }

    @Test
    fun `the currency is always the real and the country always BR`() {
        val result = payload()

        assertThat(valueOf(result, "53")).isEqualTo("986")
        assertThat(valueOf(result, "58")).isEqualTo("BR")
    }

    @Test
    fun `accents are folded so a bank does not show a mangled name`() {
        val result = payload(name = "Padaria São João", city = "Ribeirão Preto")

        assertThat(valueOf(result, "59")).isEqualTo("PADARIA SAO JOAO")
        assertThat(valueOf(result, "60")).isEqualTo("RIBEIRAO PRETO")
    }

    @Test
    fun `an over-long name and city are cut to the lengths the spec allows`() {
        val result = payload(
            name = "Padaria e Confeitaria Gourmet do Centro",
            city = "Sao Jose dos Campos"
        )

        assertThat(valueOf(result, "59")!!.length).isAtMost(25)
        assertThat(valueOf(result, "60")!!.length).isAtMost(15)
    }

    @Test
    fun `a blank name and city fall back rather than emitting an empty field`() {
        val result = payload(name = "   ", city = "")

        assertThat(valueOf(result, "59")).isNotEmpty()
        assertThat(valueOf(result, "60")).isNotEmpty()
    }

    @Test
    fun `a static payload carries the wildcard transaction id`() {
        assertThat(valueOf(payload(), "62")).isEqualTo("0503***")
    }

    @Test
    fun `the payload ends in a four digit CRC over everything before it`() {
        val result = payload()

        assertThat(result).contains("6304")
        val crc = result.takeLast(4)
        assertThat(crc).matches("[0-9A-F]{4}")
        assertThat(result.dropLast(4)).endsWith("6304")
    }

    @Test
    fun `changing the amount changes the CRC`() {
        val without = payload(amount = null).takeLast(4)
        val with = payload(amount = 10.0).takeLast(4)

        assertThat(without).isNotEqualTo(with)
    }

    @Test
    fun `every field declares its own length`() {
        // Walking the payload by declared lengths has to land exactly on the CRC field; if any
        // length is wrong the walk desynchronises and overruns.
        val result = payload(amount = 99.9)
        var index = 0
        var last = ""
        while (index + 4 <= result.length) {
            val id = result.substring(index, index + 2)
            val length = result.substring(index + 2, index + 4).toInt()
            index += 4 + length
            last = id
        }

        assertThat(index).isEqualTo(result.length)
        assertThat(last).isEqualTo("63")
    }

    @Test
    fun `a blank key is refused rather than producing a payload nobody can pay`() {
        runCatching { PixPayload.build("  ", "BipSale", "Sao Paulo") }
            .onSuccess { error("expected a blank key to be refused") }
            .onFailure { assertThat(it).isInstanceOf(IllegalArgumentException::class.java) }
    }

    @Test
    fun `it reproduces a known-good payload byte for byte`() {
        // A wrong field order, a wrong length or the wrong CRC variant all produce a string a bank
        // silently refuses, so the whole thing is pinned against a reference payload rather than
        // only structurally.
        val result = PixPayload.build(
            pixKey = "123e4567-e12b-12d1-a456-426655440000",
            merchantName = "FULANO DE TAL",
            merchantCity = "BRASILIA",
            amount = 10.0
        )

        assertThat(result).isEqualTo(
            "00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-426655440000" +
                "520400005303986540510.005802BR5913FULANO DE TAL6008BRASILIA62070503***6304C5A0"
        )
    }
}
