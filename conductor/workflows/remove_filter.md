---
name: remove-filter
description: Targeted removal of log statements carrying a specific [Filter] token, preserving logic, protected filters, and adjacent code. Also governs how new filters are added.
---

# /remove_filter <FILTER> — Log Filter Management

> Invoked as `/remove_filter <FILTER_NAME>`. Removes `Timber.d` / `Timber.v` calls tagged with a specific filter **while protecting application logic**. Also the runbook for adding a filter.

Filter format, PII rules, and the catalogue contract: [`LOGGING_RULES.md §8`](../rules/LOGGING_RULES.md). Catalogue SOT: [`KI-04-LOG-FILTERS.md`](../knowledge/KI-04-LOG-FILTERS.md).

---

## 1. Filter Classification

- **Protected (all catalogued filters).** Every filter in KI-04 is load-bearing for post-incident diagnosis and **MUST NOT** be removed unless the user names that exact filter and confirms. All three filters in the codebase today are error-level breadcrumbs inside `catch` blocks — each is the sole diagnostic for its failure path.
- **Unprotected.** This project has none by policy — a filter that exists in code but is missing from KI-04 gets catalogued, not deleted. Ticket-scoped filters (`[BipSale][BUG-123]`) are forbidden outright by `LOGGING_RULES.md §8.1`.

## 2. Adding a Filter

1. **Log format:** `Timber.d("[BipSale][FILTER_NAME] message")`.
2. **Update the SOT:** add the filter to [`KI-04-LOG-FILTERS.md`](../knowledge/KI-04-LOG-FILTERS.md) with its primary class, what it monitors, and level.
3. **Update rules:** amend `LOGGING_RULES.md §8` only if it names the filter as an example.
4. **Synchronize:** update every call site and test assertion in the same turn.

## 3. Removing a Filter

### ✅ Safe to remove
- `Timber.d("[<FILTER>] …")` / `Timber.v(...)` statements for the named filter.
- Local variables that existed **solely** to build the removed message.
- Empty blocks left behind by the removal.

### ❌ Forbidden to remove
- **Logic code** — `if`/`when` branches, method calls, assignments.
- **Exception handling** — never delete a `catch` or `onFailure` block because its only body was a log; empty it deliberately instead.
- **`Timber.i` / `Timber.w` / `Timber.e`** — intentional production signal, regardless of filter.
- **Protected filters** — unless the user names that exact filter and confirms.

## 4. Removal Procedure

1. **Locate.** Grep `\[<FILTER>\]` across the repo. Exclude `build/` and third-party sources. Build the complete file list and show it to the user before editing.
2. **Check protection.** Verify the filter against KI-04. If catalogued, stop and ask for confirmation.
3. **Check block context, per match:**
   - If the log is the **only** statement in a block (`if`, `else`, `try`, `catch`, `onFailure`, lambda), remove the whole block only when doing so cannot change control flow — otherwise keep the block and empty its body deliberately.
   - If the block has other statements, remove **only** the log statement, including any trailing `)` on its own line.
   - If the call spans multiple lines, remove all of its lines.
   - **Never remove code adjacent to the log.**
4. **Imports.** If `Timber` becomes unused in a file, remove the now-orphaned import.
5. **Tests.** Search for assertions on the tag and update them in the same turn.
6. **Update the SOT.** If the filter is retired entirely, remove its row from KI-04.
7. **Report.** Files modified, calls removed, orphaned variables flagged, tests updated, filter retired (yes/no).
8. **Stop before commit** — `CORE_RULES.md §2` (Git Safety).

## 5. Verification

- `./gradlew detekt ktlintCheck` — no new warnings introduced.
- `./gradlew test` — green; a filter removal must not change behaviour.
- Re-read the diff: only log lines and deliberately emptied blocks should appear.
