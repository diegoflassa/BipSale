# Plan — Execute every unblocked KI-TBD item

**Status:** 🟡 Active — every code task is done; only the debug deploy is outstanding
**Started:** 2026-08-22
**Source backlog:** [`KI-TBD.md`](../../knowledge/KI-TBD.md) — this plan closes every row marked ✅ Executable.
**Contract:** [`KI-07`](../../knowledge/KI-07-SALES-CART-AND-DISCOUNTS.md) is the spec this work produced.

## State

| Task | KI-TBD # | State |
|---|---|---|
| T1 cart arithmetic | #2 | ✅ Done — row removed from KI-TBD |
| T2 repository failure paths | #3 | ✅ Done — row removed |
| T3 checkout trace logging | #7 | ✅ Done — row removed, filter catalogued in KI-04 |
| T7 static analysis everywhere | #9 | ✅ Done — row removed, `./gradlew detekt` clean on all 10 modules |
| T8 broader coverage | #1 | ✅ Done — row removed, 182 tests across all 10 modules |
| T4 sale-level discount dialog | #10 | ✅ Done — row removed |
| T5 per-line discounts | #13 | ✅ Done — row removed |
| T6 catalogue pick + typed code | #12 | ✅ Done — row removed |
| Shared back-arrow header | — | ✅ Done — user request, `core:ui`'s `BipSaleTopAppBar` on every screen |
| Debug deploy | — | ⛔ **Not run.** The Bash tool lost its shell mid-session (`bash.exe` no longer resolvable), so Gradle and adb could not be invoked. Nothing about the code blocks it |

**To finish:** `./gradlew assembleDebug`, then `adb uninstall dev.diegoflassa.bipsale` followed by `adb install` (a **clean** install — the `sale_items` schema moved and no migration ships), then `./gradlew appDistributionUploadDebug`.

## Scope

| KI-TBD # | Item | Task |
|---|---|---|
| #2 | Checkout total / cart arithmetic untested | T1 |
| #3 | Repository failure paths untested | T2 |
| #7 | Checkout flow has no trace logging | T3 — pulled in by `LOGGING_RULES §8.2`, since T4–T6 rewrite the money path |
| #10 | Discount is display-only | T4 |
| #13 | Product-level discounts | T5 |
| #12 | Alternative ways to make a sale | T6 |
| #9 | Static analysis covers only `:app` | T7 |
| #1 | Coverage absent in 8 of 10 modules | T8 |
| #4 | No Room migration harness | Out of scope by the user's call — the app is unreleased, so there is nobody to migrate |

Out of scope (still blocked at the source): #5 (needs a device), #6 (screens need the two-layer split first), #8 (won't fix), #11 (blocked on release gating, not on #4 alone).

## Execution order

`KI-TBD.md`'s own proposed order, followed as written: **#2 → #3 → #7 → #9 → #1 → #10 → #13 → #12**.
The `SaleItem` / `ItemDiscount` domain shape from #13 lands first regardless, because #2 pins the arithmetic and would otherwise be written twice.

## Tasks

### T1 — Pin the cart arithmetic (#2)
The money maths lives twice: `SalesViewModel.recalculate()` and `FinalizeSaleUseCase`. Two implementations of one number is the defect — the cart can show a total the receipt does not.
- Extract one pure domain function `saleTotals(items, discountPercentage)` in `core:domain`; both call sites use it.
- Round monetary results HALF_UP to 2 decimals — a `Double` sum persists `80.91000000000001` as a sale's `finalAmount` today.
- Suites: `SaleTotalsTest`, `FinalizeSaleUseCaseTest`, `AddProductByQrUseCaseTest` (`:core:domain`), `SalesViewModelTest` (`:feature:sales`).

### T2 — Pin the repository failure paths (#3)
JVM suites in `:core:data` driving fake DAOs that throw: `ProductRepositoryImplTest`, `SaleRepositoryImplTest`, `SaleMapperTest`, `ProductMapperTest`.
Pins: `getProductByCode` swallows and returns null; the write paths rethrow; `getSaleById` swallows and returns null; `insertFullSale` propagates.

### T3 — Checkout trace logging (#7)
`[BipSale][Sale][CHECKOUT]` breadcrumbs across `SalesViewModel` + `FinalizeSaleUseCase`: item added/removed, discount applied, finalize entered, persisted, failed. Redaction per `LOGGING_RULES §8.3` — never the CPF or the customer name, always the counts and the total. Catalogue the filter in KI-04 in the same turn.

### T4 — Sale-level discount dialog (#10)
`feature/sales/components/DiscountDialog.kt` (Material 3 `AlertDialog`), wired to the existing `UpdateDiscount` intent. Rejects anything outside 0–100.

### T5 — Product-level discounts (#13)
- `core:domain`: `ItemDiscount` sealed hierarchy (`None` / `Percentage` / `Amount`) — the cases carry payloads, so `CORE_RULES §5.3` puts it on the sealed side. `SaleItem` gains `id` (stable line identity) + `discount`, and the per-line `grossAmount` / `discountAmount` / `netAmount`.
- `core:data`: `sale_items` gains `discountType` + `discountValue`; the sealed type is flattened at the mapper seam, never stored as a sealed type.
- `feature:sales`: `ItemDiscountDialog` + a per-row action.

### T6 — Alternative ways to add a product (#12)
- `AddProductByCodeUseCase` in `core:domain`.
- `ProductPickerDialog` (tap a registered product) and `ManualCodeDialog` (type a code) in `feature/sales/components/`.
- Replaces the dead `AddManualItem` intent, which no screen ever sent.

### T7 — Static analysis across all modules (#9)
`detekt-convention.gradle.kts` in `build-logic`, applied by `android-library-convention`, `android-application-convention` and `:core:domain`. Fix every finding it surfaces.

### T8 — Broader coverage (#1)
`ProductViewModelTest` (`:feature:products`), `HistoryViewModelTest` (`:feature:history`), `UiTextTest` (`:core:ui`), plus whatever T1/T2/T5 land.

### T9 — Schema move for T5 (no migration)
DB `version = 2`, exported `2.json`. **No** `Migration` and **no** `MigrationTestHelper` suite: the app is unreleased, so dev-time schema churn is reset by clearing app data, which is what `CORE_RULES §13.4` prescribes and is why the debug install below is a clean one. `fallbackToDestructiveMigration()` stays forbidden regardless.
Backup: `BackupSaleItem` gains the two fields with defaults so an archive written before the change still restores; `SCHEMA_VERSION` → 2.

## Testing checklist

- [ ] `./gradlew test` green
- [ ] `./gradlew detekt` green across every module
- [ ] `./gradlew assembleDebug` green
- [ ] Debug APK **clean-installed** (uninstall first — the schema moved and there is no migration) on the connected device, and uploaded to Firebase App Distribution

## Doc sync (same turn, `CORE_RULES §6`)

- `KI-TBD.md` — drop every closed row, re-cut the execution order
- `TEST_COVERAGE.md` — re-count every module
- `KI-04-LOG-FILTERS.md` — `[BipSale][Sale][CHECKOUT]` row
- New `KI-07-SALES-CART-AND-DISCOUNTS.md` — the cart/discount contract
- `CHANGELOG.md` — one line per work unit

## Blockers

None. Every item above is reachable without a release, a backend, or a CI runner.
