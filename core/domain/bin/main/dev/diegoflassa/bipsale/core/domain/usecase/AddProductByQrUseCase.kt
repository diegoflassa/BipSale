package dev.diegoflassa.bipsale.core.domain.usecase

import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import dev.diegoflassa.bipsale.core.domain.repository.ProductRepository
import javax.inject.Inject

class AddProductByQrUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(qrData: String): Result<SaleItem> = runCatching {
        val code = parseQueryParam(qrData, "code")
            ?: throw IllegalArgumentException("Invalid QR: missing 'code' parameter.")
        val priceFromQr = parseQueryParam(qrData, "price")?.toDoubleOrNull()
        val product = productRepository.getProductByCode(code)
        val name = product?.name ?: code
        val price = priceFromQr ?: product?.price ?: 0.0
        SaleItem(saleId = "", productCode = code, productName = name, unitPrice = price, quantity = 1)
    }

    private fun parseQueryParam(url: String, key: String): String? {
        val queryStart = url.indexOf('?').takeIf { it >= 0 } ?: return null
        return url.substring(queryStart + 1)
            .split('&')
            .mapNotNull { param ->
                val parts = param.split('=', limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }
            .find { it.first == key }
            ?.second
    }
}
