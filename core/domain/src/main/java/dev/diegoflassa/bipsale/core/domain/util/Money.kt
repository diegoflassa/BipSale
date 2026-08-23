package dev.diegoflassa.bipsale.core.domain.util

import java.math.BigDecimal
import java.math.RoundingMode

private const val CENTS_SCALE = 2

/**
 * A binary `Double` sum lands on 80.91000000000001, and that is the number Room stores as the
 * amount actually charged. Every monetary result crosses this before it is shown or persisted.
 */
fun Double.roundToCents(): Double =
    BigDecimal.valueOf(this).setScale(CENTS_SCALE, RoundingMode.HALF_UP).toDouble()
