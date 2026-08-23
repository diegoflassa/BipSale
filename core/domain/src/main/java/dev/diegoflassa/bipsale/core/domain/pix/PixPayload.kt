package dev.diegoflassa.bipsale.core.domain.pix

import java.text.Normalizer
import java.util.Locale

/**
 * Builds the "PIX Copia e Cola" string a customer's bank app reads from a QR code.
 *
 * This is the EMV® merchant-presented QR format the Banco Central specifies for PIX: a flat list of
 * `id + two-digit length + value` fields, closed by a CRC over everything before it. A static
 * payload can carry the amount, which is why the code below fills tag 54 whenever there is one —
 * the customer then confirms a pre-filled value instead of typing it, and a typo at the counter
 * stops being possible.
 */
object PixPayload {

    /**
     * @param amount the sale total, or `null` to leave the customer to type it. A non-positive
     *  amount is treated as absent rather than written as zero, which banks reject.
     */
    fun build(
        pixKey: String,
        merchantName: String,
        merchantCity: String,
        amount: Double? = null
    ): String {
        require(pixKey.isNotBlank()) { "A PIX payload needs a key." }

        val body = buildString {
            append(field(ID_FORMAT, FORMAT_INDICATOR))
            append(field(ID_MERCHANT_ACCOUNT, merchantAccount(pixKey)))
            append(field(ID_CATEGORY, CATEGORY_UNDEFINED))
            append(field(ID_CURRENCY, CURRENCY_BRL))
            amount?.takeIf { it.isFinite() && it > 0.0 }?.let {
                append(field(ID_AMOUNT, formatAmount(it)))
            }
            append(field(ID_COUNTRY, COUNTRY_BR))
            append(field(ID_MERCHANT_NAME, sanitize(merchantName, MAX_NAME).ifBlank { FALLBACK_NAME }))
            append(field(ID_MERCHANT_CITY, sanitize(merchantCity, MAX_CITY).ifBlank { FALLBACK_CITY }))
            append(field(ID_ADDITIONAL_DATA, field(ID_TXID, STATIC_TXID)))
        }

        // The CRC is computed over the payload plus its own id and length, which is why they are
        // appended before the checksum is taken rather than after.
        val withCrcHeader = body + ID_CRC + CRC_LENGTH
        return withCrcHeader + crc16(withCrcHeader)
    }

    private fun merchantAccount(pixKey: String): String =
        field(ID_GUI, PIX_GUI) + field(ID_KEY, pixKey.trim())

    private fun field(id: String, value: String): String =
        id + value.length.toString().padStart(LENGTH_DIGITS, '0') + value

    private fun formatAmount(amount: Double): String = String.format(Locale.US, "%.2f", amount)

    /**
     * The spec allows only a restricted character set here, and a bank that reads an accented name
     * shows the customer a mangled one. Accents are folded, everything else outside the set is
     * dropped, then the field is cut to its maximum length.
     */
    private fun sanitize(value: String, maxLength: Int): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .filter { it.isLetterOrDigit() || it == ' ' }
            .trim()
            .take(maxLength)
            .uppercase(Locale.US)

    /** CRC-16/CCITT-FALSE, the variant the PIX spec names. */
    private fun crc16(payload: String): String {
        var crc = CRC_INITIAL
        payload.toByteArray(Charsets.UTF_8).forEach { byte ->
            crc = crc xor (byte.toInt() and BYTE_MASK shl BYTE_SHIFT)
            repeat(BITS_PER_BYTE) {
                crc = if (crc and TOP_BIT != 0) {
                    (crc shl 1) xor CRC_POLYNOMIAL
                } else {
                    crc shl 1
                }
                crc = crc and WORD_MASK
            }
        }
        return crc.toString(HEX_RADIX).uppercase(Locale.US).padStart(CRC_DIGITS, '0')
    }

    private val DIACRITICS = "\\p{Mn}".toRegex()

    private const val ID_FORMAT = "00"
    private const val ID_MERCHANT_ACCOUNT = "26"
    private const val ID_CATEGORY = "52"
    private const val ID_CURRENCY = "53"
    private const val ID_AMOUNT = "54"
    private const val ID_COUNTRY = "58"
    private const val ID_MERCHANT_NAME = "59"
    private const val ID_MERCHANT_CITY = "60"
    private const val ID_ADDITIONAL_DATA = "62"
    private const val ID_TXID = "05"
    private const val ID_GUI = "00"
    private const val ID_KEY = "01"
    private const val ID_CRC = "63"

    private const val FORMAT_INDICATOR = "01"
    private const val PIX_GUI = "br.gov.bcb.pix"
    private const val CATEGORY_UNDEFINED = "0000"
    private const val CURRENCY_BRL = "986"
    private const val COUNTRY_BR = "BR"
    private const val STATIC_TXID = "***"
    private const val CRC_LENGTH = "04"
    private const val FALLBACK_NAME = "NAO INFORMADO"
    private const val FALLBACK_CITY = "SAO PAULO"

    private const val LENGTH_DIGITS = 2
    private const val MAX_NAME = 25
    private const val MAX_CITY = 15
    private const val CRC_DIGITS = 4
    private const val CRC_INITIAL = 0xFFFF
    private const val CRC_POLYNOMIAL = 0x1021
    private const val TOP_BIT = 0x8000
    private const val WORD_MASK = 0xFFFF
    private const val BYTE_MASK = 0xFF
    private const val BYTE_SHIFT = 8
    private const val BITS_PER_BYTE = 8
    private const val HEX_RADIX = 16
}
