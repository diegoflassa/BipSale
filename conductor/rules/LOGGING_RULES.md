# Logging — BipSale

Log format and filters, coverage, redaction by build variant, protected filters, and how to choose the level.

> **Part of this project's rule set.** Section numbers are one shared space across
> `conductor/rules/` — `§8` is `§8` no matter which file holds it, and the master index in
> [CORE_RULES.md](CORE_RULES.md) says where each one lives. Numbers are never reused or renumbered.
> Cite as `§N`, never by line.

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

### 8.6 Choosing the level - what survives into `release` (MANDATORY)

§8.2 says *what* must be logged and §8.3 says *how* to redact it. This decides **at which level**, and it
is what makes the other two mean anything: coverage without a level policy produces exactly the failure
they exist to prevent - everything at `Timber.d`, a mute release build, and redaction that has quietly become
deletion.

**The gate is the `Timber.Tree` planted per build variant in the `Application`** - a debug tree that accepts every level, and a release tree that drops `Timber.d`/`Timber.v` and forwards the rest. It is a runtime floor, so where a line is written is the only thing deciding whether the field ever sees it.

**Choose by asking one question: _if this line is missing from a field capture, can the problem still be
diagnosed?_** If the answer is no, it may not be `Timber.d`.

| Level | Use it for | Survives release |
|---|---|---|
| `Timber.e` | A failure. Every `catch`, every failed Retrofit call, every branch that loses a sale or a stock movement. | ✅ |
| `Timber.w` | A recoverable or defensive decision: retry, dedup, fallback, denied gate, unexpected-but-handled state. | ✅ |
| `Timber.i` | Milestones that prove *what the app did*: checkout committed, stock movement applied, export written, scan resolved, navigation the user can perceive. | ✅ |
| `Timber.d` | Developer chatter with no diagnostic value once the feature works - loop internals, per-item detail, values already implied by a surviving line. | ❌ stripped |
| `Timber.wtf` | Assertion level: a state the code believes unreachable. | ✅ |

**Mandatory at a surviving level - a `Timber.d` here is a defect:**

- **The money path.** Checkout started, cart totalled, sale persisted, sale confirmed, payment recorded,
  stock decremented, and every dedup or skip along the way. A release capture has to answer *was this sale
  persisted?* and *was stock actually moved?* without the device. This is why the rule exists: a sale that
  fails silently is a sale nobody can reconstruct.
- **Boot gates.** Database open and migration outcome, backup restore result - they decide whether the app
  can trade at all.
- **Every failure branch.** §8.2 forbids swallowing an error; in `release` the line has to still exist.
- **Every external boundary outcome.** Room read and write results, Retrofit status and payload shape, CameraX scan callback outcome, Excel export file write.

**Not to be promoted** - keeping these at `Timber.d` is correct, and promoting them buries the signal: success
of a pure in-memory helper, cache-hit and skip fast paths, per-item loop progress, recomposition and
render traces, and any line whose entire content is already carried by an adjacent surviving line.

**A surviving line must stay useful after redaction (§8.3).** `Timber.e("[BipSale][Sale] sale failed")` passes the level rule and fails this
one - it carries no identifier, no code, no reason. Every surviving line names its scenario filter, a
non-reversible correlation id, and the status or error code.

> **The gate has to actually exist.** Until 2026-08-27 `BipSaleApp` planted a tree only under
> `isDebug()`, so a shipped build emitted **no log line at all** and every rule above described
> something that never happened in the field. `ReleaseTree` is that gate; if a future refactor drops
> the `Timber.plant` call for release, this whole section silently stops being true again.

### 8.7 Long messages are split, not truncated (MANDATORY)

Android caps one log entry at roughly 4 KB **counted in bytes** and discards the rest without saying
so. A truncated line reads exactly like a line that never mentioned the thing you are looking for,
which is how §8.2 gets quietly defeated by a payload that simply got long — an exported sheet summary,
a scanned payload, a stack trace.

**`LogChunker` handles this inside the planted trees, so the `Timber.*` call sites §8.1 mandates stay
exactly as written.** It cuts on a UTF-8 byte budget and repeats the scenario filter on every piece:

```text
[BipSale][Sale][CHECKOUT][part 1/3] <first 3 500 bytes>
[BipSale][Sale][CHECKOUT][part 2/3] <next 3 500 bytes>
```

Two properties are the whole point, and both are why Timber's own splitter could not be used:

- **Bytes, not characters.** Timber splits at 4 000 *characters*. Portuguese product names are full of
  accented characters at two bytes each, so a character budget waves them through and the kernel cuts
  them anyway.
- **The filter is repeated.** Without it, `logcat | grep "\[BipSale]\[Sale]"` returns the first
  fragment and silently hides the rest — the worst possible failure for a log being grepped precisely
  because a sale went wrong.

A message that already fits is returned untouched, with no marker. A stack trace rides on the **last**
piece only: attaching it to each would repeat the whole trace per piece and rebuild the oversized entry
the split just prevented.

### 8.8 Redaction helpers (MANDATORY)

§8.3 decides *what* to redact and puts the decision at the call site. `LogRedaction` is what that call
site uses — do not hand-roll a placeholder, and do not pass a raw value and hope:

| Helper | For | Keeps in `release` |
|---|---|---|
| `LogRedaction.cpf(s)` | A CPF | Length only — enough to separate "field was empty" from "11 digits went and were still rejected" |
| `LogRedaction.name(s)` | A customer or operator name | Length only |
| `LogRedaction.contact(s)` | A phone or e-mail | Length, and `kind=email` / `kind=phone` |
| `LogRedaction.path(s)` | An export destination | Depth and length only |
| `LogRedaction.text(s)` | Any other personal free text | Length only |

**There is deliberately no money helper.** §8.3 keeps totals and counts: `items=4 total=12990` identifies
nobody and is exactly what reconciling a failed sale needs. Redacting it would be the "redaction became
deletion" failure §8.6 warns about, in the one place this app can least afford it.

Only `release` redacts. Every helper returns `null` for a null input and a length-bearing placeholder
for an empty one, because *absent* and *present-but-empty* are different bugs.
