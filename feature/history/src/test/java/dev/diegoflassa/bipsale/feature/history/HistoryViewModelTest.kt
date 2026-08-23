package dev.diegoflassa.bipsale.feature.history

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.domain.export.SalesExportRepository
import dev.diegoflassa.bipsale.core.domain.model.PaymentMethod
import dev.diegoflassa.bipsale.core.domain.model.Sale
import dev.diegoflassa.bipsale.core.domain.model.SaleItem
import dev.diegoflassa.bipsale.core.domain.repository.SaleRepository
import dev.diegoflassa.bipsale.core.domain.usecase.ExportSalesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun sale(id: String, customer: String, date: Long = 1_700_000_000_000) = Sale(
        id = id,
        customerName = customer,
        customerCpf = "123.456.789-00",
        totalAmount = 100.0,
        discountPercentage = 0.0,
        finalAmount = 100.0,
        paymentMethod = PaymentMethod.PIX,
        date = date,
        items = listOf(
            SaleItem(
                id = "item-$id",
                saleId = id,
                productCode = "PR-100",
                productName = "Prancheta oficio",
                unitPrice = 100.0,
                quantity = 1
            )
        )
    )

    private val ana = sale("sale-1", "Ana Paula Nogueira")
    private val bruno = sale("sale-2", "Bruno Carvalho")

    private class FakeSaleRepository(
        private val all: Flow<List<Sale>> = flowOf(emptyList()),
        private val search: (String) -> Flow<List<Sale>> = { flowOf(emptyList()) },
        private val byDate: Flow<List<Sale>> = flowOf(emptyList())
    ) : SaleRepository {
        override fun getAllSales(): Flow<List<Sale>> = all
        override suspend fun getSaleById(saleId: String): Sale? = null
        override suspend fun insertFullSale(sale: Sale) = Unit
        override fun searchSales(query: String): Flow<List<Sale>> = search(query)
        override fun getSalesByDateRange(startDate: Long, endDate: Long): Flow<List<Sale>> = byDate
    }

    private class NoOpExportRepository : SalesExportRepository {
        override suspend fun exportSales(destinationUri: String, sales: List<Sale>) = Unit
        override fun suggestedFileName(): String = "test.xlsx"
    }

    private val noOpExport = ExportSalesUseCase(NoOpExportRepository())

    private fun viewModel(repo: FakeSaleRepository) =
        HistoryViewModel(repo, noOpExport)

    @Test
    fun `loads every sale on creation`() = runTest {
        val vm = viewModel(FakeSaleRepository(all = flowOf(listOf(ana, bruno))))
        advanceUntilIdle()

        assertThat(vm.uiState.value.sales).containsExactly(ana, bruno)
        assertThat(vm.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `a search replaces the list and remembers the query`() = runTest {
        val vm = viewModel(
            FakeSaleRepository(
                all = flowOf(listOf(ana, bruno)),
                search = { query -> flowOf(listOf(ana).filter { it.customerName.contains(query) }) }
            )
        )
        advanceUntilIdle()

        vm.onIntent(HistoryContract.Intent.SearchSales("Ana"))
        advanceUntilIdle()

        assertThat(vm.uiState.value.searchQuery).isEqualTo("Ana")
        assertThat(vm.uiState.value.sales).containsExactly(ana)
    }

    @Test
    fun `a superseded query can no longer write to the list`() = runTest {
        val slowSearch = MutableSharedFlow<List<Sale>>(replay = 0)
        val vm = viewModel(
            FakeSaleRepository(
                all = flowOf(emptyList()),
                search = { query -> if (query == "Ana") slowSearch else flowOf(listOf(bruno)) }
            )
        )
        advanceUntilIdle()

        vm.onIntent(HistoryContract.Intent.SearchSales("Ana"))
        advanceUntilIdle()
        vm.onIntent(HistoryContract.Intent.SearchSales("Bruno"))
        advanceUntilIdle()

        slowSearch.emit(listOf(ana))
        advanceUntilIdle()

        assertThat(vm.uiState.value.sales).containsExactly(bruno)
    }

    @Test
    fun `refreshing after a search goes back to the full list`() = runTest {
        val vm = viewModel(
            FakeSaleRepository(
                all = flowOf(listOf(ana, bruno)),
                search = { flowOf(listOf(ana)) }
            )
        )
        advanceUntilIdle()
        vm.onIntent(HistoryContract.Intent.SearchSales("Ana"))
        advanceUntilIdle()

        vm.onIntent(HistoryContract.Intent.RefreshSales)
        advanceUntilIdle()

        assertThat(vm.uiState.value.sales).containsExactly(ana, bruno)
    }

    @Test
    fun `a date range filters the list`() = runTest {
        val vm = viewModel(
            FakeSaleRepository(all = flowOf(listOf(ana, bruno)), byDate = flowOf(listOf(bruno)))
        )
        advanceUntilIdle()

        vm.onIntent(HistoryContract.Intent.LoadSalesByDate(0, Long.MAX_VALUE))
        advanceUntilIdle()

        assertThat(vm.uiState.value.sales).containsExactly(bruno)
    }

    @Test
    fun `a load failure reports an error and stops the spinner`() = runTest {
        val vm = viewModel(
            FakeSaleRepository(all = flow { throw IllegalStateException("database is closed") })
        )

        vm.effect.test {
            advanceUntilIdle()

            assertThat(awaitItem()).isInstanceOf(HistoryContract.Effect.ShowSnackbar::class.java)
        }
        assertThat(vm.uiState.value.isLoading).isFalse()
        assertThat(vm.uiState.value.sales).isEmpty()
    }

    @Test
    fun `selection toggles on and off`() = runTest {
        val vm = viewModel(FakeSaleRepository(all = flowOf(listOf(ana, bruno))))
        advanceUntilIdle()

        vm.onIntent(HistoryContract.Intent.ToggleSaleSelection("sale-1"))
        assertThat(vm.uiState.value.selectedSaleIds).containsExactly("sale-1")

        vm.onIntent(HistoryContract.Intent.ToggleSaleSelection("sale-1"))
        assertThat(vm.uiState.value.selectedSaleIds).isEmpty()
    }

    @Test
    fun `clearing drops every selection`() = runTest {
        val vm = viewModel(FakeSaleRepository(all = flowOf(listOf(ana, bruno))))
        advanceUntilIdle()
        vm.onIntent(HistoryContract.Intent.ToggleSaleSelection("sale-1"))
        vm.onIntent(HistoryContract.Intent.ToggleSaleSelection("sale-2"))

        vm.onIntent(HistoryContract.Intent.ClearSelection)

        assertThat(vm.uiState.value.selectedSaleIds).isEmpty()
    }

    @Test
    fun `a sale's items are readable by its id`() = runTest {
        val vm = viewModel(FakeSaleRepository(all = flowOf(listOf(ana, bruno))))
        advanceUntilIdle()

        vm.getSaleItems("sale-1").test {
            assertThat(awaitItem().single().saleId).isEqualTo("sale-1")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an unknown sale id yields no items rather than throwing`() = runTest {
        val vm = viewModel(FakeSaleRepository(all = flowOf(listOf(ana))))
        advanceUntilIdle()

        vm.getSaleItems("missing").test {
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
