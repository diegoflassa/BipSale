package dev.diegoflassa.bipsale.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.diegoflassa.bipsale.core.data.repository.ProductRepository
import dev.diegoflassa.bipsale.core.data.repository.ProductRepositoryImpl
import dev.diegoflassa.bipsale.core.data.repository.SaleRepository
import dev.diegoflassa.bipsale.core.data.repository.SaleRepositoryImpl
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
}
