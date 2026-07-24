# Test Coverage Inventory — BipSale

Per-module test inventory. Counts are `*.kt` files hosting `@Test` methods. IDE-generated `ExampleUnitTest` / `ExampleInstrumentedTest` placeholders are excluded from the counts.

**Last verified:** 2026-07-21

> ⚠️ **The project currently has no real test coverage.** The only test files present are the two IDE-generated placeholders in `:app`. Every row below is zero by fact, not by omission.

| Module | JVM test files | Instrumented test files | Notes |
|---|---|---|---|
| `:app` | 0 | 0 | Only `ExampleUnitTest` / `ExampleInstrumentedTest`. Export-to-Excel flow untested. |
| `:core:domain` | 0 | 0 | Pure Kotlin use cases — the cheapest and highest-value place to start. |
| `:core:data` | 0 | 0 | Room DAOs, mappers, `ProductRepositoryImpl`, `SaleRepositoryImpl` all untested. **No Room migration test exists** — `CORE_RULES §13` requires one per schema change. |
| `:core:ui` | 0 | 0 | Theme + `UiText` only. |
| `:core:navigation` | 0 | 0 | Route definitions. |
| `:core:qrcode` | 0 | 0 | QR encode/decode utilities — pure functions, trivially testable. |
| `:feature:sales` | 0 | 0 | **Checkout is the money path** (cart → CPF → `SaleRepositoryImpl`) and has no coverage at all. |
| `:feature:products` | 0 | 0 | CRUD + QR generation. |
| `:feature:history` | 0 | 0 | History + search. |
| `:feature:qrcode` | 0 | 0 | CameraX scanning — hardware-dependent; instrumented only. |

**Priority gaps** (highest value first):
1. **Checkout total/cart arithmetic** (`:feature:sales`) — a wrong total is a wrong charge. Pure-logic tests, no Android needed.
2. **`SaleRepositoryImpl` + `ProductRepositoryImpl`** — both already have `[BipSale][Sale]` / `[BipSale][Product]` error breadcrumbs proving the failure paths matter; none are pinned by a test.
3. **Room migration harness** — required before the first schema change ships (`CORE_RULES §13`).
4. **`:core:qrcode`** — pure functions, cheapest possible coverage.

**Verification commands:**

| Command | Purpose |
|---|---|
| `./gradlew test` | All JVM unit suites |
| `./gradlew connectedAndroidTest` | Instrumented suites on an attached device/emulator |
| `./gradlew detekt ktlintCheck` | Static analysis |
| `./gradlew koverHtmlReport` | Coverage report |

> **Update protocol:** on every test-coverage boundary, re-count the files and update the rows. If a module's count grows by 1+ without an entry here, the row is stale.
