package dev.diegoflassa.bipsale.logging

import android.util.Log
import dev.diegoflassa.bipsale.core.utils.logging.LogChunker
import timber.log.Timber

/**
 * The `debug` tree: every level, full fidelity (`LOGGING_RULES.md` §8.3), but split by bytes.
 *
 * `Timber.DebugTree` already splits, and splits badly for this codebase's purposes — it counts
 * characters rather than bytes, and its pieces carry no `[BipSale][…]` filter, so grepping a filter
 * returns the first fragment of a long message and silently hides the rest. [LogChunker] fixes both;
 * this tree exists only to apply it.
 */
class ChunkedDebugTree : Timber.DebugTree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val pieces = LogChunker.split(message)
        if (pieces.size == 1) {
            super.log(priority, tag, message, t)
            return
        }
        pieces.forEachIndexed { index, piece ->
            // The throwable rides on the last piece only: Timber renders the whole stack trace onto
            // every message it is handed, so attaching it to each piece would repeat the trace once
            // per piece and rebuild the oversized entry the split just prevented.
            super.log(priority, tag, piece, t.takeIf { index == pieces.lastIndex })
        }
    }
}

/**
 * The `release` tree (§8.6).
 *
 * Before this existed, `BipSaleApp` planted a tree only in debug — so **release builds logged
 * nothing at all**. That is the failure §8.6 describes in its own words: *"coverage without a level
 * policy produces exactly the failure they exist to prevent — everything at `Timber.d`, a mute release
 * build, and redaction that has quietly become deletion."* §8.6 defines the gate as *"drops
 * `Timber.d`/`Timber.v` and forwards the rest"*, which is what this does.
 *
 * It does **not** redact: §8.3 puts that at the call site via
 * [dev.diegoflassa.bipsale.core.utils.logging.LogRedaction], where the value is still typed. A tree
 * only sees a finished string and would have to guess.
 */
class ReleaseTree : Timber.Tree() {

    /** Skips the two developer-chatter levels before Timber builds the message. */
    // Widened from Timber's protected declaration so the ReleaseTreeTest can pin the §8.6 gate.
    public override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.INFO

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val label = tag ?: DEFAULT_TAG
        // Straight to the platform, never back through Timber: this tree is planted in the forest, so
        // a Timber.log() here would re-enter this same method until the stack ran out.
        LogChunker.split(message).forEachIndexed { index, piece ->
            if (priority == Log.ASSERT) {
                Log.wtf(label, piece)
            } else {
                Log.println(priority, label, piece)
            }
            if (t != null && index == 0 && priority >= Log.ERROR) {
                Log.println(priority, label, Log.getStackTraceString(t))
            }
        }
    }

    private companion object {
        const val DEFAULT_TAG = "BipSale"
    }
}
