# CI Gate — BipSale

**Status:** ✅ Active — document-only
**Scope:** Solo-developer project on Windows; no hosted CI runner provisioned.
**Mode:** manual pre-PR checklist. No GitHub Actions, no git hook.

---

## Rationale

A hosted pipeline would pay runner minutes for code one person pushes and still could not run the CameraX scanning suites, which need real camera hardware. Enforcement is the developer's manual checklist below.

> **The AI never runs these.** `CORE_RULES.md §1` forbids builds without explicit user confirmation in the current turn — the assistant reports the command, the developer runs it.

---

## Pre-PR checklist (manual)

Before merging to `main`, run in order:

1. **Unit tests** — `./gradlew test`
2. **Instrumented tests** — `./gradlew connectedAndroidTest` on a device with a working camera (QR scanning is not meaningfully testable on a bare emulator)
3. **Static analysis** — `./gradlew detekt ktlintCheck`
4. **Assemble** — `./gradlew assembleDebug` and, for release-impacting changes, `./gradlew assembleRelease bundleRelease` (catches R8 / resource-shrink breakage that debug misses)

Failing any of these blocks the merge.

**Schema changes additionally require** the migration test from [`CORE_RULES.md §13`](CORE_RULES.md) to pass — a Room version bump without it is an incomplete change, and this database holds sales records.

> ⚠️ **Step 1 is currently vacuous** — the project has no real test files ([`TEST_COVERAGE.md`](../knowledge/TEST_COVERAGE.md)). `./gradlew test` passing proves nothing today. Treat the checkout-arithmetic and repository tests listed there as the prerequisite for this gate to mean anything.

---

## What is intentionally NOT enforced

| Gate | Why skipped |
|---|---|
| Hosted CI | Solo project; instrumented coverage needs real camera hardware. |
| Coverage thresholds | There is no coverage to threshold yet. Revisit once [`TEST_COVERAGE.md`](../knowledge/TEST_COVERAGE.md) has non-zero rows. |
| detekt/ktlint as blocking | Reviewed manually before push. |

---

## When to promote this to real CI

Any one of: a second contributor lands a commit · a device farm is provisioned · the release cadence becomes scheduled rather than manual · the test suite becomes non-empty (steps 1 and 3 are worth automating the moment they can fail).

---

## Cross-references

- [`CORE_RULES.md §1`](CORE_RULES.md) — no build without user confirmation. [`§2`](CORE_RULES.md) — Git Safety. [`§13`](CORE_RULES.md) — DB migration safety.
- [`TEST_COVERAGE.md`](../knowledge/TEST_COVERAGE.md) — per-module inventory and known gaps.
