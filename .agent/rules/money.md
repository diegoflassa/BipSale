---
description: Monetary values are integer minor units, formatted only at the UI edge, rounded exactly once. Use when writing or reviewing any code that carries a price, total, discount, commission or payment amount.
trigger: model_decision
---

# Monetary values

Full spec (source of truth) - [CORE_RULES.md](../../conductor/rules/CORE_RULES.md) §20.

- **Never `Float` or `Double`, never a formatted string outside the UI layer.**
- **Store and transport integer minor units** - `12990` is R$ 129,90. Exact comparison, no drift on sums.
- **Format only at the edge**, from the locale. Never store, send or parse back a formatted value.
- **Round exactly once**, half-up, at the point the value becomes final. Rounding twice is how a total stops
  matching the sum of its lines.
- **A total is derived, never entered twice.** Compute one from the other and assert it.
- **Percentages apply to minor units and round immediately.** Decide once where the remainder goes, write it
  in the rule, and test it.
- **Every monetary write logs at a surviving level** with its identifier and the amount in minor units.
