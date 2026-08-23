# Test Coverage Inventory — BipSale

Per-module test inventory. Counts are `*.kt` files hosting `@Test` methods. IDE-generated `ExampleUnitTest` / `ExampleInstrumentedTest` placeholders are excluded from the counts.

**Last verified:** 2026-08-22

> **Every module carries a JVM suite.** 182 JVM tests, 13 instrumented data tests, and 3 Compose UI test suites (12 + 19 + 31 instrumented tests across `:feature:history`, `:feature:sales` and `:feature:products`). The gaps that remain are `:app` screens and Android-graphics work.

| Module | JVM test files | Instrumented test files | Notes |
|---|---|---|---|
| `:app` | 1 | 0 | `BackupViewModelTest` (9 tests) pins archive write, share staging, the inspect-before-restore gate, cancellation and the unreadable-archive path. Export-to-Excel UI untested. |
| `:core:domain` | 6 | 0 | `SaleTotalsTest` (10) and `SaleItemTest` (10) pin the whole money path — line gross/discount/net, sale-level percentage ordering, HALF_UP rounding to cents, and every rejected discount. `FinalizeSaleUseCaseTest` (8), `AddProductByQrUseCaseTest` (8), `AddProductByCodeUseCaseTest` (5), `SaveProductUseCaseTest` (7), `PriceInputTest` (9). |
| `:core:data` | 4 | 2 | `SaleRepositoryImplTest` (10) and `ProductRepositoryImplTest` (9) pin which failures are swallowed and which rethrow, driven by fake DAOs that throw. `SaleMapperTest` (10) and `ProductMapperTest` (4) pin the entity↔domain round trip, the discount flattening and the coercion of a corrupt stored discount. Instrumented: `ProductImageStoreImplTest` (6), `BackupRepositoryImplTest` (7). **No Room migration test** — deferred, see KI-TBD #4. |
| `:core:ui` | 1 | 0 | `UiTextTest` (8) pins `StringResource` equality, which decides whether a screen holding one recomposes on every emission. |
| `:core:navigation` | 0 | 0 | Route definitions only — nothing to assert that the compiler does not. |
| `:core:qrcode` | 1 | 0 | `QrLabelSheetLayoutTest` (9) pins the A4 4x5 grid, pagination, cell geometry and the tiny-paper guard. `QrGenerator` and `QrLabelSheetRenderer` need Android graphics, so they stay instrumented-only. |
| `:core:utils` | 1 | 0 | `ExcelExporterTest` (7) pins the header, one row per sale item, and that values stay under the header they belong to. |
| `:feature:sales` | 1 | 1 | `SalesViewModelTest` (20) pins the cart end to end. `SalesScreenContentInstrumentedTest` (19) drives the stateless composable. |
| `:feature:products` | 1 | 1 | `ProductViewModelTest` (24) pins list load, price parsing, image lifecycle. `ProductScreenContentInstrumentedTest` (31) drives the stateless composable. |
| `:feature:history` | 1 | 1 | `HistoryViewModelTest` (10) pins search, date filtering, load failure, selection. `HistoryScreenContentInstrumentedTest` (12) drives the stateless composable. |

**Priority gaps** (highest value first):
1. **`:app` screen Compose UI tests** — `BackupScreen`, `ExportScreen` and `DashboardScreen` still lack the two-layer split and test tags needed for instrumented testing (KI-TBD #6).
2. **Room migration harness** — required before the first schema change ships to a released build (`CORE_RULES §13`, KI-TBD #4).
3. **`:core:qrcode` bitmap rendering** — `QrGenerator` / `QrLabelSheetRenderer` need real Android graphics, so instrumented only (KI-TBD #5).
4. **EXIF rotation** — the one part of `ProductImageStoreImpl` still unpinned; needs a fixture photo carrying an orientation tag.

**Verification commands:**

| Command | Purpose |
|---|---|
| `./gradlew test` | All JVM unit suites (178 tests) |
| `./gradlew :core:domain:test` | The money path on its own |
| `./gradlew :core:data:connectedDebugAndroidTest` | Image store + backup suites (needs a device) |
| `./gradlew connectedAndroidTest` | Instrumented suites on an attached device/emulator |
| `./gradlew detekt` | Static analysis — **every module**, via `detekt-convention` in `build-logic` |
| `./gradlew koverHtmlReport` | Coverage report |

> **Update protocol:** on every test-coverage boundary, re-count the files and update the rows. If a module's count grows by 1+ without an entry here, the row is stale.
