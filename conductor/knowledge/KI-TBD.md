# KI-TBD: Future Work Index

**Scope:** Whole project — aggregates every known deferred item, backlog item, and not-yet-implemented rule
**Status:** TBD — planning reference only, no code here
**Last verified:** 2026-08-22

## Purpose

Single landing page for "what's left." Each row points to where the authoritative detail lives — this file indexes, it does not duplicate. When an item ships, remove its row and strip the "deferred/TBD" framing at the source.

## Open Items

| # | Item | Detail lives in | Source location | Executable? | Blocker(s) |
|---|---|---|---|---|---|
| 4 | No Room migration harness | [CORE_RULES §13](../rules/CORE_RULES.md) | `core/data/` | ⏸️ Deferred | App not yet released — no users to migrate. Required before **first release** so post-release schema changes don't wipe sales data |
| 5 | `:core:qrcode` bitmap rendering untested — grid maths now covered | [TEST_COVERAGE.md](TEST_COVERAGE.md) | `core/qrcode/` | ⏸️ Partially blocked | `QrGenerator` / `QrLabelSheetRenderer` need real Android graphics, so instrumented only |
| 6 | Compose UI tests | [TEST_COVERAGE.md](TEST_COVERAGE.md) | all `feature/*/` | ⏸️ Partially blocked | `:feature:sales`, `:feature:products` and `:feature:history` now have `ScreenContent` instrumented suites. The remaining gap is screens in `:app` (`BackupScreen`, `ExportScreen`, `DashboardScreen`) which still lack the two-layer split |
| 8 | No `CHANGELOG.md` history before 2026-07-21 | [CORE_RULES §14](../rules/CORE_RULES.md) | `CHANGELOG.md` | ⏸️ Won't fix | Pre-existing work predates the rule; start from Unreleased |
| 11 | `android:allowBackup` is on for release without migrations | [CORE_RULES §13](../rules/CORE_RULES.md) | `app/src/main/AndroidManifest.xml` | ⏸️ Deferred | Blocked on #4; both deferred until pre-release. No users to migrate yet |

## Proposed Execution Order

**Nothing executable is left.** Every remaining row is gated on a release, a device, or a rule change.

### Phase 1 — Pre-Release

| Order | Item | Rationale |
|-------|------|-----------|
| 1st | **#4** Room migration harness | No users yet — required before first release so post-release schema changes are safe |
| 2nd | **#11** `allowBackup` safety | One-liner once #4 lands; same pre-release gate |

### Phase 2 — Needs a device

| Order | Item | Rationale |
|-------|------|-----------|
| 3rd | **#6** Compose UI tests | `:feature:sales` is now unblocked — `SalesScreenContent` is split out and tagged. The products and history screens still need the same treatment |
| 4th | **#5** QR bitmap rendering tests | Instrumented-only; schedule when a device/CI runner is available |

### Won't fix

| Item | Rationale |
|---|---|
| **#8** CHANGELOG backfill | Predates the rule |

## Notes

- Items become plans via [`CORE_RULES §7`](../rules/CORE_RULES.md) once work starts — this index is not an execution plan.
- A row here is a **pointer**. Never let it become the only place a rule is written down; the detail belongs in the KI or rules file named in the "Detail lives in" column.
