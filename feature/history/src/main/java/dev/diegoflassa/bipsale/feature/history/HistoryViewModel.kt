package dev.diegoflassa.bipsale.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import dev.diegoflassa.bipsale.core.domain.repository.SaleRepository
import dev.diegoflassa.bipsale.core.domain.usecase.ExportSalesUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.NoSalesToExport
import dev.diegoflassa.bipsale.core.ui.util.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val saleRepository: SaleRepository,
    private val exportSales: ExportSalesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryContract.State())
    val uiState: StateFlow<HistoryContract.State> = _uiState.asStateFlow()

    private val _effect = Channel<HistoryContract.Effect>()
    val effect: Flow<HistoryContract.Effect> = _effect.receiveAsFlow()

    /**
     * Only one query feeds the list at a time. Without this, typing a search and then clearing it
     * leaves both collectors alive, and whichever emits last wins — the list would show results
     * for a query the field no longer holds.
     */
    private var salesJob: Job? = null

    /** Survives the trip out to the document picker and back. */
    private var pendingExportScope: HistoryContract.ExportScope? = null

    init {
        onIntent(HistoryContract.Intent.RefreshSales)
    }

    fun onIntent(intent: HistoryContract.Intent) {
        when (intent) {
            is HistoryContract.Intent.SearchSales -> updateSearch(intent.query)
            is HistoryContract.Intent.LoadSalesByDate -> loadSalesByDate(intent.start, intent.end)
            is HistoryContract.Intent.RefreshSales -> refreshSales()
            is HistoryContract.Intent.ToggleSaleSelection -> toggleSelection(intent.id)
            is HistoryContract.Intent.ClearSelection -> clearSelection()
            is HistoryContract.Intent.ExportRequested -> requestExport(intent.scope)
            is HistoryContract.Intent.ExportDestinationChosen -> export(intent.destinationUri)
            is HistoryContract.Intent.ExportCancelled -> cancelExport()
        }
    }

    private fun refreshSales() {
        Timber.d("[BipSale][History] Loading every sale")
        collectSales(saleRepository.getAllSales())
    }

    private fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        Timber.d("[BipSale][History] Searching sales queryLength=%d", query.length)
        collectSales(saleRepository.searchSales(query))
    }

    private fun loadSalesByDate(start: Long, end: Long) {
        Timber.d("[BipSale][History] Loading sales between %d and %d", start, end)
        collectSales(saleRepository.getSalesByDateRange(start, end))
    }

    private fun collectSales(source: Flow<List<Sale>>) {
        salesJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }
        salesJob = viewModelScope.launch {
            source
                .catch { throwable ->
                    Timber.e(throwable, "[BipSale][History] Loading sales failed")
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.send(
                        HistoryContract.Effect.ShowSnackbar(
                            UiText.StringResource(R.string.history_load_failed)
                        )
                    )
                }
                .collect { sales ->
                    Timber.d("[BipSale][History] Sales emitted count=%d", sales.size)
                    _uiState.update { it.copy(sales = sales, isLoading = false) }
                }
        }
    }

    private fun toggleSelection(id: String) {
        _uiState.update { state ->
            val newSelection = if (state.selectedSaleIds.contains(id)) {
                state.selectedSaleIds - id
            } else {
                state.selectedSaleIds + id
            }
            state.copy(selectedSaleIds = newSelection)
        }
    }

    private fun clearSelection() {
        _uiState.update { it.copy(selectedSaleIds = emptySet()) }
    }

    /** Checks there is something to write before sending the operator off to pick a destination. */
    private fun requestExport(scope: HistoryContract.ExportScope) {
        val sales = salesIn(scope)
        if (sales.isEmpty()) {
            Timber.w("[BipSale][Export] Export requested with nothing to write scope=%s", scope)
            pendingExportScope = null
            viewModelScope.launch {
                _effect.send(
                    HistoryContract.Effect.ShowSnackbar(
                        UiText.StringResource(R.string.history_export_empty)
                    )
                )
            }
            return
        }
        Timber.d("[BipSale][Export] Export requested scope=%s sales=%d", scope, sales.size)
        pendingExportScope = scope
        viewModelScope.launch {
            _effect.send(
                HistoryContract.Effect.PickExportDestination(exportSales.suggestedFileName())
            )
        }
    }

    private fun export(destinationUri: String) {
        val scope = pendingExportScope
        if (scope == null) {
            Timber.w("[BipSale][Export] A destination arrived with no export pending")
            return
        }
        pendingExportScope = null
        val sales = salesIn(scope)
        _uiState.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            Timber.d("[BipSale][Export] Writing scope=%s sales=%d", scope, sales.size)
            exportSales(destinationUri, sales)
                .onSuccess { count ->
                    Timber.i("[BipSale][Export] Export finished scope=%s sales=%d", scope, count)
                    _uiState.update {
                        val selection = if (scope == HistoryContract.ExportScope.SELECTED) {
                            emptySet()
                        } else {
                            it.selectedSaleIds
                        }
                        it.copy(isExporting = false, selectedSaleIds = selection)
                    }
                    _effect.send(
                        HistoryContract.Effect.ShowSnackbar(
                            UiText.StringResource(R.string.history_export_success, count)
                        )
                    )
                }
                .onFailure { failure(it) }
        }
    }

    private fun cancelExport() {
        Timber.d("[BipSale][Export] Destination picker dismissed")
        pendingExportScope = null
        viewModelScope.launch {
            _effect.send(
                HistoryContract.Effect.ShowSnackbar(
                    UiText.StringResource(R.string.history_export_cancelled)
                )
            )
        }
    }

    private suspend fun failure(throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        Timber.e(throwable, "[BipSale][Export] Writing the spreadsheet failed")
        _uiState.update { it.copy(isExporting = false) }
        // The list can empty out between picking a destination and writing, so that failure gets
        // the message that explains itself rather than a generic one.
        val message = if (throwable is NoSalesToExport) {
            R.string.history_export_empty
        } else {
            R.string.history_export_failed
        }
        _effect.send(HistoryContract.Effect.ShowSnackbar(UiText.StringResource(message)))
    }

    private fun salesIn(scope: HistoryContract.ExportScope): List<Sale> {
        val state = _uiState.value
        return when (scope) {
            HistoryContract.ExportScope.ALL -> state.sales
            HistoryContract.ExportScope.SELECTED ->
                state.sales.filter { it.id in state.selectedSaleIds }
        }
    }

    fun getSaleItems(saleId: String): Flow<List<SaleItem>> {
        return uiState.map { state ->
            state.sales.find { it.id == saleId }?.items ?: emptyList()
        }
    }
}
