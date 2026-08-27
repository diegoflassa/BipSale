package dev.diegoflassa.bipsale.core.utils.logging

/**
 * Call-site redaction for log messages (`LOGGING_RULES.md` §8.3).
 *
 * §8.3 puts the decision **at the call site**, not in a Timber tree: a tree sees a finished string and
 * cannot tell a CPF from an order number, so anything it stripped it would have to strip by pattern —
 * which fails open on the first shape nobody predicted. These helpers are called where the value is
 * still typed and its meaning is still known.
 *
 * The other half of §8.3 is that **redaction is not deletion**, and this app has a specific reason to
 * care: the checkout path moves money, and a sale that fails in release still has to be reconcilable
 * from the capture. So the personal fields go and the commercial ones stay — §8.3 names
 * *"counts and totals (`items=4`, `total=12990` in centavos)"* as things to keep, because a monetary
 * total identifies nobody and is exactly what reconciliation needs. There is deliberately no helper
 * for money below.
 */
object LogRedaction {

    /**
     * Defaults to redacting. An un-configured path then over-redacts, which costs detail; the opposite
     * default would print a customer's CPF the first time something logged before [configure] ran.
     */
    @Volatile
    private var redacting: Boolean = true

    /** Called once from `BipSaleApp.onCreate`, before anything else logs. */
    fun configure(isDebugBuild: Boolean) {
        redacting = !isDebugBuild
    }

    /** True when the current build must redact — `release`. Exposed so a call site can branch. */
    val isRedacting: Boolean get() = redacting

    /**
     * A CPF. Keeps only the length, which is what distinguishes "the field was empty" from "the field
     * was 11 digits and the server still rejected it" — the actual diagnostic question.
     */
    fun cpf(value: String?): String {
        if (value == null) return "null"
        return if (redacting) "[REDACTED len=${value.length}]" else value
    }

    /** A person's name — customer, operator, anyone. Nothing about it is safe to keep but its length. */
    fun name(value: String?): String {
        if (value == null) return "null"
        return if (redacting) "[REDACTED len=${value.length}]" else value
    }

    /**
     * A phone number or e-mail. Keeps the length and, for an address, the fact that it had an `@` —
     * enough to separate a malformed value from a rejected one.
     */
    fun contact(value: String?): String {
        if (value == null) return "null"
        if (!redacting) return value
        val shape = if (value.contains('@')) " kind=email" else " kind=phone"
        return "[REDACTED len=${value.length}$shape]"
    }

    /** A filesystem path — an export destination routinely contains the device owner's name. */
    fun path(value: String?): String {
        if (value == null) return "null"
        if (!redacting) return value
        val depth = value.count { it == '/' || it == '\\' }
        return "[REDACTED path depth=$depth len=${value.length}]"
    }

    /** Any other free-text personal value with no safe shape worth keeping. */
    fun text(value: String?): String {
        if (value == null) return "null"
        return if (redacting) "[REDACTED len=${value.length}]" else value
    }
}
