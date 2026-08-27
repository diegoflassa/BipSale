package dev.diegoflassa.bipsale

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

import dev.diegoflassa.bipsale.core.utils.isDebug
import dev.diegoflassa.bipsale.core.utils.logging.LogRedaction
import dev.diegoflassa.bipsale.logging.ChunkedDebugTree
import dev.diegoflassa.bipsale.logging.ReleaseTree

@HiltAndroidApp
class BipSaleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        installLogging()
        Timber.i("[BipSale][App] onCreate debug=${isDebug()} redacting=${LogRedaction.isRedacting}")
    }

    /**
     * Plants the tree for this build and arms call-site redaction in the same step
     * (`LOGGING_RULES.md` §8.3, §8.6).
     *
     * The two belong together: the tree decides which levels reach the field, and [LogRedaction]
     * decides what those surviving lines may say. Arming one without the other gives either a mute
     * release build or a talkative one that leaks a customer's CPF — so there is exactly one call site
     * for both, and it runs before anything else logs.
     *
     * Release used to plant nothing at all, which meant no log line ever left a shipped build.
     */
    private fun installLogging() {
        val debugBuild = isDebug()
        LogRedaction.configure(isDebugBuild = debugBuild)
        Timber.plant(if (debugBuild) ChunkedDebugTree() else ReleaseTree())
    }
}
