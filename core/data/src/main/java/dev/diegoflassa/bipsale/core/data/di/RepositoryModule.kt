package dev.diegoflassa.bipsale.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.diegoflassa.bipsale.core.data.backup.BackupRepositoryImpl
import dev.diegoflassa.bipsale.core.data.export.SalesExportRepositoryImpl
import dev.diegoflassa.bipsale.core.data.image.ProductImageStoreImpl
import dev.diegoflassa.bipsale.core.data.product.ProductImportRepositoryImpl
import dev.diegoflassa.bipsale.core.data.repository.ProductRepositoryImpl
import dev.diegoflassa.bipsale.core.data.repository.SaleRepositoryImpl
import dev.diegoflassa.bipsale.core.data.settings.SettingsRepositoryImpl
import dev.diegoflassa.bipsale.core.domain.backup.BackupRepository
import dev.diegoflassa.bipsale.core.domain.export.SalesExportRepository
import dev.diegoflassa.bipsale.core.domain.image.ProductImageStore
import dev.diegoflassa.bipsale.core.domain.product.ProductImportRepository
import dev.diegoflassa.bipsale.core.domain.repository.ProductRepository
import dev.diegoflassa.bipsale.core.domain.repository.SaleRepository
import dev.diegoflassa.bipsale.core.domain.settings.SettingsRepository
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

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        backupRepositoryImpl: BackupRepositoryImpl
    ): BackupRepository

    @Binds
    @Singleton
    abstract fun bindSalesExportRepository(
        salesExportRepositoryImpl: SalesExportRepositoryImpl
    ): SalesExportRepository

    @Binds
    @Singleton
    abstract fun bindProductImportRepository(
        productImportRepositoryImpl: ProductImportRepositoryImpl
    ): ProductImportRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
}
