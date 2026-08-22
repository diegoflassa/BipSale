# Test Coverage Inventory — BipSale

Per-module test inventory. Counts are `*.kt` files hosting `@Test` methods. IDE-generated `ExampleUnitTest` / `ExampleInstrumentedTest` placeholders are excluded from the counts.

**Last verified:** 2026-08-22

> ⚠️ **Coverage is thin but no longer zero.** `:core:domain` and `:core:qrcode` have their first suites; every other module is still uncovered. A green `./gradlew test` proves only what the rows below claim.

| Module | JVM test files | Instrumented test files | Notes |
|---|---|---|---|
| `:app` | 0 | 0 | Only `ExampleUnitTest` / `ExampleInstrumentedTest`. Export-to-Excel flow untested. |
| `:core:domain` | 2 | 0 | `PriceInputTest` (9 tests) pins comma/dot decimal parsing and the zero-price rejection; `SaveProductUseCaseTest` (7 tests) pins code/name/price validation and QR payload shape. `FinalizeSaleUseCase` and `AddProductByQrUseCase` remain uncovered. |
| `:core:data` | 0 | 1 | `ProductImageStoreImplTest` (6 tests) pins image import against a real `ContentResolver`, downscaling, code sanitising, per-pick naming and deletion. Room DAOs, mappers, `ProductRepositoryImpl`, `SaleRepositoryImpl` still untested. **No Room migration test exists** — `CORE_RULES §13` requires one per schema change; the v1 schema is now exported, so the harness is unblocked. |
| `:core:ui` | 0 | 0 | Theme + `UiText` only. |
| `:core:navigation` | 0 | 0 | Route definitions. |
| `:core:qrcode` | 1 | 0 | `QrLabelSheetLayoutTest` (9 tests) pins the A4 4x5 grid, pagination, cell geometry and the tiny-paper guard. `QrGenerator` and `QrLabelSheetRenderer` need Android graphics, so they stay instrumented-only. |
| `:feature:sales` | 0 | 0 | **Checkout is the money path** (cart → CPF → `SaleRepositoryImpl`) and has no coverage at all. |
| `:feature:products` | 0 | 0 | CRUD + QR generation. |
| `:feature:history` | 0 | 0 | History + search. |
| `:feature:qrcode` | 0 | 0 | CameraX scanning — hardware-dependent; instrumented only. |

**Priority gaps** (highest value first):
1. **Checkout total/cart arithmetic** (`:feature:sales`) — a wrong total is a wrong charge. Pure-logic tests, no Android needed.
2. **`SaleRepositoryImpl` + `ProductRepositoryImpl`** — both already have `[BipSale][Sale]` / `[BipSale][Product]` error breadcrumbs proving the failure paths matter; none are pinned by a test.
3. **`ProductViewModel`** — now owns price validation, currency formatting and image lifecycle; none of it is pinned. `ProductImageStore` is an interface, so a fake makes this a plain JVM suite.
4. **Room migration harness** — required before the next schema change ships (`CORE_RULES §13`). Now unblocked: `exportSchema = true` and `core/data/schemas/…/1.json` is committed.
5. **EXIF rotation** — the one part of `ProductImageStoreImpl` still unpinned; needs a fixture photo carrying an orientation tag.

**Verification commands:**

| Command | Purpose |
|---|---|
| `./gradlew test` | All JVM unit suites |
| `./gradlew :core:domain:test :core:qrcode:testDebugUnitTest` | The JVM suites that currently exist |
| `./gradlew :core:data:connectedDebugAndroidTest` | Image store suite (needs a device) |
| `./gradlew connectedAndroidTest` | Instrumented suites on an attached device/emulator |
| `./gradlew :app:detekt` | Static analysis — **only `:app` has detekt wired; there is no `ktlintCheck` task in this build** |
| `./gradlew koverHtmlReport` | Coverage report |

> **Update protocol:** on every test-coverage boundary, re-count the files and update the rows. If a module's count grows by 1+ without an entry here, the row is stale.
