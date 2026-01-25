package dev.diegoflassa.bipsale

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

import dev.diegoflassa.bipsale.core.utils.isDebug

@HiltAndroidApp
class BipSaleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (isDebug()) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
