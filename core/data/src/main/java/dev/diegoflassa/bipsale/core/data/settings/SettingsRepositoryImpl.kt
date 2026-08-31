package dev.diegoflassa.bipsale.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.diegoflassa.bipsale.core.domain.model.ItemDiscount
import dev.diegoflassa.bipsale.core.domain.settings.AppSettings
import dev.diegoflassa.bipsale.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "bipsale_settings"
)

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    override val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { throwable ->
            // A corrupt preferences file must not take the sale screen down with it.
            if (throwable !is IOException) throw throwable
            Timber.e(throwable, "[BipSale][Settings] Reading settings failed, falling back")
            emit(emptyPreferences())
        }
        .map { it.toAppSettings() }

    override suspend fun current(): AppSettings = settings.first()

    override suspend fun save(settings: AppSettings) {
        Timber.d(
            "[BipSale][Settings] Saving discount=%s askCustomer=%b qrColumns=%d",
            settings.pixDiscount.storedType(),
            settings.askCustomerInfo,
            settings.qrLabelColumns
        )
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_DISCOUNT_TYPE] = settings.pixDiscount.storedType()
            prefs[KEY_DISCOUNT_VALUE] = settings.pixDiscount.storedValue()
            prefs[KEY_ASK_CUSTOMER] = settings.askCustomerInfo
            prefs[KEY_QR_LABEL_COLUMNS] = settings.qrLabelColumns
        }
        Timber.i("[BipSale][Settings] Settings saved")
    }

    private fun Preferences.toAppSettings() = AppSettings(
        pixDiscount = readDiscount(
            this[KEY_DISCOUNT_TYPE].orEmpty(),
            this[KEY_DISCOUNT_VALUE] ?: 0.0
        ),
        askCustomerInfo = this[KEY_ASK_CUSTOMER] ?: true,
        qrLabelColumns = (this[KEY_QR_LABEL_COLUMNS] ?: AppSettings.DEFAULT_QR_LABEL_COLUMNS)
            .coerceIn(AppSettings.MIN_QR_LABEL_COLUMNS, AppSettings.MAX_QR_LABEL_COLUMNS)
    )

    private companion object {
        val KEY_DISCOUNT_TYPE = stringPreferencesKey("pix_discount_type")
        val KEY_DISCOUNT_VALUE = doublePreferencesKey("pix_discount_value")
        val KEY_ASK_CUSTOMER = booleanPreferencesKey("ask_customer_info")
        val KEY_QR_LABEL_COLUMNS = intPreferencesKey("qr_label_columns")
    }
}

internal fun ItemDiscount.storedType(): String = when (this) {
    is ItemDiscount.None -> "NONE"
    is ItemDiscount.Percentage -> "PERCENTAGE"
    is ItemDiscount.Amount -> "AMOUNT"
}

internal fun ItemDiscount.storedValue(): Double = when (this) {
    is ItemDiscount.None -> 0.0
    is ItemDiscount.Percentage -> percent
    is ItemDiscount.Amount -> amount
}

/** A stored value out of range is coerced rather than thrown — a bad row must not brick checkout. */
internal fun readDiscount(type: String, value: Double): ItemDiscount {
    if (!value.isFinite()) return ItemDiscount.None
    return when (type) {
        "PERCENTAGE" -> ItemDiscount.Percentage(value.coerceIn(0.0, ItemDiscount.MAX_PERCENT))
        "AMOUNT" -> ItemDiscount.Amount(value.coerceAtLeast(0.0))
        else -> ItemDiscount.None
    }
}
