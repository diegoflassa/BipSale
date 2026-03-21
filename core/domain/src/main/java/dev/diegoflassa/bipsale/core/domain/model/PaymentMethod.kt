package dev.diegoflassa.bipsale.core.domain.model

enum class PaymentMethod(val serializedName: String) {
    PIX("PIX"),
    CASH("CASH"),
    CREDIT_CARD("CREDIT_CARD"),
    DEBIT_CARD("DEBIT_CARD");

    companion object {
        fun fromString(value: String): PaymentMethod =
            entries.find { it.serializedName == value } ?: PIX
    }
}
