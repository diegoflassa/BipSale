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
| 4 | No Room migration harness | [CORE_RULES §13](../rules/CORE_RULES.md) | `core/data/` | ✅ Executable | None — `exportSchema = true` and `core/data/schemas/…/1.json` are committed, so `MigrationTestHelper` can now run against a real schema set. Required before the next schema change ships; this DB holds sales records |
| 5 | `:core:qrcode` bitmap rendering untested — grid maths now covered | [TEST_COVERAGE.md](TEST_COVERAGE.md) | `core/qrcode/` | ⏸️ Partially blocked | `QrGenerator` / `QrLabelSheetRenderer` need real Android graphics, so instrumented only |
| 6 | No Compose UI tests for any screen | [TEST_COVERAGE.md](TEST_COVERAGE.md) | all `feature/*/` | ⏸️ Partially blocked | Screens must satisfy [`INSTRUMENTED_TEST_STANDARD.md`](../rules/INSTRUMENTED_TEST_STANDARD.md) (two-layer split + test tags) first |
| 7 | Checkout flow still has no trace logging | [KI-04](KI-04-LOG-FILTERS.md) | `feature/sales/` | ⏸️ Judgement call | The product and QR-export flows now carry debug breadcrumbs; the money path does not |
| 8 | No `CHANGELOG.md` history before 2026-07-21 | [CORE_RULES §14](../rules/CORE_RULES.md) | `CHANGELOG.md` | ⏸️ Won't fix | Pre-existing work predates the rule; start from Unreleased |
| 9 | Static analysis covers only `:app` | [TEST_COVERAGE.md](TEST_COVERAGE.md) | `build-logic/`, all library modules | ✅ Executable | None — detekt is applied only in `app/build.gradle.kts`, and the ktlint plugin is declared `apply false` at the root and never applied, so `ktlintCheck` does not exist. Wiring detekt into `android-library-convention` surfaces ~15 mostly-`allRules` findings across the library modules |
| 11 | `android:allowBackup` is on for release without migrations | [CORE_RULES §13](../rules/CORE_RULES.md) | `app/src/main/AndroidManifest.xml` | ⏸️ Blocked | Blocked on item #4. Debug has backup off so local schema churn is not resurrected; release has it on so sales history survives a device migration. Release is only safe once migrations exist — until then a restore onto a newer build crashes on the identity-hash mismatch |
| 10 | Discount is display-only | — | `feature/sales/SalesScreen.kt` | ✅ Executable | None — the ViewModel already applies `UpdateDiscount` and the sale persists `discountPercentage`, but the percentage in the bottom bar is plain text with no way to change it. `discount_dialog_title` / `discount_dialog_hint` exist for a dialog that was never built |

## Ready to Execute Now

**#1, #2, #3, #4, #9, #10** are unblocked. Suggested order: #2 (money path) → #3 (known failure paths) → #9 (stops new code shipping unchecked) → #10 (cheap tidy-up).

## Notes

- Items become plans via [`CORE_RULES §7`](../rules/CORE_RULES.md) once work starts — this index is not an execution plan.
- A row here is a **pointer**. Never let it become the only place a rule is written down; the detail belongs in the KI or rules file named in the "Detail lives in" column.
