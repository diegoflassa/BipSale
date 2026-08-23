package dev.diegoflassa.bipsale.core.domain.model

/**
 * A discount on one cart line, either a percentage of the line or a fixed amount off it.
 *
 * Sealed rather than an enum plus a loose `Double`: the two carry different quantities, and a
 * single value column would let `NONE` sit next to a 5.0 nobody can interpret.
 */
sealed interface ItemDiscount {

    data object None : ItemDiscount

    data class Percentage(val percent: Double) : ItemDiscount {
        init {
            require(percent.isFinite() && percent in 0.0..MAX_PERCENT) {
                "Item discount percentage must be between 0 and $MAX_PERCENT, was $percent."
            }
        }
    }

    data class Amount(val amount: Double) : ItemDiscount {
        init {
            require(amount.isFinite() && amount >= 0.0) {
                "Item discount amount cannot be negative, was $amount."
            }
        }
    }

    companion object {
        const val MAX_PERCENT: Double = 100.0
    }
}
