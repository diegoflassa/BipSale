package dev.diegoflassa.bipsale.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.diegoflassa.bipsale.R
import dev.diegoflassa.bipsale.core.domain.model.ItemDiscount
import dev.diegoflassa.bipsale.core.domain.settings.AppSettings
import dev.diegoflassa.bipsale.core.domain.settings.SettingsRepository
import dev.diegoflassa.bipsale.core.domain.util.parseDecimalInput
import dev.diegoflassa.bipsale.core.ui.util.UiText
import dev.diegoflassa.bipsale.feature.sales.components.ItemDiscountKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsContract.State())
    val uiState: StateFlow<SettingsContract.State> = _uiState.asStateFlow()

    private val _effect = Channel<SettingsContract.Effect>()
    val effect: Flow<SettingsContract.Effect> = _effect.receiveAsFlow()

    init {
        load()
    }

    fun onIntent(intent: SettingsContract.Intent) {
        when (intent) {
            is SettingsContract.Intent.DiscountKindChanged ->
                _uiState.update { it.copy(discountKind = intent.kind) }

            is SettingsContract.Intent.DiscountValueChanged ->
                _uiState.update { it.copy(discountInput = intent.value) }

            is SettingsContract.Intent.AskCustomerInfoChanged ->
                _uiState.update { it.copy(askCustomerInfo = intent.enabled) }

            is SettingsContract.Intent.QrColumnsChanged ->
                _uiState.update { it.copy(qrLabelColumns = intent.columns) }

            is SettingsContract.Intent.QrLabelNameTextSizeChanged ->
                _uiState.update { it.copy(qrLabelNameTextSizePt = intent.sizePt) }

            is SettingsContract.Intent.QrLabelPriceTextSizeChanged ->
                _uiState.update { it.copy(qrLabelPriceTextSizePt = intent.sizePt) }

            is SettingsContract.Intent.Save -> save()
        }
    }

    private fun load() {
        viewModelScope.launch {
            Timber.d("[BipSale][Settings] Loading settings")
            val settings = runCatching { settingsRepository.current() }
                .onFailure { Timber.e(it, "[BipSale][Settings] Loading settings failed") }
                .getOrDefault(AppSettings.EMPTY)

            Timber.d(
                "[BipSale][Settings] Settings loaded discount=%s",
                settings.pixDiscount::class.simpleName
            )
            _uiState.update {
                it.copy(
                    discountKind = settings.pixDiscount.toKind(),
                    discountInput = settings.pixDiscount.toInput(),
                    askCustomerInfo = settings.askCustomerInfo,
                    qrLabelColumns = settings.qrLabelColumns,
                    qrLabelNameTextSizePt = settings.qrLabelNameTextSizePt,
                    qrLabelPriceTextSizePt = settings.qrLabelPriceTextSizePt,
                    isLoading = false
                )
            }
        }
    }

    private fun save() {
        val state = _uiState.value
        val discount = state.toDiscount()
        if (discount == null) {
            Timber.w("[BipSale][Settings] Rejected an unusable default discount")
            emit(SettingsContract.Effect.ShowSnackbar(UiText.StringResource(R.string.settings_discount_invalid)))
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching {
                settingsRepository.save(
                    AppSettings(
                        pixDiscount = discount,
                        askCustomerInfo = state.askCustomerInfo,
                        qrLabelColumns = state.qrLabelColumns,
                        qrLabelNameTextSizePt = state.qrLabelNameTextSizePt,
                        qrLabelPriceTextSizePt = state.qrLabelPriceTextSizePt
                    )
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                emit(SettingsContract.Effect.ShowSnackbar(UiText.StringResource(R.string.settings_saved)))
                _effect.send(SettingsContract.Effect.Saved)
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                Timber.e(throwable, "[BipSale][Settings] Saving settings failed")
                _uiState.update { it.copy(isSaving = false) }
                emit(SettingsContract.Effect.ShowSnackbar(UiText.StringResource(R.string.settings_save_failed)))
            }
        }
    }

    private fun emit(effect: SettingsContract.Effect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    /** Blank clears the discount; anything unparseable or out of range is refused, never clamped. */
    private fun SettingsContract.State.toDiscount(): ItemDiscount? {
        if (discountInput.isBlank()) return ItemDiscount.None
        val value = parseDecimalInput(discountInput) ?: return null
        return when (discountKind) {
            ItemDiscountKind.PERCENTAGE ->
                if (value > ItemDiscount.MAX_PERCENT) null else ItemDiscount.Percentage(value)

            ItemDiscountKind.AMOUNT -> ItemDiscount.Amount(value)
        }
    }

    private fun ItemDiscount.toKind(): ItemDiscountKind = when (this) {
        is ItemDiscount.Amount -> ItemDiscountKind.AMOUNT
        else -> ItemDiscountKind.PERCENTAGE
    }

    private fun ItemDiscount.toInput(): String = when (this) {
        is ItemDiscount.None -> ""
        is ItemDiscount.Percentage -> percent.toPlainInput()
        is ItemDiscount.Amount -> amount.toPlainInput()
    }

    private fun Double.toPlainInput(): String =
        if (this == toLong().toDouble()) toLong().toString() else toString()
}
