# KI-08: Sales Export Report

**Scope:** `core/utils/ExcelExporter.kt`, `core/domain/model/SalesSummary.kt`, `core/domain/usecase/ExportSalesUseCase.kt`, `core/data/export/`, `app/ui/export/`, the export half of `feature/history/`
**Last verified:** 2026-08-23

## Problem

The spreadsheet is what the business reconciles against. A column that quietly means something other than its header, or a total that counts the same sale twice, is money the operator cannot account for — and unlike a UI bug, nobody notices until the numbers are compared against a drawer.

## Files

| File | Purpose |
|---|---|
| `core/domain/model/SalesSummary.kt` | `salesSummary(sales)` — every figure the report totals, plus the per-payment-method breakdown |
| `core/domain/usecase/ExportSalesUseCase.kt` | Refuses an export with nothing to write; returns how many sales reached the file |
| `core/domain/export/SalesExportRepository.kt` | Domain seam — the destination crosses it as a `String` URI so the domain stays Android-free |
| `core/data/export/SalesExportRepositoryImpl.kt` | Opens the picked document, writes off the main thread, logs the totals at the boundary |
| `core/utils/ExcelExporter.kt` | Renders the workbook |
| `feature/history/HistoryViewModel.kt` | Owns the export state machine for both screens |
| `app/ui/export/ExportScreen.kt` | Export-everything screen; delegates to `HistoryViewModel` |

## The workbook

Two sheets. **"Vendas"** — one row per sale *item*:

| # | Header | Source |
|---|---|---|
| 0 | Data | `sale.date`, a real Excel date cell |
| 1 | ID Venda | `sale.id` |
| 2 | Cliente | `sale.customerName` |
| 3 | CPF | `sale.customerCpf` |
| 4 | Código Produto | `item.productCode` |
| 5 | Produto | `item.productName` |
| 6 | Qtd | `item.quantity` |
| 7 | Valor Unit. | `item.unitPrice` |
| 8 | Desconto Item | `item.discountAmount` |
| 9 | Total Item | `item.netAmount` |
| 10 | Desconto Venda (%) | `sale.discountPercentage` — **first line of the sale only** |
| 11 | Total Venda | `sale.finalAmount` — **first line of the sale only** |

Then a blank separator row and a bold **TOTAIS** row.

**"Resumo"** — first/last sale date, sale count, units sold, average ticket, gross subtotal, item discounts, sale discounts, total discounts, net revenue, and a `Forma de pagamento / Vendas / Total` table.

## Business rules

1. **The header and the values come from one `SalesColumn` enum.** A column added in one place cannot shift the other out of alignment. `writeRow`'s `when` is exhaustive over the enum, so a new column will not compile until it is written.
2. **Sale-level columns are written once per sale**, on its first line. Repeating them made a `SUM` over the column count a multi-item sale once per item — the report over-reported revenue by exactly the number of extra lines.
3. **The totals row sums only what a sum means.** Qtd, Desconto Item, Total Item and Total Venda. **Not** Valor Unit. and **not** Desconto Venda (%) — those are per-line rates, and adding them produces a figure that looks like money and is not. `writeTotalsRow` addresses cells by enum constant, so a new column gets no total until someone decides it deserves one.
4. **Totals come from `salesSummary`, never from the exporter.** The money maths lives in `core:domain` ([KI-07](KI-07-SALES-CART-AND-DISCOUNTS.md) rule 1), and every result crosses `roundToCents`.
5. **`salesSummary` reads what each sale recorded** (`Sale.totalAmount`, `Sale.finalAmount`) rather than re-running `saleTotals` over the lines, so the report can only add up to what was actually charged — including for a sale restored from an archive written under older rules.
6. **The sale-level discount is derived, not stored:** `gross − itemDiscounts − net`. There is no `Sale` field for it.
7. **An export with nothing to write is refused**, not written. `sales.none { it.items.isNotEmpty() }` covers both an empty list and sales that carry no lines — the latter would otherwise produce a header-only file reported to the operator as a success.
8. **Never call POI's `autoSizeColumn`.** It measures text through `java.awt`, which does not exist on Android and throws at runtime. Widths are declared on the `SalesColumn` enum.
9. **The workbook is written inside `use`**, so a failure part-way through does not leak it.
10. **Sheet content is Portuguese literals, not string resources.** The workbook is a document with a fixed layout, not UI; `:core:utils` pulls in no resources. This is the one deliberate carve-out from [`CORE_RULES §10`](../rules/CORE_RULES.md), which scopes to Compose.

## The flow

`ExportScreen` (all sales) and `HistoryScreen`'s app-bar action (the selection) share `HistoryViewModel`:

1. `ExportRequested(scope)` → the ViewModel checks there is something to write, then emits `PickExportDestination(suggestedFileName)`.
2. The screen launches `ActivityResultContracts.CreateDocument`, so the operator picks Drive, Downloads or anywhere else — never app-private storage they cannot browse to.
3. `ExportDestinationChosen(uri)` or `ExportCancelled` comes back. The scope is held in `pendingExportScope` across the trip; a destination that arrives with nothing pending is dropped with a warning.
4. The write runs on `Dispatchers.IO`. Success, failure, empty and cancelled each get their own snackbar.
5. `CancellationException` is re-thrown before the generic failure branch.

## Logging

`[BipSale][Export]` — see [KI-04](KI-04-LOG-FILTERS.md). The boundary log carries the destination URI, then the sale/line/unit counts and the gross, discount and net totals, with the take per payment method behind it. Those totals are what an operator disputes a report against, and none of them identify anybody ([`CORE_RULES §8.3`](../rules/CORE_RULES.md)).

## Test targets

`SalesSummaryTest` (9) · `ExcelExporterTest` (16) · `ExportSalesUseCaseTest` (5) · the export half of `HistoryViewModelTest` (7).

The two that matter most: `the sale total is written once so summing the column cannot double count`, and `the payment breakdown adds back up to the net`.
