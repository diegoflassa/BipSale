package dev.diegoflassa.bipsale.core.domain.pix

/**
 * The shop's fixed Nubank PIX identity, extracted from the poster's QR.
 *
 * Change any constant here → rebuild. The key is an EVP (random UUID) registered with Nubank
 * under CNPJ 64.333.090/0001-15. If the key is ported to another institution the QR stops
 * working and a new build is needed.
 */
object PixDefaults {
    const val KEY = "d9ee61ce-fa46-405c-9e85-100064830927"
    const val MERCHANT_NAME = "Marcia Andrea dos Reis Carreiro"
    const val MERCHANT_CITY = "Campinas"
}
