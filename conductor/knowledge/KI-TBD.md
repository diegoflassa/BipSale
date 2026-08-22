# KI-TBD: Future Work Index

**Scope:** Whole project — aggregates every known deferred item, backlog item, and not-yet-implemented rule
**Status:** TBD — planning reference only, no code here
**Last verified:** 2026-08-22

## Purpose

Single landing page for "what's left." Each row points to where the authoritative detail lives — this file indexes, it does not duplicate. When an item ships, remove its row and strip the "deferred/TBD" framing at the source.

## Open Items

| # | Item | Detail lives in | Source location | Executable? | Blocker(s) |
|---|---|---|---|---|---|
| 1 | **Coverage still absent in 8 of 10 modules** — `:core:domain` and `:core:qrcode` now have suites | [TEST_COVERAGE.md](TEST_COVERAGE.md) | all other modules | ✅ Executable | None — highest-priority item in the project |
| 2 | Checkout total / cart arithmetic untested | [TEST_COVERAGE.md](TEST_COVERAGE.md) | `feature/sales/` | ✅ Executable | None — pure logic, no Android needed. A wrong total is a wrong charge. |
| 3 | `SaleRepositoryImpl` / `ProductRepositoryImpl` failure paths untested | [TEST_COVERAGE.md](TEST_COVERAGE.md), [KI-04](KI-04-LOG-FILTERS.md) | `core/data/repository/` | ✅ Executable | None — both already log `[BipSale][Sale]` / `[BipSale][Product]` on failure, so the paths are known to matter |
| 4 | No Room migration harness | [CORE_RULES §13](../rules/CORE_RULES.md) | `core/data/` | ⏸️ Deferred | App not yet released — no users to migrate. Required before **first release** so post-release schema changes don't wipe sales data |
| 5 | `:core:qrcode` bitmap rendering untested — grid maths now covered | [TEST_COVERAGE.md](TEST_COVERAGE.md) | `core/qrcode/` | ⏸️ Partially blocked | `QrGenerator` / `QrLabelSheetRenderer` need real Android graphics, so instrumented only |
| 6 | No Compose UI tests for any screen | [TEST_COVERAGE.md](TEST_COVERAGE.md) | all `feature/*/` | ⏸️ Partially blocked | Screens must satisfy [`INSTRUMENTED_TEST_STANDARD.md`](../rules/INSTRUMENTED_TEST_STANDARD.md) (two-layer split + test tags) first |
| 7 | Checkout flow still has no trace logging | [KI-04](KI-04-LOG-FILTERS.md) | `feature/sales/` | ⏸️ Judgement call | The product and QR-export flows now carry debug breadcrumbs; the money path does not |
| 8 | No `CHANGELOG.md` history before 2026-07-21 | [CORE_RULES §14](../rules/CORE_RULES.md) | `CHANGELOG.md` | ⏸️ Won't fix | Pre-existing work predates the rule; start from Unreleased |
| 9 | Static analysis covers only `:app` | [TEST_COVERAGE.md](TEST_COVERAGE.md) | `build-logic/`, all library modules | ✅ Executable | None — detekt is applied only in `app/build.gradle.kts`, and the ktlint plugin is declared `apply false` at the root and never applied, so `ktlintCheck` does not exist. Wiring detekt into `android-library-convention` surfaces ~15 mostly-`allRules` findings across the library modules |
| 11 | `android:allowBackup` is on for release without migrations | [CORE_RULES §13](../rules/CORE_RULES.md) | `app/src/main/AndroidManifest.xml` | ⏸️ Deferred | Blocked on #4; both deferred until pre-release. No users to migrate yet |
| 10 | Discount is display-only | — | `feature/sales/SalesScreen.kt` | ✅ Executable | None — the ViewModel already applies `UpdateDiscount` and the sale persists `discountPercentage`, but the percentage in the bottom bar is plain text with no way to change it. `discount_dialog_title` / `discount_dialog_hint` exist for a dialog that was never built |
| 12 | Alternative ways to make a sale: Select product from a list, or input its code manually | — | `feature/sales/SalesScreen.kt` | ✅ Executable | None — currently products can only be added by scanning a QR code |
| 13 | Product-level discounts | — | `feature/sales/SalesScreen.kt` | ✅ Executable | None — add a way to apply a discount to a specific product by percentage of the price or by a given fixed value |

## Proposed Execution Order

### Phase 1 — Safety-Critical Tests (money path first)

| Order | Item | Rationale |
|-------|------|-----------|
| 1st | **#2** Checkout total / cart arithmetic | A wrong total is a wrong charge — highest-risk untested logic |
| 2nd | **#3** Repository failure paths | Data-layer failures that swallow errors silently; already logged, just untested |
| 3rd | **#7** Checkout trace logging | The money path needs diagnostic breadcrumbs before new features touch it |

### Phase 2 — Infrastructure Hardening

| Order | Item | Rationale |
|-------|------|-----------|
| 4th | **#9** Static analysis across all modules | Catches issues early in all subsequent work |
| 5th | **#1** Broader test coverage | Can be tackled incrementally module-by-module |

### Phase 3 — Features

| Order | Item | Rationale |
|-------|------|-----------|
| 8th | **#10** Discount dialog (sale-level) | Existing infra (`UpdateDiscount`, `discountPercentage`) is already wired; just needs UI |
| 9th | **#13** Product-level discounts | Extends #10 with per-item discount by percentage or fixed value |
| 10th | **#12** Alternative sale methods | List selection and manual code input — broadens how products enter the cart |

### Phase 4 — Pre-Release / Blocked / Deferred

| Order | Item | Rationale |
|-------|------|-----------|
| 10th | **#4** Room migration harness | No users yet — required before first release so post-release schema changes are safe |
| 11th | **#11** `allowBackup` safety | One-liner once #4 lands; same pre-release gate |
| 12th | **#5** QR bitmap rendering tests | Instrumented-only; schedule when a device/CI runner is available |
| 13th | **#6** Compose UI tests | Screens need two-layer split + test tags first |
| — | **#8** CHANGELOG backfill | Won't fix — predates the rule |

## Notes

- Items become plans via [`CORE_RULES §7`](../rules/CORE_RULES.md) once work starts — this index is not an execution plan.
- A row here is a **pointer**. Never let it become the only place a rule is written down; the detail belongs in the KI or rules file named in the "Detail lives in" column.
