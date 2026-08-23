package dev.diegoflassa.bipsale.core.domain.util

/**
 * Parses a price the way an operator types it. pt-BR keyboards produce a comma decimal separator,
 * and `String.toDoubleOrNull` only accepts a dot — accepting "130,50" as 130.50 is what keeps a
 * mistyped separator from silently becoming a zero-priced product.
 */
fun parsePriceInput(raw: String): Double? {
    val normalized = raw.trim().replace(',', '.')
    if (normalized.isEmpty()) return null
    if (normalized.count { it == '.' } > 1) return null
    val value = normalized.toDoubleOrNull() ?: return null
    if (!value.isFinite() || value <= 0.0) return null
    return value
}

/**
 * Any non-negative decimal an operator can type, comma or dot. Unlike [parsePriceInput] this
 * accepts zero, because zero is how a discount is cleared. Callers apply their own upper bound.
 */
fun parseDecimalInput(raw: String): Double? {
    val normalized = raw.trim().replace(',', '.')
    if (normalized.isEmpty()) return null
    if (normalized.count { it == '.' } > 1) return null
    val value = normalized.toDoubleOrNull() ?: return null
    if (!value.isFinite() || value < 0.0) return null
    return value
}
