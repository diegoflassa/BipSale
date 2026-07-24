# KI-04: App Log Filters Catalogue

**Scope:** all modules (cross-cutting)
**Last verified:** 2026-07-13

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
| `[BipSale][Sale]` | `core/data/repository/SaleRepositoryImpl.kt` | Failure inside `getSaleById`'s `runCatching { }.onFailure { }` when `saleDao.getFullSaleById(saleId)` throws — reports the sale id that failed to load. | `Timber.e` — production signal |
| `[BipSale][Export]` | `app/ui/export/ExportScreen.kt` | Failure in the "export sales to Excel" flow — wraps file creation/write via `ExcelExporter().exportSalesToExcel(...)`. | `Timber.e` — production signal |

All three filters are currently error-level (`Timber.e`) — there are no debug-level (`Timber.d`) filters in the codebase yet. None are candidates for `/remove_filter` today since each is the sole diagnostic breadcrumb for its catch block; all three are effectively **Protected**.

## Adding / renaming procedure

Filter names are **public string contracts**. Renaming or removing any one requires updating, **in the same turn**:
1. this KI (the row),
2. `CORE_RULES.md` §8 (Log Filter Management), if it names the filter as an example,
3. `workflows/remove_filter.md`, if it names the filter,
4. every production `Timber.*` call site that emits the tag,
5. every test assertion on the removed/renamed tag.

One-turn change or none.

## Validation
- [x] All 3 filter tags in the codebase catalogued (verified via full-repo grep, 2026-07-13)
- [x] No `android.util.Log` usage anywhere in the codebase
- [x] No 3-segment `[BipSale][X][Y]` tags exist
