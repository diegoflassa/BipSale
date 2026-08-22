package dev.diegoflassa.bipsale.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.diegoflassa.bipsale.core.data.image.ProductImageStoreImpl
import dev.diegoflassa.bipsale.core.data.repository.ProductRepositoryImpl
import dev.diegoflassa.bipsale.core.data.repository.SaleRepositoryImpl
import dev.diegoflassa.bipsale.core.domain.image.ProductImageStore
import dev.diegoflassa.bipsale.core.domain.repository.ProductRepository
import dev.diegoflassa.bipsale.core.domain.repository.SaleRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(
        productRepositoryImpl: ProductRepositoryImpl
    ): ProductRepository

    @Binds
    @Singleton
    abstract fun bindSaleRepository(
        saleRepositoryImpl: SaleRepositoryImpl
    ): SaleRepository

    @Binds
    @Singleton
    abstract fun bindProductImageStore(
        productImageStoreImpl: ProductImageStoreImpl
    ): ProductImageStore
}
