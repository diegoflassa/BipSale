package dev.diegoflassa.bipsale.core.utils

import android.content.Context
import android.content.pm.ApplicationInfo

/**
 * Checks if the application is running in debug mode.
 */
fun Context.isDebug(): Boolean {
    return (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
