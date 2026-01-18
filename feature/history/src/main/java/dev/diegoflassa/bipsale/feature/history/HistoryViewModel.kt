package dev.diegoflassa.bipsale.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.diegoflassa.bipsale.core.domain.repository.SaleRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val saleRepository: SaleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryContract.State())
    val uiState: StateFlow<HistoryContract.State> = _uiState.asStateFlow()

    private val _effect = Channel<HistoryContract.Effect>()
    val effect: Flow<HistoryContract.Effect> = _effect.receiveAsFlow()

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
        }
    }

    private fun refreshSales() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // getAllSales now returns domain Sales which include items if mapped correctly by repo
            saleRepository.getAllSales().collect { list ->
                _uiState.update { it.copy(sales = list, isLoading = false) }
            }
        }
    }

    private fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query, isLoading = true) }
        viewModelScope.launch {
            saleRepository.searchSales(query).collect { list ->
                _uiState.update { it.copy(sales = list, isLoading = false) }
            }
        }
    }

    private fun loadSalesByDate(start: Long, end: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            saleRepository.getSalesByDateRange(start, end).collect { list ->
                _uiState.update { it.copy(sales = list, isLoading = false) }
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

    fun getSaleItems(saleId: String): Flow<List<dev.diegoflassa.bipsale.core.domain.model.SaleItem>> {
        return uiState.map { state ->
            state.sales.find { it.id == saleId }?.items ?: emptyList()
        }
    }
}
