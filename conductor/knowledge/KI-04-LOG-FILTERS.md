# KI-04: App Log Filters Catalogue

**Scope:** all modules (cross-cutting)
**Last verified:** 2026-08-23

## Problem

This KI is the **single source of truth** for every runtime log filter the app emits: the filter tag, the class it lives in, and what it lets an engineer diagnose from `logcat`. **Every filter listed here is protected** — none may be stripped by `/remove_filter`, `/clean`, or any "log hygiene" sweep unless the user explicitly names it and confirms (see `CORE_RULES.md` §8 "Log Filter Management" and `workflows/remove_filter.md`).

## Filter convention (CORE_RULES.md §8)

- **Tag = filter, not class.** Every log carries a leading bracket filter: `Timber.x("[BipSale][FILTER_NAME] message")`.
- **Format:** `[FILTER_PAI][FILTRO_FILHO]` → in this project, `[BipSale][FILTER_NAME]`. One filter per log message.
- `Timber.i` / `Timber.w` / `Timber.e` are intentional production signal and are never stripped regardless of filter.
- **MANDATORY:** every new log with a filter MUST be added to the catalogue below in the same turn.

## Catalogue

| Filter | Primary class | What it monitors | Level |
|---|---|---|---|
| `[BipSale][Product]` | `core/data/repository/ProductRepositoryImpl.kt` | Failures in the product CRUD path (`getProductByCode`, `insertProduct`, `updateProduct`, `deleteProduct`) — each catches an `Exception` from the Room `productDao` call and reports the failing operation plus product code. | `Timber.e` — production signal |
| `[BipSale][Product]` | `feature/products/ProductViewModel.kt` | Product list load, single-product load, save, and delete — logs the product code on entry, success, and failure, plus the emitted list size. Warns on an unknown code and on a price the parser rejects. | `Timber.d` (entry/success), `Timber.w` (rejected input / missing row), `Timber.e` (failure) |
| `[BipSale][Product][IMAGE]` | `core/data/image/ProductImageStoreImpl.kt`, `feature/products/ProductViewModel.kt` | Image import from the picker URI into app-private storage — logs the decoded dimensions and sample size, the stored file name and byte size, and the deletion outcome. Each decode failure branch reports which step failed (stream unavailable, unusable bounds, or no bitmap) plus EXIF-orientation read failures. | `Timber.d` (stored/deleted), `Timber.e` (failure) |
| `[BipSale][Product][QR_EXPORT]` | `feature/products/print/QrLabelPrintAdapter.kt`, `feature/products/print/QrLabelPrinter.kt`, `feature/products/ProductViewModel.kt`, `core/qrcode/QrGenerator.kt` | QR label sheet printing — logs the resolved media size, grid shape, labels-per-page and page count at layout time, the label/page totals actually written, and QR encode failures. Warns when a print is requested with nothing to print. | `Timber.d` (progress), `Timber.w` (empty request), `Timber.e` (encode / write failure) |
| `[BipSale][Scanner]` | `core/qrcode/QrScannerView.kt` | QR capture — logs camera binding and whether the device has a flash unit, the length of a decoded payload, and failures to obtain the provider, bind, unbind or toggle the torch. A frame holding no code is the expected result and is not logged, since that would emit at frame rate. | `Timber.d` (bind/decode), `Timber.e` (failure) |
| `[BipSale][Backup]` | `core/data/backup/BackupRepositoryImpl.kt`, `app/ui/backup/BackupViewModel.kt` | Backup archive write, inspect, restore and share staging — logs the counts written or restored, what an inspected archive holds, and staging size. | `Timber.d` (progress), `Timber.i` (write/restore totals), `Timber.w` (cancelled/no selection), `Timber.e` (failure) |
| `[BipSale][Sale]` | `core/data/repository/SaleRepositoryImpl.kt`, `feature/sales/SalesViewModel.kt` | Sale reads outside the checkout path — a `getSaleById` failure reports the sale id that failed to load, and the sales screen logs the product catalogue size it loaded for the picker, or the failure that left the picker empty. | `Timber.d` (catalogue size), `Timber.e` (failure) |
| `[BipSale][Sale][CHECKOUT]` | `feature/sales/SalesViewModel.kt`, `core/data/repository/SaleRepositoryImpl.kt` | The money path end to end — every line added (and whether it arrived by scan or by code), every line removed, every per-line and sale-level discount applied, the payment method chosen, and finalize requested → persisted → confirmed, with the sale id on the persisted and confirmed legs. A rejected scan, an unknown code, an out-of-range discount and a failed write each log their own branch. **No customer field is ever emitted** — line counts, the gross and the final total carry the diagnosis, and none of them identify anybody (`CORE_RULES.md` §8.3). | `Timber.d` (cart changes), `Timber.i` (finalize requested / persisted / confirmed), `Timber.w` (rejected input), `Timber.e` (failure) |
| `[BipSale][History]` | `feature/history/HistoryViewModel.kt` | The sales list feeding the history and export screens — which query is running (full list, search, date range), the size of every emission, and the failure that left the list empty. The query text itself is never emitted, only its length, since an operator searches by customer name and CPF (`CORE_RULES.md` §8.3). | `Timber.d` (query issued / emission size), `Timber.e` (load failure) |
| `[BipSale][Export]` | `feature/history/HistoryViewModel.kt`, `core/data/export/SalesExportRepositoryImpl.kt` | Export-to-Excel flow — logs the scope and sale count on export start, the destination URI, then the sale/line/unit counts and the gross, discount and net totals actually written with the take per payment method behind them, the finished export, a dismissed picker, and the failure branches (nothing to write, a destination with no export pending, write error). `ExportScreen` delegates to `HistoryViewModel` → `ExportSalesUseCase` → `SalesExportRepositoryImpl`. | `Timber.d` (progress), `Timber.i` (written/finished), `Timber.w` (empty/orphan destination), `Timber.e` (failure) |

The codebase now includes both error-level (`Timber.e`) and debug-level (`Timber.d`) filters. All filters are **Protected**.

## Adding / renaming procedure

Filter names are **public string contracts**. Renaming or removing any one requires updating, **in the same turn**:
1. this KI (the row),
2. `CORE_RULES.md` §8 (Log Filter Management), if it names the filter as an example,
3. `workflows/remove_filter.md`, if it names the filter,
4. every production `Timber.*` call site that emits the tag,
5. every test assertion on the removed/renamed tag.

One-turn change or none.

## Validation
- [x] All 9 distinct filter tags catalogued across 10 rows — `[BipSale][Product]` and `[BipSale][Sale]` are each emitted from both a repository and a ViewModel
- [x] No `android.util.Log` usage anywhere in the codebase
- [x] 3-segment tags `[BipSale][Product][IMAGE]`, `[BipSale][Product][QR_EXPORT]` and `[BipSale][Sale][CHECKOUT]` used for step-level granularity within their flows
- [x] Print path logs the media size and grid it resolved, so a wrong-looking sheet can be diagnosed from a capture alone
- [x] The checkout path logs every cart mutation and every finalize leg, so a sale that failed at a terminal can be reconstructed from a capture without the customer's identity appearing in it
- [x] The export path logs the destination and the totals it wrote, so a report an operator disputes can be checked against the capture without re-running the export
