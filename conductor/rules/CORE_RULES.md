# CORE RULES — BipSale

**Self-contained operational + project rules.** Load alongside `ai_behavior.md` (behavioral) for any non-trivial change. Architecture (module graph, layers, MVI, DI, persistence, build) lives in [`architecture.md`](architecture.md) — not restated here.

---

## 0. Stability & Non-Regression (ABSOLUTE)

Preserving existing functionality is the top priority of any change. Before writing logic, do an impact analysis (race conditions, state leaks, UI regressions). **This mandate overrides any conflicting rule.**

## 1. Standards

| Aspect | Rule |
|---|---|
| Persona | Code-first. No prose unless asked. |
| Language | English in code/comments; user-facing strings via resources. |
| Auth | No BUILD or COMMIT without explicit user confirmation in the current turn. |

## 2. Git Safety (ABSOLUTE)

Never run `git add`/`commit`/`mv`/`rm` or any staging/commit command unless the user explicitly requests it **in the current turn**. Past approvals never carry over. A failing build/test is never a reason to commit a WIP. For moves/renames, use plain `mv`/`cp`, never the `git` equivalents.

## 3. Token Economy

Minimize tokens every task. Read only what you need (targeted `grep`/`glob`, line-ranges over whole files). Skip files already in context. Run independent tool calls in parallel. Prefer `Edit` over `Write` for existing files. Keep responses terse — no preamble/recap/pleasantries. Use `file:line` refs instead of quoting blocks.

## 4. Large File Protocol (>500 lines)

1. **Map** — outline/grep for the relevant ranges.
2. **Focus** — read only the target range.
3. **Note** — record findings in context (or a KI); don't re-read.

## 5. Code Style & Readability

### 5.1 Readability & Simplicity (MANDATORY)

Avoid clever tricks, overly terse one-liners, or complex language constructs that save a few lines at the cost of obviousness. **Prefer code readability instead.** Code is read far more often than it is written. Clear, straightforward code reduces bugs and helps the next developer (or AI) understand the intent instantly.

### 5.2 No Inline Fully-Qualified Names (MANDATORY)

Never reference a project type by its FQN inside an expression, generic, or annotation argument (e.g. a `hiltViewModel<…>()` or `R.drawable.…` written with a full package path). Add a top-of-file `import` and use the simple name. **Exception:** KDoc cross-references (`[fully.qualified.Symbol]`) require the FQN — that is correct.

**Rationale:** inline FQNs bloat call sites, hide real dependencies from the import block, and break IDE refactor/rename.

### 5.3 Enum When Every Case Is Stateless (MANDATORY)

If every subtype of a `sealed class` / `sealed interface` is a bare `object` / `data object`, it is an `enum class`. A sealed hierarchy earns its cost only when at least one case carries data that distinguishes two instances of that same case — `UiText.DynamicString(value)`, `UiText.StringResource(resId, args)`. `PaymentMethod` is the other side of the same coin: stateless cases, so it is correctly an enum. A hierarchy of only `data object`s belongs on that side too.

What the enum buys that an all-objects sealed hierarchy does not:

- `entries` — iterate the cases for an exhaustive UI mapping, a payment-method picker, or a filter row, without maintaining a hand-written `listOf(...)` that silently goes stale when a case is added.
- `valueOf` / `name` — free round-trip for Room columns and Retrofit payloads in both directions; a sealed hierarchy needs a hand-written `TypeConverter` or adapter for the same thing.
- `when` exhaustiveness with none of the per-case declaration noise.

**Promote to sealed the moment one case needs a payload.** That is the signal, and it is a mechanical change. Do not model as sealed pre-emptively "in case a case grows a field later" (`ai_behavior.md` §2).

**Carve-out — the presentation state machine stays sealed.** The per-screen `Intent` / `Effect` hierarchies inside `XxxContract.kt` stay sealed even while every case is a `data object`: [`architecture.md` § MVI Contract](architecture.md) mandates that shape per screen, those hierarchies reliably grow payload-carrying cases as a screen gains fields (`SalesContract.Effect` already mixes `NavigateBack` with `ShowError(message)`), and the ViewModel and route composable do `is`-checks against them. This subsection scopes to **domain and data outcome types** in `core:domain` / `core:data` — not to the MVI contract.

## 6. KI Sync Rule (GLOBAL — MANDATORY)

After **any** code change, planning update, or architectural decision that modifies the behaviour,
structure, or contracts of a feature:

1. **Identify the affected KI(s)** from `conductor/knowledge/INDEX.md`.
2. **Update the KI immediately** — before the turn ends. Do not defer.
3. **What to sync:** file tables, business rules, layer boundaries, public contracts, and test targets affected by the change.
4. **Scope:** only update what changed. Do not rewrite unrelated sections.
5. **If the change affects `architecture.md` or `CORE_RULES.md`** (global rules), those files ARE the source of truth — reflect their content in the relevant KI's Business Rules section.

### 6.1 Write KIs as if every change was always the original intent

A KI is a **present-tense canonical spec**, not a changelog. Whenever you update a KI:

- **Rewrite affected sections in the present tense.** Do NOT prepend "Phase X added…", "Task Y changed…", or similar historical scaffolding. Just state what the code IS, as if it had always been that way.
- **Do NOT keep "Previously verified" / "Earlier verified" parenthetical chains** describing what the file used to say. Each KI has a single `**Last verified:** YYYY-MM-DD` line and no further dated history.
- **Diff context belongs in the commit message**, not in the KI. The reader of the KI is implementing fresh; they do not need to know the spec used to be different.

### 6.2 KIs must be self-sufficient

A KI must be **readable on its own** by an AI making code changes to the relevant module. That means:

- All contracts, file tables, business rules, test targets, and worked examples needed to safely change the module live inside the KI.
- **No references to `conductor/plannings/*` files** — plannings are temporary artefacts that get deleted; a KI that links to one breaks the moment the plan is removed. If a planning contained information the KI needs, **inline it into the KI**.
- Cross-KI references are fine when they prevent duplication, but the KI must still be useful on its own for the change at hand.

### 6.3 Optimise for targeted reads

Keep KIs **small and focused** so an AI only loads what it needs:

- If a KI grows past roughly 400 lines or covers multiple sub-packages, **split it** along sub-package boundaries (convention: `KI-Xa`, `KI-Xb`, … with a small overview parent `KI-X.md`).
- Cross-cutting concerns that span sub-packages go in the **overview parent**, not duplicated in each sub-KI.
- Trim aggressively: drop historical change-narratives, duplicated patterns already documented in `rules/`, and planning markers. **Never** trim the substantive specs needed to make code changes — file purpose, contracts, business rules, test cases.

> **Rationale:** KIs are the reference an AI reads when implementing. A planning can diverge from a KI silently and cause regressions. Plannings are temporary; KIs persist. The KI must always reflect the current implementation contract, written in present tense, self-contained, and small enough to load targeted reads.

## 7. Planning Protocol

- **Storage:** all plans in `conductor/plannings/`. Completed/obsolete → `conductor/plannings/archived/` (**never delete**). Keep `conductor/plannings/INDEX.md` current.
- **Naming:** `[CODE]_[desc]_plan.md` if a ticket exists, else `[feature]_[desc]_plan.md`.
- **Create a plan when** work spans 3+ files, crosses layers (UI+VM+data), fixes a blocking bug, gates behind a flag, or the approach is uncertain. Content: scope, steps, testing checklist, blockers, dependencies.
- **META_PLANNING** (`META_PLANNING_*.md`) is disposable scaffolding: synthesise the multi-model proposals into one canonical plan, write it, then delete the META_PLANNING and update `INDEX.md`. Never execute a META_PLANNING as-is.

### 7.1 META_PLANNING Protocol (GLOBAL RULE)

A `META_PLANNING_*.md` file is a **consolidation prompt**: it collects planning proposals produced by multiple AI models and instructs one AI to synthesise them into a single canonical execution plan.

**Purpose:** META_PLANNINGs are never executed as-is. They exist only to produce a real plan.

**Execution protocol (mandatory):**

1. **Read the META_PLANNING file** in full.
2. **Synthesise** the proposals into a single canonical `[feature_name]_plan.md` inside `conductor/plannings/` following naming rules.
3. **The synthesised plan is the output** — write it with full scope, implementation steps, testing checklist, blockers, and dependencies.
4. **Delete the META_PLANNING file** after the synthesised plan is written and confirmed. META_PLANNINGs are disposable scaffolding; the canonical plan is the artefact that persists.
5. **Update `conductor/plannings/INDEX.md`**: remove the META_PLANNING row, add the new canonical plan row with status `🔵 Backlog`.
6. **Do NOT start implementing** during the META_PLANNING synthesis turn unless the user explicitly asks. Synthesis = planning only.
7. **One surviving edition at completion.** When a synthesis produces **multiple editions of the same
   plan** (e.g. a Sonnet edition and a Gemini edition — same task set, same numbering, different
   executor tuning), all editions stay live and in sync while the work is in progress (§7.1a). **Once
   the planning is finished** — every task closed, or the plan declared obsolete — archive **exactly
   one** edition to `conductor/plannings/archived/` and **delete** the others.
    - **Which one survives:** prefer the **Claude-tuned** edition. If no Claude edition exists, keep
      the edition that was actually executed.
    - **Before deleting**, port any execution notes, revision history, or decisions that exist *only*
      in a doomed edition into the surviving one. The survivor must be a complete record on its own.
    - **Timing is strict:** never delete a sibling edition while any task is still open — the
      editions are two views of one live backlog until the last task closes.
    - This is a **narrow carve-out** from §7.2's never-delete rule. It applies only to redundant
      editions of a *single* plan, never to distinct plans.

> **Rationale:** META_PLANNINGs accumulate noise. The synthesis step produces a clean, deduplicated, actionable plan that any AI can execute without re-reading the original proposals.

### 7.1a Multi-Edition Plan Sync (GLOBAL RULE)

When one task set is published as **more than one planning file** (e.g. a Sonnet edition and a Gemini
edition), those files are **two views of ONE backlog, not two backlogs**. They must never disagree.

- **Same task IDs, same numbering, forever.** Task IDs are authoritative — never renumber them in one
  edition without renumbering every sibling identically.
- **Mirror every advance in the same turn.** When a task is completed or advanced in one edition,
  update *all* sibling editions in that same turn: the task-index row, the task body/header, and a
  mirrored revision-history note.
- **Each edition must carry this rule in its own text**, so an executor that opens only one edition
  still learns it has siblings to update.
- **One source of truth.** Where the task set is also tracked elsewhere (KI-TBD, a plan's own
  tracker), that tracker wins. If editions drift from it or from each other, reconcile *every*
  edition to the tracker.
- **At completion**, collapse the editions down to one survivor per §7.1 step 7.

### 7.2 Plan Lifecycle

- ❌ **NEVER DELETE** completed or obsolete plans from `conductor/plannings/`. **Sole exception:**
  redundant *editions* of one plan collapse to a single survivor at completion — see §7.1 step 7.
- ✅ **MOVE** completed or expired plans to `conductor/plannings/archived/` for permanent record.
- **Status Transitions:**
  - **Ready → Active**: Update plan with start date, update `INDEX.md` status.
  - **Active → Completed**: Create/update corresponding KI, link bidirectionally, move to archived.
  - **Active → Obsolete**: Archive immediately if plan is superseded or ticket closed without implementation.

---

## 8. Logging — Timber Only

**Mandatory.** Every log goes through Timber. `android.util.Log` is forbidden anywhere in the codebase.

### 8.1 Adding a log (MANDATORY — read before writing any `Timber.*` call)

1. **Tag = filter, not class.** Timber auto-tags with the calling class; the bracket filter carries the diagnostic meaning.
2. **Every log message carries exactly one scenario filter as its leading bracket tag:**
   ```kotlin
   Timber.e("[BipSale][FILTER_NAME] message")
   ```
   The format is `[FILTRO_PAI][FILTRO_FILHO]`. Here the **parent is always `[BipSale]`** (the app root) and the **child names the flow being diagnosed** — `[BipSale][Product]`, `[BipSale][Sale]`, `[BipSale][Export]`. When a flow is large enough to need step-level granularity, append a third segment naming the exact step: `[BipSale][Sale][CHECKOUT]`.
3. **The filter names what is being diagnosed — never a ticket.** `[BipSale][BUG-123]` is forbidden; ticket context belongs in git history, not in runtime logs.
4. **One filter per log.** Use a second child tag only when a single log line genuinely spans two distinct scenarios, and combine them **without spaces**: `[BipSale][Sale][Product]`.
5. **Sensitive values follow the build-variant policy in §8.3** — not a blanket ban.
6. **Catalogue it in the same turn.** Every new filter MUST be added to [`KI-04-LOG-FILTERS.md`](../knowledge/KI-04-LOG-FILTERS.md) — the SOT — before the turn ends. This applies project-wide without exception.

### 8.2 Coverage — everything must be diagnosable from logs alone (MANDATORY)

**All app code must be logged well enough that an AI can root-cause any problem from a log capture alone**, without reproducing the failure and without reading the source. Concretely, each of these gets a log:

- **Every use case / repository call that can fail or branch on external state** — entry and outcome.
- **Every failure branch.** No silently swallowed error, ever — every `catch`, every `runCatching { }.onFailure { }`, every `else` that handles an error case.
- **Every state transition the user can perceive** — navigation, cart add/remove, checkout start → persisted → confirmed, scan success/failure.
- **Every external boundary crossing** — Room read/write results, Retrofit call issued and returned (status + shape), CameraX scan callbacks, Excel export file write.

A branch that can fail and logs nothing is a defect of the same severity as a missing regression test (§12). This matters more here than in most apps: **the checkout path moves money**, and a sale that fails silently is a sale nobody can reconstruct. When adding a feature, the reviewer question is: *if this breaks at a point of sale, does the log tell me where?* If not, the logging is incomplete.

### 8.3 Redaction by build variant (MANDATORY)

| Build | Sensitive values | Rule |
|---|---|---|
| `debug` | **Allowed in full** | Raw CPF, customer names, full sale payloads, product data. Debug builds run only on a developer machine. |
| `release` | **Forbidden** | **The only variant that must redact.** No CPF, customer names, or any personal field from a sale. |

**Redacted must never mean silent.** A release build has to stay diagnosable — log the *shape* instead of the value:

- field **presence** and length (`cpf=[REDACTED len=11]`), not the value
- **counts and totals** (`items=4`, `total=12990` in centavos) — a monetary total is not personal data and is essential to reconciling a failed sale
- **status / result / error codes** (`result=PERSISTED`, `SQLiteConstraintException`)
- **non-reversible identifiers** — sale id, product code, and Room row id are fine; CPF and customer name are not

Emit the `[REDACTED]` placeholder rather than dropping the field, so the message shape stays readable and greppable across variants. A release log that says only `sale failed` is as useless as no log at all and violates §8.2.

> **Enforcement seam:** redaction is decided at the log call site, based on the build variant — never by post-processing in a Timber tree. `release` is the only variant that redacts; when in doubt about a value in a release path, redact it and log its shape.

### 8.4 Protected filters

**Every filter listed in [`KI-04-LOG-FILTERS.md`](../knowledge/KI-04-LOG-FILTERS.md) is protected.** None may be stripped by `/remove_filter`, `/clean`, or any "log hygiene" sweep unless the user explicitly names that exact filter and confirms. A filter present in code but absent from the catalogue is still protected — catalogue it rather than deleting it.

`Timber.i` / `Timber.w` / `Timber.e` are intentional production signal and are **never** touched by `/remove_filter`, regardless of filter.

### 8.5 Renaming or removing a filter

Filter names are public string contracts. Any rename or removal updates, **in the same turn**: the KI-04 row, this section if it names the filter, [`workflows/remove_filter.md`](../workflows/remove_filter.md) if it names the filter, every production `Timber.*` call site, and every test assertion on the tag. One-turn change or none.

## 9. Composable Extraction (GLOBAL — all Compose screens)

- **No inline reusable widgets.** Never define a reusable UI element as a local function or private composable inside a screen file. Extract it to its own file.
- **Placement:** a composable used by 2+ modules goes in `core:ui`; one used by a single feature goes in `feature/<name>/components/`.
- **Build on the Material 3 component, not on a `Box`.** When the thing being built *is* a button, text field, card, chip, checkbox, or dialog, start from the Material 3 composable and restyle it through its `colors` / `shape` / `border` / `contentPadding` parameters. Fall back to a `Box` / `Row` / `Column` with `.background().clip().clickable()` only when the design genuinely cannot be expressed through those parameters, and say why in a one-line comment at the declaration. A hand-rolled clickable `Box` silently drops the 48dp minimum touch target, ripple / state-layer feedback, `Role` semantics for TalkBack, `enabled` handling that also blocks the click, and focus traversal for an attached keyboard or wedge scanner. Reproducing the *visual* result is not the bar — none of those behaviours show up in a screenshot or in a `@Preview`, which is exactly why they get lost. On a checkout screen, an `enabled = false` that still fires the click is a duplicate sale.
- **Every screen's header is `core:ui`'s `BipSaleTopAppBar`.** Never hand-roll a `TopAppBar` in a screen. It renders a back arrow from `onBack`, or whatever `navigationIcon` a screen passes when it needs a different control (a "clear selection" close). Omit both only on a start destination, where there is nowhere to go back to. A per-screen copy is how the back affordance ends up in a different place, or with a different `contentDescription`, on every screen — which is exactly what a TalkBack user navigates by.
- **Previews are mandatory.** Every widget file carries at least one `@Preview` per [`PREVIEW_STANDARD.md`](PREVIEW_STANDARD.md). A widget without one is incomplete.
- Recomposition / stability / memory rules: [`COMPOSE_RULES.md`](COMPOSE_RULES.md). Screen test-friendliness: [`INSTRUMENTED_TEST_STANDARD.md`](INSTRUMENTED_TEST_STANDARD.md).

## 10. String Resource Ownership (GLOBAL — MANDATORY)

**Every user-facing string is an Android string resource. No hardcoded literals in Compose**, `contentDescription` included. `UiText` (`core:ui`) is the carrier for strings resolved outside a composable.

- **Package-owned strings** live in the owning module's `res/values/strings_<package>.xml`, named after the package that uses them (e.g. `strings_sales.xml`). Each file opens with a comment declaring its owner package, so the Kotlin↔resource binding is explicit:
  ```xml
  <!-- Owner package: br.com.diegolassa.bipsale.feature.sales -->
  ```
  When a screen or package is deleted, its strings file is deleted in the same commit.
- **Single-screen modules** may keep a monolithic `strings.xml`. Once a module reaches 2+ screens or ~20 keys, split per the rule above in the same turn the second screen lands.
- **Shared strings** used by 2+ modules live in `core:ui`'s `strings_common.xml` with a `common_` prefix. Do **not** pre-seed speculatively — promote a key only when the second module needs it, and delete the per-module copies in the same turn.
- **Key naming:** `<module>_<package>_<role>` (e.g. `sales_checkout_button_confirm`). Stable across translations; never embed locale-specific wording in the key.
- **Every locale folder declares every key.** A key present in one locale and missing from another is a bug, not a fallback strategy.

## 11. Extension Functions

Use them when they make the call site read better and keep behaviour next to its type: reusable transforms/mappers (`Entity.toDomain()`), domain↔UI adapters, small helpers on stdlib/platform types. Place in a file named after the receiver (`StringExt.kt`, `ResultExt.kt`), in the module where it's used. Keep pure and single-purpose; an extension needing injected deps belongs in a class. Don't wrap a single call site or hide where work happens.

## 12. Regression Test Rule (GLOBAL — MANDATORY)

**Every bug fix ships one or more tests in the same turn that prove the specific failure no longer happens.**

- The test must fail against the pre-fix code and pass against the fix — it pins the exact defect, not just general area coverage. A happy-path test already covered elsewhere does not satisfy this rule.
- Applies at any layer: unit, instrumented, or — where the bug is only reproducible on hardware — a documented manual on-device verification step.
- New tests live alongside the existing suite for the fixed class; don't create a separate "regression" file unless that class has no suite yet.
- A bug fix without a pinning test is an incomplete change, same severity as a stale KI (§6).

> **Rationale:** a fix without a pinning test can silently regress on the next refactor — nothing in the suite would catch it reverting.

## 13. Database Migration Safety (GLOBAL — MANDATORY)

Room is the single source of truth for sales and products. A lost row is a lost sale — money the business cannot reconcile. Schema evolution is governed by hard rules, not convention.

1. **NEVER `fallbackToDestructiveMigration()` (or `…OnDowngrade`).** It wipes `BipSaleDatabase` on any schema-hash mismatch — catastrophic for sales history. This is the first "fix" someone reaches for when a forgotten migration crashes on open — reject it in review.
2. **Every schema change ships three things in the SAME turn:** (a) bump the DB `version`, (b) a real `Migration(n, n+1)` registered on the builder, (c) the exported schema JSON for `n+1` **plus** a passing migration test that migrates `n → n+1` and asserts every row survives. A schema edit missing any of the three is an incomplete change, same severity as a stale KI (§6).
3. **Keep the migration harness.** `room-testing` + `MigrationTestHelper` against the exported schema set is what makes a forgotten version bump fail in CI instead of at a point of sale.
4. **Before shipping,** confirm no previously released version had a different shape with no migration path. Dev-time schema churn is reset by clearing app data — never by a destructive fallback.

> **Why a crash-on-open is worse than it looks:** if the DB throws on open, no sale can be recorded or read — the terminal is down until a fixed build ships.

## 14. Changelog Rule (GLOBAL — MANDATORY)

**Update `CHANGELOG.md` → `## Unreleased` with each planning or fix**, one line per work unit (not per commit):

```
- TEXT_OF_THE_FIX (CODE_OF_THE_FIX)
```

Omit the `(CODE)` suffix when no ticket exists. On a release cut, retitle `## Unreleased` to `## [X.Y.Z] YYYY-MM-DD` and add a fresh empty `## Unreleased` above it.

## 15. Cross-Project Rule Sync (GLOBAL — MANDATORY)

BipSale, Slotify, and Comiqueta share one AI-workflow rule set and one developer. Whenever a **shared** rule is added, updated, or deleted in any of the three, apply the equivalent change to the other two **in the same turn**.

**What counts as shared:** §0 Stability · §2 Git Safety · §3 Token Economy · §4 Large File Protocol · §5 Code Style & Readability (5.1 Readability, 5.2 No Inline FQN, 5.3 Enum-vs-sealed) · §6 KI Sync (all sub-sections) · §7 Planning + META_PLANNING + Lifecycle · §8.1–8.5 log filter format, coverage, redaction-by-variant, protection, rename · §9 Composable Extraction · §10 String Ownership (the ownership model, not the key namespaces) · §12 Regression Test · §13 DB Migration Safety (BipSale ↔ Comiqueta only — Slotify has no Room) · §14 Changelog · this section · everything in `ai_behavior.md` · everything in [`GRADLE_RULES.md`](GRADLE_RULES.md).

**What is NOT shared** — adapt or omit, never copy verbatim: module graphs, DI framework (Hilt vs Koin), logging API (`Timber` vs `TimberLogger` vs `Logger`/Kermit), persistence (Room/Retrofit vs Room/SAF vs Supabase), build config, locale sets, and everything in `architecture.md`.

**Counterpart mapping:** the three trees are structurally identical — the same relative path in each repo is the counterpart (`conductor/rules/CORE_RULES.md` ↔ `conductor/rules/CORE_RULES.md`, and so on).

> **Rationale:** the same clarification should never be needed twice. Divergent workflow rules make an AI behave differently in each repo for no reason.
