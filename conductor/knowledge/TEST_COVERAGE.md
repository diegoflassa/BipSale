# Test Coverage Inventory — BipSale

Per-module test inventory. Counts are `*.kt` files hosting `@Test` methods. IDE-generated `ExampleUnitTest` / `ExampleInstrumentedTest` placeholders are excluded from the counts.

**Last verified:** 2026-08-23

> **Every module carries a JVM suite.** 211 JVM tests and 95 instrumented tests: 13 data-layer, 19 QR-rendering, and 63 Compose UI across `:feature:history` (13), `:feature:sales` (19) and `:feature:products` (31). The gap that remains is the `:app` screens.

| Module | JVM test files | Instrumented test files | Notes |
|---|---|---|---|
| `:app` | 1 | 0 | `BackupViewModelTest` (9) pins archive write, share staging, the inspect-before-restore gate, cancellation and the unreadable-archive path. The `:app` screens themselves have no UI suite. |
| `:core:domain` | 9 | 0 | `SaleTotalsTest` (10), `SaleItemTest` (10) and `SalesSummaryTest` (9) pin the whole money path — line gross/discount/net, sale-level percentage ordering, HALF_UP rounding to cents, and every rejected discount. `PriceInputTest` (13), `FinalizeSaleUseCaseTest` (8), `AddProductByQrUseCaseTest` (8), `SaveProductUseCaseTest` (7), `AddProductByCodeUseCaseTest` (5), `ExportSalesUseCaseTest` (5) — the last pins that an export with no sales, or with sales carrying no lines, is refused rather than written as a header-only file. `SalesSummaryTest` also pins that the payment-method breakdown adds back up to the net. |
| `:core:data` | 4 | 2 | `SaleRepositoryImplTest` (10) and `ProductRepositoryImplTest` (9) pin which failures are swallowed and which rethrow, driven by fake DAOs that throw. `SaleMapperTest` (10) and `ProductMapperTest` (4) pin the entity↔domain round trip, the discount flattening and the coercion of a corrupt stored discount. Instrumented: `ProductImageStoreImplTest` (6), `BackupRepositoryImplTest` (7). **No Room migration test** — deferred, see KI-TBD #4. |
| `:core:ui` | 1 | 0 | `UiTextTest` (8) pins `StringResource` equality, which decides whether a screen holding one recomposes on every emission. |
| `:core:navigation` | 0 | 0 | Route definitions only — nothing to assert that the compiler does not. |
| `:core:qrcode` | 1 | 2 | `QrLabelSheetLayoutTest` (9) pins the A4 4x5 grid, pagination, cell geometry and the tiny-paper guard. `QrGenerator` and `QrLabelSheetRenderer` need real Android graphics, so `QrGeneratorTest` (9) and `QrLabelSheetRendererTest` (10) are instrumented. |
| `:core:utils` | 1 | 0 | `ExcelExporterTest` (16) pins the header, one row per sale item, the date/quantity/line-discount columns, that values stay under the header they belong to, that the sale total is written once per sale so summing the column cannot double count, and the TOTAIS row plus the Resumo sheet. |
| `:feature:sales` | 1 | 1 | `SalesViewModelTest` (20) pins the cart end to end. `SalesScreenContentInstrumentedTest` (19) drives the stateless composable. |
| `:feature:products` | 1 | 2 | `ProductViewModelTest` (24) pins list load, price parsing, image lifecycle. `ProductListContentInstrumentedTest` (14) and `AddEditProductContentInstrumentedTest` (17) drive the stateless composables. |
| `:feature:history` | 1 | 1 | `HistoryViewModelTest` (17) pins search, date filtering, load failure, selection, and the whole export flow — nothing to write, destination requested before any write, scope filtering, an orphan destination, cancellation and a write failure. `HistoryScreenContentInstrumentedTest` (13) drives the stateless composable. |

**Priority gaps** (highest value first):
1. **`:app` screen Compose UI tests** — `BackupScreen`, `ExportScreen` and `DashboardScreen` have no test tags, and their `ScreenContent` halves are `private` rather than `internal`, so nothing can drive them (KI-TBD #6).
2. **Room migration harness** — required before the first schema change ships to a released build (`CORE_RULES §13`, KI-TBD #4).
3. **EXIF rotation** — the one part of `ProductImageStoreImpl` still unpinned; needs a fixture photo carrying an orientation tag.

**Verification commands:**

| Command | Purpose |
|---|---|
| `./gradlew test` | All JVM unit suites (211 tests) |
| `./gradlew :core:domain:test` | The money path on its own |
| `./gradlew :core:data:connectedDebugAndroidTest` | Image store + backup suites (needs a device) |
| `./gradlew connectedAndroidTest` | Instrumented suites on an attached device/emulator |
| `./gradlew detekt` | Static analysis — **every module**, via `detekt-convention` in `build-logic` |
| `./gradlew koverHtmlReport` | Coverage report |

> **Update protocol:** on every test-coverage boundary, re-count the files and update the rows. If a module's count grows by 1+ without an entry here, the row is stale.
