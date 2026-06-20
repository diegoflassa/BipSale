---
name: remove-filter
description: Surgical removal of debug log calls carrying a specific [Filter] token. Preserves intentional signal (Timber.i/w/e).
---

# /remove_filter <FILTER> — Scoped Debug-Log Removal

> Removes `Timber.d` / `Timber.v` calls whose message carries a given bracket filter token (e.g. `/remove_filter Checkout` targets `[Checkout]`).

## Procedure

1. **Locate** all `Timber.d("[<FILTER>] …")` and `Timber.v(...)` call sites for the token (grep `\[<FILTER>\]`).
2. **Remove** only those debug/verbose calls and any local variable that existed solely to build the removed message.
3. **Never touch** `Timber.i` / `Timber.w` / `Timber.e` — intentional production signal.
4. **Update tests** that assert on the removed log lines, in the same turn.

## Safety

- Honor `CORE_RULES.md §2` (Git Safety): no `git add`/`commit`.
- Surgical only (`ai_behavior.md §3`): do not reformat or remove unrelated logs.
- Report: files touched, calls removed, tests updated, token retired? (yes/no).
