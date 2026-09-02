package dev.diegoflassa.bipsale.core.utils.logging

/**
 * Splits log messages that would not survive a single logcat write.
 *
 * Android caps one log entry at roughly 4 KB **counted in bytes** and discards the remainder without
 * saying so. An exported sheet summary, a scanned payload or a stack trace therefore arrives silently
 * half-written — the precise failure `LOGGING_RULES.md` §8.2 exists to prevent, and it matters more
 * here than in most apps because §8.2's example is a sale that fails at a point of sale and has to be
 * reconstructed from the capture alone.
 *
 * Timber's `DebugTree` does split long messages, but it counts **characters**, so a message of 3 000
 * accented characters still overflows the byte cap; and the pieces it emits carry no filter, so
 * `logcat | grep "\[BipSale]\[Sale]"` returns the first fragment and hides the rest. This splitter
 * counts bytes and repeats the filter on every piece (§8.1).
 *
 * Applied by the planted trees, not at the call site, so the `Timber.*` calls §8 mandates stay exactly
 * as they are written.
 */
object LogChunker {

    /**
     * Byte budget for one emitted entry. Deliberately under the ~4 KB platform cap: the tag, the
     * priority and the process/thread preamble count against the same limit and are not visible here.
     */
    private const val MAX_ENTRY_BYTES = 3_500

    /**
     * Reserved for the `[part 12/34] ` marker. Sized for a three-digit count on both sides, which at
     * this budget is a message of roughly 3 MB — far past anything worth emitting.
     */
    private const val MARKER_BYTES = 20

    /** The leading `[BipSale][Sale][CHECKOUT]` run, when the message carries one (§8.1). */
    private val LEADING_FILTERS = Regex("""^(?:\[[^\[\]]+])+""")

    /**
     * Returns the pieces to emit, in order. A message that already fits comes back as a single element
     * and is left untouched — no marker, no rewriting — so the common case is byte-identical to not
     * calling this at all.
     */
    fun split(message: String): List<String> {
        if (message.utf8Size() <= MAX_ENTRY_BYTES) return listOf(message)

        val filters = LEADING_FILTERS.find(message)?.value.orEmpty()
        // Drop the single space that separated the filters from the text: each piece re-adds one after
        // its own marker, and without this the first piece would carry two.
        val body = message.substring(filters.length).removePrefix(" ")

        val budget = MAX_ENTRY_BYTES - filters.utf8Size() - MARKER_BYTES
        // A filter run long enough to swallow the whole budget would loop forever below. Emitting the
        // message untouched keeps the old truncating behaviour, which is bad but bounded.
        if (budget <= 0) return listOf(message)

        val pieces = body.chunkByUtf8Budget(budget)
        return pieces.mapIndexed { index, piece ->
            "$filters[part ${index + 1}/${pieces.size}] $piece"
        }
    }

    private fun String.utf8Size(): Int = toByteArray(Charsets.UTF_8).size

    /**
     * Splits on whole code points, never on a UTF-16 surrogate pair: half a pair becomes a replacement
     * character on the way out, corrupting the payload the split was meant to preserve.
     */
    private fun String.chunkByUtf8Budget(budget: Int): List<String> {
        val pieces = mutableListOf<String>()
        val current = StringBuilder()
        var currentBytes = 0
        var index = 0

        while (index < length) {
            val codePoint = codePointAt(index)
            val charCount = Character.charCount(codePoint)
            val byteCount = utf8ByteCount(codePoint)

            if (currentBytes + byteCount > budget && current.isNotEmpty()) {
                pieces += current.toString()
                current.setLength(0)
                currentBytes = 0
            }

            current.appendRange(this, index, index + charCount)
            currentBytes += byteCount
            index += charCount
        }

        if (current.isNotEmpty()) pieces += current.toString()
        return pieces
    }

    /** UTF-8 encodes a code point in one to four bytes, split at these boundaries. */
    private fun utf8ByteCount(codePoint: Int): Int = when {
        codePoint < ONE_BYTE_LIMIT -> ONE_BYTE
        codePoint < TWO_BYTE_LIMIT -> TWO_BYTES
        codePoint < THREE_BYTE_LIMIT -> THREE_BYTES
        else -> FOUR_BYTES
    }

    private const val ONE_BYTE_LIMIT = 0x80
    private const val TWO_BYTE_LIMIT = 0x800
    private const val THREE_BYTE_LIMIT = 0x10000

    private const val ONE_BYTE = 1
    private const val TWO_BYTES = 2
    private const val THREE_BYTES = 3
    private const val FOUR_BYTES = 4
}
