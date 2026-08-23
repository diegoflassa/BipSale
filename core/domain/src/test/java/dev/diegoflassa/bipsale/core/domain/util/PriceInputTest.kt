package dev.diegoflassa.bipsale.core.domain.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PriceInputTest {

    @Test
    fun `accepts a comma decimal separator`() {
        // The pt-BR keyboard produces a comma; parsing it as null used to save the product at 0.0.
        assertThat(parsePriceInput("130,50")).isEqualTo(130.50)
    }

    @Test
    fun `accepts a dot decimal separator`() {
        assertThat(parsePriceInput("130.50")).isEqualTo(130.50)
    }

    @Test
    fun `accepts an integer amount`() {
        assertThat(parsePriceInput("130")).isEqualTo(130.0)
    }

    @Test
    fun `trims surrounding whitespace`() {
        assertThat(parsePriceInput("  12,50  ")).isEqualTo(12.50)
    }

    @Test
    fun `rejects non numeric text`() {
        assertThat(parsePriceInput("abc")).isNull()
    }

    @Test
    fun `rejects an empty input`() {
        assertThat(parsePriceInput("")).isNull()
        assertThat(parsePriceInput("   ")).isNull()
    }

    @Test
    fun `rejects zero and negatives`() {
        assertThat(parsePriceInput("0")).isNull()
        assertThat(parsePriceInput("0,00")).isNull()
        assertThat(parsePriceInput("-5,00")).isNull()
    }

    @Test
    fun `rejects a thousands separated amount rather than guessing`() {
        // "1.234,56" carries two separators; silently reading it as 1.234 would undercharge.
        assertThat(parsePriceInput("1.234,56")).isNull()
    }

    @Test
    fun `rejects infinity and NaN literals`() {
        assertThat(parsePriceInput("Infinity")).isNull()
        assertThat(parsePriceInput("NaN")).isNull()
    }

    @Test
    fun `a decimal input accepts zero, which is how a discount is cleared`() {
        assertThat(parseDecimalInput("0")).isEqualTo(0.0)
        assertThat(parseDecimalInput("0,00")).isEqualTo(0.0)
    }

    @Test
    fun `a decimal input accepts either separator`() {
        assertThat(parseDecimalInput("12,50")).isEqualTo(12.50)
        assertThat(parseDecimalInput("12.50")).isEqualTo(12.50)
    }

    @Test
    fun `a decimal input rejects a negative value`() {
        assertThat(parseDecimalInput("-1")).isNull()
    }

    @Test
    fun `a decimal input rejects text and blanks`() {
        assertThat(parseDecimalInput("de graca")).isNull()
        assertThat(parseDecimalInput("   ")).isNull()
        assertThat(parseDecimalInput("1,2,3")).isNull()
    }
}
