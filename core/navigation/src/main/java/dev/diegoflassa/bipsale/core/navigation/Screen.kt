package dev.diegoflassa.bipsale.core.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Dashboard : Screen

    @Serializable
    data object NewSale : Screen

    @Serializable
    data object ManageProducts : Screen

    @Serializable
    data object History : Screen

    @Serializable
    data object Export : Screen

    @Serializable
    data object Backup : Screen

    @Serializable
    data class ProductDetail(val productCode: String?) : Screen

    @Serializable
    data class SaleDetail(val saleId: String) : Screen
}
