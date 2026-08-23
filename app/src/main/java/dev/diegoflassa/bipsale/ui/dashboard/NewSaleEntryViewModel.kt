package dev.diegoflassa.bipsale.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.diegoflassa.bipsale.core.domain.settings.AppSettings
import dev.diegoflassa.bipsale.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Decides which screen a new sale opens on.
 *
 * `null` means the setting has not been read yet, so the caller waits rather than flashing the
 * customer form and navigating off it a frame later.
 */
@HiltViewModel
class NewSaleEntryViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _asksForCustomer = MutableStateFlow<Boolean?>(null)
    val asksForCustomer: StateFlow<Boolean?> = _asksForCustomer.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = runCatching { settingsRepository.current() }
                .onFailure { Timber.e(it, "[BipSale][Settings] Reading the sale entry point failed") }
                .getOrDefault(AppSettings.EMPTY)
            Timber.d("[BipSale][Sale] New sale asks for customer=%b", settings.askCustomerInfo)
            _asksForCustomer.value = settings.askCustomerInfo
        }
    }
}
