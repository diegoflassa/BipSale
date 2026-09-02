package dev.diegoflassa.bipsale.core.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.diegoflassa.bipsale.core.data.database.BipSaleDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BipSaleDatabase {
        // No destructive fallback, deliberately: this database holds the sales record, and wiping
        // it on a schema-hash mismatch loses money the shop cannot reconcile (CORE_RULES §13).
        return Room.databaseBuilder(
            context,
            BipSaleDatabase::class.java,
            BipSaleDatabase.NAME
        )
            .apply {
                BipSaleDatabase.MIGRATIONS.forEach { migration -> addMigrations(migration) }
            }
            .build()
    }

    @Provides
    fun provideProductDao(database: BipSaleDatabase) = database.productDao()

    @Provides
    fun provideSaleDao(database: BipSaleDatabase) = database.saleDao()
}
