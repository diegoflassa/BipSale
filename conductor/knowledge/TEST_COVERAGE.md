# Test Coverage Inventory — BipSale

Per-module test inventory. Counts are `*.kt` files hosting `@Test` methods. IDE-generated `ExampleUnitTest` / `ExampleInstrumentedTest` placeholders are excluded from the counts.

**Last verified:** 2026-09-02

> **Every module carries a JVM suite.** 287 JVM tests and 125 instrumented tests: 18 data-layer, 21 QR-rendering, and 86 Compose UI across `:feature:history` (13), `:feature:sales` (20), `:feature:products` (35) and `:app` (18). Every `:app` screen now has a suite.

| Module | JVM test files | Instrumented test files | Notes |
|---|---|---|---|
| `:app` | 1 | 4 | `BackupViewModelTest` (9) pins archive write, share staging, the inspect-before-restore gate, cancellation and the unreadable-archive path. `BackupScreenContentInstrumentedTest` (4) pins that the summary card fills the content width. `ExportScreenContentInstrumentedTest` (6) pins the idle render, that export fires once, and that the button is disabled — and inert — mid-export. `DashboardScreenContentInstrumentedTest` (3) pins that all six entries are reachable by scrolling and that each fires its own callback, which is what catches two cards wired to the same destination. `SettingsScreenContentInstrumentedTest` (5) pins that the two label type-size steppers move their own setting and stop at their bounds. |
| `:core:domain` | 11 | 0 | `SaleTotalsTest` (10), `SaleItemTest` (10) and `SalesSummaryTest` (9) pin the whole money path — line gross/discount/net, sale-level percentage ordering, HALF_UP rounding to cents, and every rejected discount. `PriceInputTest` (13), `FinalizeSaleUseCaseTest` (8), `AddProductByQrUseCaseTest` (8), `SaveProductUseCaseTest` (7), `AddProductByCodeUseCaseTest` (5), `ExportSalesUseCaseTest` (5) — the last pins that an export with no sales, or with sales carrying no lines, is refused rather than written as a header-only file. `SalesSummaryTest` also pins that the payment-method breakdown adds back up to the net. `PixPayloadTest` (15) pins the PIX payload against a reference string byte for byte — field order, lengths and the CRC variant. `PixDefaultsTest` (1) pins the shop's fixed key. |
| `:core:data` | 4 | 3 | `SaleRepositoryImplTest` (11) and `ProductRepositoryImplTest` (9) pin which failures are swallowed and which rethrow, driven by fake DAOs that throw. `SaleMapperTest` (10) and `ProductMapperTest` (4) pin the entity↔domain round trip, the discount flattening and the coercion of a corrupt stored discount. `SaleRepositoryImplTest` also pins that stock moves in the same write as the sale. Instrumented: `ProductImageStoreImplTest` (6), `BackupRepositoryImplTest` (9) — the latter also pins that every configured setting round-trips and that a value carried out of range is clamped on restore — and `BipSaleDatabaseMigrationTest` (3), the `CORE_RULES` §13 harness. |
| `:core:ui` | 1 | 0 | `UiTextTest` (8) pins `StringResource` equality, which decides whether a screen holding one recomposes on every emission. |
| `:core:navigation` | 0 | 0 | Route definitions only — nothing to assert that the compiler does not. |
| `:core:qrcode` | 2 | 2 | `QrLabelSheetLayoutTest` (11) pins the A4 grid, pagination, cell geometry, the requested-column override and the tiny-paper guard. `QrLabelTypographyTest` (6) pins that line height and the price shrink floor derive from the configured size and that the renderer default matches what settings hand out. `QrGenerator` and `QrLabelSheetRenderer` need real Android graphics, so `QrGeneratorTest` (9) and `QrLabelSheetRendererTest` (12) are instrumented — the latter also pins that a larger configured type genuinely renders differently and that the code still scans at the 24 pt maximum. |
| `:core:utils` | 4 | 0 | `ExcelExporterTest` (16) pins the header, one row per sale item, the date/quantity/line-discount columns, that values stay under the header they belong to, that the sale total is written once per sale so summing the column cannot double count, and the TOTAIS row plus the Resumo sheet. `ProductSheetTest` (11) pins the import template round trip, numeric barcodes read as digits, comma decimals, and that a bad row is reported while the good rows still import. `LogChunkerTest` (5) and `LogRedactionTest` (7) pin the byte-budget split and the release redaction helpers. |
| `:feature:sales` | 1 | 1 | `SalesViewModelTest` (32) pins the cart end to end, plus the payment-method gate and the whole PIX path — default discount, payload following the total, and the amount-carrying toggle. `SalesScreenContentInstrumentedTest` (20) drives the stateless composable. |
| `:feature:products` | 1 | 2 | `ProductViewModelTest` (36) pins list load, price parsing, image lifecycle, the spreadsheet import flow, stock-driven label runs, the import overwrite confirmation, and that the configured label type sizes reach both the print effect and the edit-screen preview. `ProductListContentInstrumentedTest` (18) and `AddEditProductContentInstrumentedTest` (17) drive the stateless composables; the former covers the import overwrite dialog. |
| `:feature:history` | 1 | 1 | `HistoryViewModelTest` (21) pins search, date filtering, load failure, selection, and the whole export flow — nothing to write, destination requested before any write, scope filtering, an orphan destination, cancellation and a write failure. `HistoryScreenContentInstrumentedTest` (13) drives the stateless composable. |

**Priority gaps** (highest value first):
1. **EXIF rotation** — the one part of `ProductImageStoreImpl` still unpinned; needs a fixture photo carrying an orientation tag.
2. **Backup share staging and `formatVersion` rejection** — covered at the ViewModel level but not end to end through the repository.

**Verification commands:**

| Command | Purpose |
|---|---|
| `./gradlew test` | All JVM unit suites (287 tests) |
| `./gradlew :core:domain:test` | The money path on its own |
| `./gradlew :core:data:connectedDebugAndroidTest` | Image store, backup and migration suites (needs a device) |
| `./gradlew connectedAndroidTest` | All instrumented suites (125 tests) on an attached device/emulator |
| `./gradlew assembleDebugAndroidTest` | Compile every instrumented suite without a device — catches a screen signature that changed without its test |
| `./gradlew detekt` | Static analysis — **every module**, via `detekt-convention` in `build-logic` |
| `./gradlew koverHtmlReport` | Coverage report |

> **Update protocol:** on every test-coverage boundary, re-count the files and update the rows. If a module's count grows by 1+ without an entry here, the row is stale.
>
> **Compile the instrumented suites even when you cannot run them.** `connectedAndroidTest` needs hardware, so a screen whose signature changed without its test can sit broken indefinitely — `assembleDebugAndroidTest` is the cheap guard and belongs in the same pass as `test` and `detekt`.
