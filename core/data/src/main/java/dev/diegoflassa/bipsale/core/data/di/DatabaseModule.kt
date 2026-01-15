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
        return Room.databaseBuilder(
            context,
            BipSaleDatabase::class.java,
            "bipsale_database"
        ).build()
    }

    @Provides
    fun provideProductDao(database: BipSaleDatabase) = database.productDao()

    @Provides
    fun provideSaleDao(database: BipSaleDatabase) = database.saleDao()
}
