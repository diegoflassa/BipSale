# KI-04: App Log Filters Catalogue

**Scope:** all modules (cross-cutting)
**Last verified:** 2026-08-22

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
| `[BipSale][Sale]` | `core/data/repository/SaleRepositoryImpl.kt` | Failure inside `getSaleById`'s `runCatching { }.onFailure { }` when `saleDao.getFullSaleById(saleId)` throws — reports the sale id that failed to load. | `Timber.e` — production signal |
| `[BipSale][Export]` | `app/ui/export/ExportScreen.kt` | Failure in the "export sales to Excel" flow — wraps file creation/write via `ExcelExporter().exportSalesToExcel(...)`. | `Timber.e` — production signal |

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
- [x] All 5 distinct filter tags catalogued across 6 rows — `[BipSale][Product]` is emitted from both the repository and the ViewModel (verified 2026-08-22)
- [x] No `android.util.Log` usage anywhere in the codebase
- [x] 3-segment tags `[BipSale][Product][IMAGE]` and `[BipSale][Product][QR_EXPORT]` used for step-level granularity within the Product flow
- [x] Print path logs the media size and grid it resolved, so a wrong-looking sheet can be diagnosed from a capture alone
