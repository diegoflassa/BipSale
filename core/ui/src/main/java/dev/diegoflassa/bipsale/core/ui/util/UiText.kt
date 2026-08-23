package dev.diegoflassa.bipsale.core.ui.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed class UiText {

    data class DynamicString(val value: String) : UiText()

    /**
     * Equality is written by hand because `vararg` lands as an array, and an array compares by
     * identity: two `StringResource(R.string.x)` built from the same id would otherwise never be
     * equal, so a state holding one would look changed on every emission and recompose forever.
     */
    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any
    ) : UiText() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StringResource) return false
            return resId == other.resId && args.contentEquals(other.args)
        }

        override fun hashCode(): Int = 31 * resId + args.contentHashCode()

        override fun toString(): String = "StringResource(resId=$resId, args=${args.contentToString()})"
    }

    // The platform format APIs are vararg-only, so the array copy the spread causes is unavoidable.
    @Suppress("SpreadOperator")
    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> stringResource(resId, *args)
        }
    }

    @Suppress("SpreadOperator")
    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(resId, *args)
        }
    }
}
