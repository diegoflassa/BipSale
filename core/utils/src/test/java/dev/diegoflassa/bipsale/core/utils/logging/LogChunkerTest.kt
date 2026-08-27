package dev.diegoflassa.bipsale.core.utils.logging

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pins the defect `LogChunker` was written to fix (CORE_RULES §12): a message over the logcat byte cap
 * lost its tail silently, and whatever did survive was unfindable because only the first fragment
 * carried the `[BipSale][…]` filter.
 *
 * This matters most on the checkout path, where §8.2's stated reason for existing is that a sale which
 * fails at a point of sale has to be reconstructible from the capture alone.
 */
class LogChunkerTest {

    private val filter = "[BipSale][Sale][CHECKOUT]"

    private fun String.utf8Size() = toByteArray(Charsets.UTF_8).size

    @Test
    fun `a message that fits is returned untouched`() {
        val message = "$filter finalize confirmed saleId=42 items=4 total=12990"

        assertThat(LogChunker.split(message)).containsExactly(message)
    }

    @Test
    fun `an oversized message is split rather than truncated`() {
        val message = "$filter " + "a".repeat(10_000)

        val pieces = LogChunker.split(message)

        assertThat(pieces.size).isGreaterThan(1)
        val rejoined = pieces.joinToString("") { it.substringAfter("] ", missingDelimiterValue = it) }
        assertThat(rejoined).isEqualTo("a".repeat(10_000))
    }

    @Test
    fun `every piece carries the filter so a grep finds the whole message`() {
        val pieces = LogChunker.split("$filter " + "b".repeat(10_000))

        assertThat(pieces).isNotEmpty()
        pieces.forEach { assertThat(it).startsWith(filter) }
    }

    @Test
    fun `every piece fits inside one logcat entry, counted in bytes not characters`() {
        // Portuguese product names are full of accented characters, and each costs two bytes. A
        // character-based budget — which is what Timber's own splitter uses — waves these through.
        val message = "$filter " + "ã".repeat(4_000)

        val pieces = LogChunker.split(message)

        assertThat(pieces.size).isGreaterThan(1)
        pieces.forEach { assertThat(it.utf8Size()).isAtMost(3_500) }
    }

    @Test
    fun `a surrogate pair is never split down the middle`() {
        val pieces = LogChunker.split("$filter " + "😀".repeat(2_000))

        assertThat(pieces.size).isGreaterThan(1)
        pieces.forEach { piece ->
            assertThat(piece.last().isHighSurrogate()).isFalse()
            assertThat(piece.first().isLowSurrogate()).isFalse()
        }
    }
}
