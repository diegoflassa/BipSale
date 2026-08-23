# KI-TBD: Future Work Index

**Scope:** Whole project — aggregates every known deferred item, backlog item, and not-yet-implemented rule
**Status:** TBD — planning reference only, no code here
**Last verified:** 2026-08-23

## Purpose

Single landing page for "what's left." Each row points to where the authoritative detail lives — this file indexes, it does not duplicate. When an item ships, remove its row and strip the "deferred/TBD" framing at the source.

## Open Items

| # | Item | Detail lives in | Source location | Executable? | Blocker(s) |
|---|---|---|---|---|---|
| 4 | No Room migration harness | [CORE_RULES §13](../rules/CORE_RULES.md) | `core/data/` | ⏸️ Deferred | App not yet released — no users to migrate. Required before **first release** so post-release schema changes don't wipe sales data |
| 6 | Compose UI tests for the `:app` screens | [TEST_COVERAGE.md](TEST_COVERAGE.md) | `app/ui/` | ✅ Executable | Every `feature/*` module has a `ScreenContent` instrumented suite. `BackupScreen`, `ExportScreen` and `DashboardScreen` are the gap: they carry no test tags, and their `ScreenContent` halves are `private` rather than `internal` |
| 8 | No `CHANGELOG.md` history before 2026-07-21 | [CORE_RULES §14](../rules/CORE_RULES.md) | `CHANGELOG.md` | ⏸️ Won't fix | Pre-existing work predates the rule; start from Unreleased |
| 11 | `android:allowBackup` is on for release without migrations | [CORE_RULES §13](../rules/CORE_RULES.md) | `app/src/main/AndroidManifest.xml` | ⏸️ Deferred | Blocked on #4; both deferred until pre-release. No users to migrate yet |

## Proposed Execution Order

### Phase 1 — Executable now

| Order | Item | Rationale |
|-------|------|-----------|
| 1st | **#6** `:app` screen UI tests | Needs only a device; the pattern is already established in all three `feature/*` modules |

### Phase 2 — Pre-Release

| Order | Item | Rationale |
|-------|------|-----------|
| 2nd | **#4** Room migration harness | No users yet — required before first release so post-release schema changes are safe |
| 3rd | **#11** `allowBackup` safety | One-liner once #4 lands; same pre-release gate |

### Won't fix

| Item | Rationale |
|---|---|
| **#8** CHANGELOG backfill | Predates the rule |

## Notes

- Items become plans via [`CORE_RULES §7`](../rules/CORE_RULES.md) once work starts — this index is not an execution plan.
- A row here is a **pointer**. Never let it become the only place a rule is written down; the detail belongs in the KI or rules file named in the "Detail lives in" column.
