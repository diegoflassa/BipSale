# KI-TBD: Future Work Index

**Scope:** Whole project — aggregates every known deferred item, backlog item, and not-yet-implemented rule
**Status:** TBD — planning reference only, no code here
**Last verified:** 2026-07-21

## Purpose

Single landing page for "what's left." Each row points to where the authoritative detail lives — this file indexes, it does not duplicate. When an item ships, remove its row and strip the "deferred/TBD" framing at the source.

## Open Items

| # | Item | Detail lives in | Source location | Executable? | Blocker(s) |
|---|---|---|---|---|---|
| 1 | **No test coverage anywhere** — only IDE placeholders exist | [TEST_COVERAGE.md](TEST_COVERAGE.md) | all modules | ✅ Executable | None — highest-priority item in the project |
| 2 | Checkout total / cart arithmetic untested | [TEST_COVERAGE.md](TEST_COVERAGE.md) | `feature/sales/` | ✅ Executable | None — pure logic, no Android needed. A wrong total is a wrong charge. |
| 3 | `SaleRepositoryImpl` / `ProductRepositoryImpl` failure paths untested | [TEST_COVERAGE.md](TEST_COVERAGE.md), [KI-04](KI-04-LOG-FILTERS.md) | `core/data/repository/` | ✅ Executable | None — both already log `[BipSale][Sale]` / `[BipSale][Product]` on failure, so the paths are known to matter |
| 4 | No Room migration harness | [CORE_RULES §13](../rules/CORE_RULES.md) | `core/data/` | ✅ Executable | None — required before the next schema change ships; this DB holds sales records |
| 5 | `:core:qrcode` utilities untested | [TEST_COVERAGE.md](TEST_COVERAGE.md) | `core/qrcode/` | ✅ Executable | None — pure functions, cheapest possible coverage |
| 6 | No Compose UI tests for any screen | [TEST_COVERAGE.md](TEST_COVERAGE.md) | all `feature/*/` | ⏸️ Partially blocked | Screens must satisfy [`INSTRUMENTED_TEST_STANDARD.md`](../rules/INSTRUMENTED_TEST_STANDARD.md) (two-layer split + test tags) first |
| 7 | Only 3 log filters exist, all error-level | [KI-04](KI-04-LOG-FILTERS.md) | `core/data/repository/`, `app/ui/export/` | ⏸️ Judgement call | No debug-level breadcrumbs anywhere; the checkout flow in particular has no trace logging |
| 8 | No `CHANGELOG.md` history before 2026-07-21 | [CORE_RULES §14](../rules/CORE_RULES.md) | `CHANGELOG.md` | ⏸️ Won't fix | Pre-existing work predates the rule; start from Unreleased |

## Ready to Execute Now

**#1–#5** are all unblocked. Suggested order: #2 (money path) → #3 (known failure paths) → #4 (data-loss risk) → #5 (cheap win).

## Notes

- Items become plans via [`CORE_RULES §7`](../rules/CORE_RULES.md) once work starts — this index is not an execution plan.
- A row here is a **pointer**. Never let it become the only place a rule is written down; the detail belongs in the KI or rules file named in the "Detail lives in" column.
