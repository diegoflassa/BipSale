# KI Index — BipSale

One-line summaries. Fetch an individual KI file **only** if the current task matches.

| ID | File | Topic | When to read | Key files |
|----|------|-------|--------------|-----------|
| KI-03 | KI-03-TOKEN-AUDIT-AND-PRUNING.md | Token budget audit + index pruning rules | Editing `conductor/index.md` / running `token_audit` | `conductor/index.md`, `CLAUDE.md`, `conductor/rules/CORE_RULES.md` |
| KI-04 | KI-04-LOG-FILTERS.md | Log filter catalogue (SOT for every `[BipSale][X]` tag) | Adding/renaming/removing a log filter, running `/remove_filter` | `core/data/repository/*.kt`, `feature/history/HistoryViewModel.kt` |
| KI-05 | KI-05-PRODUCT-IMAGES-AND-QR-LABELS.md | Product image storage + QR label printing contract | Touching product images, the price input, or QR label/print code | `core/domain/image/`, `core/data/image/`, `core/qrcode/`, `feature/products/` |
| KI-06 | KI-06-BACKUP-AND-RESTORE.md | Backup archive format + restore contract | Touching backup, restore, sharing, or the archive format | `core/domain/backup/`, `core/data/backup/`, `app/ui/backup/` |
| KI-07 | KI-07-SALES-CART-AND-DISCOUNTS.md | Cart totals, sale-level and per-line discounts, cart entry paths | Touching the cart, any total, any discount, or `sale_items` | `core/domain/model/SaleTotals.kt`, `core/domain/model/ItemDiscount.kt`, `feature/sales/` |
| KI-08 | KI-08-SALES-EXPORT-REPORT.md | Excel sales report: columns, totals row, Resumo sheet, export flow | Touching the Excel export, any report column or total, or the export flow | `core/utils/ExcelExporter.kt`, `core/domain/model/SalesSummary.kt`, `core/domain/usecase/ExportSalesUseCase.kt`, `feature/history/` |
| KI-TBD | KI-TBD.md | **Master index of all deferred / not-yet-implemented items** | Planning next work, checking what is outstanding before starting a task | (index only — see linked KIs) |
| KI-AUTHORING | KI-AUTHORING.md | KI authoring rules: index maintenance, present-tense spec, self-sufficiency, size limits | Creating, renaming, revising, or deleting any KI | (this directory) |

Do NOT pre-load. Index first, fetch on match.

---

## Coverage State

Per-module test inventory, known gaps, and verification commands live in [`TEST_COVERAGE.md`](TEST_COVERAGE.md).

## Skills (Lazy-Load) → `../skills/`

> Read a skill only when its exclusive purpose is the active task. Never pre-load.

| Skill | Exclusive purpose |
|---|---|
| [testing-setup](../skills/testing-setup/SKILL.md) | Test infrastructure (unit / UI / screenshot / E2E) |
| [r8-analyzer](../skills/r8-analyzer/SKILL.md) | R8/ProGuard keep-rule audit + APK size |
| [perfetto-trace-analysis](../skills/perfetto-trace-analysis/SKILL.md) | Runtime jank / latency / memory root-cause via traces |
| [android-cli](../skills/android-cli/SKILL.md) | ADB / device orchestration (deploy, logcat, screenshots) |
| [edge-to-edge](../skills/edge-to-edge/SKILL.md) | Compose system-bar / IME inset handling |
