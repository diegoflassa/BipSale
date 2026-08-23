# KI-07: Sales Cart, Totals & Discounts

**Scope:** `core/domain/model/` (money types), `feature/sales/`, the `sale_items` discount columns
**Last verified:** 2026-08-22

## Problem

Checkout moves money. Every number the operator sees and every number Room stores comes from **one** implementation, because two implementations of a total is how a screen ends up disagreeing with a receipt.

## Files

| File | Purpose |
|---|---|
| `core/domain/util/Money.kt` | `Double.roundToCents()` — HALF_UP to 2 decimals. Every monetary result crosses it before display or persistence |
| `core/domain/model/ItemDiscount.kt` | `None` / `Percentage(percent)` / `Amount(amount)`, validated at construction |
| `core/domain/model/SaleItem.kt` | A cart line: stable `id`, `discount`, and the derived `grossAmount` / `discountAmount` / `netAmount` |
| `core/domain/model/SaleTotals.kt` | `saleTotals(items, discountPercentage)` — **the** money maths |
| `core/domain/usecase/FinalizeSaleUseCase.kt` | Builds and persists the `Sale` from `saleTotals` |
| `core/domain/usecase/AddProductByCodeUseCase.kt` | Cart entry by code — catalogue pick and typed code |
| `core/domain/usecase/AddProductByQrUseCase.kt` | Cart entry by scan |
| `feature/sales/SalesContract.kt` | State / Intent / Effect |
| `feature/sales/SalesViewModel.kt` | Cart state, `[BipSale][Sale][CHECKOUT]` breadcrumbs |
| `feature/sales/SalesScreen.kt` | `SalesScreen` (wiring) + `SalesScreenContent` (stateless layout) |
| `feature/sales/SalesScreenTestTags.kt` | Test tags |
| `feature/sales/components/` | `SaleItemRow`, `SaleBottomBar`, `PaymentMethodDropdown`, `DiscountDialog`, `ItemDiscountDialog`, `ItemDiscountKind`, `ProductPickerDialog`, `ManualCodeDialog` |

## Business rules

### Money

1. **`saleTotals` is the only place the maths lives.** `SalesViewModel.recalculate()` and `FinalizeSaleUseCase` both call it. Never re-derive a total anywhere else.
2. **Every monetary result is rounded HALF_UP to cents.** A raw `Double` sum reaches values like `80.91000000000001`, and that is what Room would store as the amount charged.
3. **Order of operations:** line gross → per-line discount → line net → sum → sale-level percentage → final. The sale percentage is charged on what the line discounts left, never on the gross.
4. **`Sale.totalAmount` is the gross**, before any discount, so the receipt keeps a readable subtotal. `Sale.finalAmount` is what the customer pays.

### Discounts

| Rule | Where enforced |
|---|---|
| A percentage is 0–100, finite | `ItemDiscount.Percentage.init`, `saleTotals` for the sale-level one |
| A fixed amount is non-negative, finite | `ItemDiscount.Amount.init` |
| A fixed amount never exceeds its line | `SaleItem.discountAmount` coerces — otherwise the line would pay the customer |
| Zero clears a discount rather than storing a 0% one | `ItemDiscountDialog`'s `toDiscount` |
| An out-of-range sale discount is refused with an error, never clamped silently | `SalesViewModel.updateDiscount` |
| A stored discount out of range is coerced at the mapper, not thrown | `SaleMapper.readDiscount` — a corrupt row must not take the history screen down |

### Cart

- **Every line has a stable `id`.** Two scans of the same product are two lines that discount and delete independently; intents address lines by id, never by index or by value equality.
- **Three ways in:** scan (`AddProductByQr`), catalogue pick and typed code (both `AddProductByCode`).
- **A scan trusts the price on the label** over the registered one — the label is what the shop advertised. A **typed code is refused if unknown**, because there is no label to trust and a zero-priced line would otherwise be rung up.
- `canFinalize` is false for an empty cart, while a write is in flight, and after the sale is finished.

## Storage

`sale_items` carries `discountType` (`NONE` / `PERCENTAGE` / `AMOUNT`) + `discountValue`. The sealed `ItemDiscount` is flattened at the mapper seam — entities stay Room-only, per [`architecture.md`](../rules/architecture.md). `BackupSaleItem` carries both fields with defaults, so an archive written before per-line discounts still restores.

> **No migration ships for the column addition.** The app is unreleased, so dev-time schema churn is reset by clearing app data ([`CORE_RULES §13.4`](../rules/CORE_RULES.md)) — which is why the debug install is a clean one. `fallbackToDestructiveMigration()` stays forbidden. The migration harness is KI-TBD #4, required before the first release.

## Logging

`[BipSale][Sale][CHECKOUT]` covers every cart mutation and every finalize leg — see [KI-04](KI-04-LOG-FILTERS.md). **No customer field is ever logged**; line counts and totals carry the diagnosis and identify nobody ([`CORE_RULES §8.3`](../rules/CORE_RULES.md)).

## Test targets

`SaleTotalsTest` (10) · `SaleItemTest` (10) · `FinalizeSaleUseCaseTest` (8) · `AddProductByCodeUseCaseTest` (5) · `AddProductByQrUseCaseTest` (8) · `SalesViewModelTest` (20) · `SaleMapperTest` (10).

The one that matters most: `finalizing persists the total the cart was showing` — it asserts the persisted sale equals the displayed state, which is the whole point of rule 1.
