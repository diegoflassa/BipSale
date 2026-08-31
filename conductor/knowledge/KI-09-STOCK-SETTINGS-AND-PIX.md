# KI-09: Stock, App Settings & PIX Payment

**Scope:** `products.quantity`, `core/domain/settings/`, `core/data/settings/`, `core/domain/pix/`, `app/ui/settings/`, the PIX half of `feature/sales/`, the import half of `feature/products/`
**Last verified:** 2026-08-30

## Problem

Three things a counter needs that the app used to leave to the operator's memory: how many units are on the shelf, which PIX key the shop is paid on, and what the customer actually scans to pay. Getting any of them wrong costs money at the till rather than in a log.

The PIX identity (key, payee name and city) is hardcoded in `PixDefaults` — the shop has a fixed Nubank poster. The Settings screen no longer edits PIX fields; only `pixDiscount` and `askCustomerInfo` remain user-configurable.

## Files

| File | Purpose |
|---|---|
| `core/domain/model/Product.kt` | `quantity` — units on hand |
| `core/data/dao/SaleDao.kt` | `decrementProductStock` + the `@Transaction` that runs it with the sale |
| `core/domain/pix/PixDefaults.kt` | Fixed PIX key, merchant name/city — build constants from the Nubank poster |
| `core/domain/settings/AppSettings.kt` | Default PIX discount, customer-info toggle, QR label columns |
| `core/domain/settings/SettingsRepository.kt` | Observe, snapshot, save |
| `core/data/settings/SettingsRepositoryImpl.kt` | DataStore Preferences, plus the discount flattening shared with the backup |
| `core/domain/pix/PixPayload.kt` | Builds the "PIX Copia e Cola" string |
| `app/ui/settings/` | The settings screen |
| `app/ui/dashboard/NewSaleEntryViewModel.kt` | Decides whether a sale opens on the customer form |
| `core/qrcode/components/PixQrCard.kt` | Renders the payload as a QR |
| `core/qrcode/components/QrCodeDialog.kt` | The code at full screen, on a white plate |
| `feature/sales/components/ProductDetailSheet.kt` | Read-only product info from a cart line |
| `core/utils/ProductSheet.kt` | Writes the import template, reads a filled one |
| `core/domain/product/ProductImport.kt` | `ImportedProduct`, `ProductImportReport`, rejections |
| `core/domain/usecase/ImportProductsUseCase.kt` | Reads the sheet and registers the accepted rows |

## Business rules

### Stock

1. **A sale decrements stock in its own transaction.** `SaleDao.insertFullSale` writes the sale, its items, and the stock decrements together. A committed sale whose decrement rolled back is inventory nobody can reconcile — which is why the `UPDATE products` query lives in the sale DAO rather than the product one.
2. **Stock never goes below zero** (`MAX(0, quantity - :soldUnits)`). Overselling is not blocked: refusing a sale at the counter because a count is stale is worse than a count that reads zero.
3. **QR label runs print one label per unit on hand**, with a floor of one — otherwise "print all" over an unstocked catalogue silently produces an empty print job.

### Settings

4. **The customer screen is optional.** With `askCustomerInfo` off, a new sale navigates straight
   to the cart and is recorded as anonymous; the hop is removed from the back stack so Back returns
   to the dashboard rather than bouncing through a screen nobody saw.
4a. **QR label printing is configurable.** The operator can choose how many columns to print per page (1 to 6) via the Settings screen. Less columns = larger labels.
5. **Settings live in DataStore, not Room.** They are per-install configuration, not transactional data, and they must survive a restore that replaces every table.
6. **A corrupt preferences file falls back to defaults** rather than taking the sale screen down.
7. **A stored discount out of range is coerced, never thrown** — same rule as the `sale_items` mapper in [KI-07](KI-07-SALES-CART-AND-DISCOUNTS.md).
8. **The PIX discount reuses `ItemDiscount`.** A discount is a percentage or a fixed amount wherever it appears; one type means one set of validation rules and one flattening at the storage seam.

### Payment

9. **No payment method is preselected.** `SalesContract.State.paymentMethod` is null until the operator picks one, and `canFinalize` requires it. A pre-picked method is the one nobody looks at, and a sale filed under the wrong one cannot be reconciled against the drawer or the card statement.
10. **Picking PIX applies the configured default discount**, but only into an empty sale discount — a percentage the operator typed is never overwritten.
11. **The PIX payload carries the amount by default** (EMV tag 54), so the customer confirms a pre-filled value instead of typing one. A per-sale toggle lets the operator switch to "customer types the amount" mode, which builds the payload without tag 54.
12. **A PIX sale holds the screen until the operator acknowledges payment.** Finalizing used to
    navigate straight back, which took the QR with it before the customer had scanned anything.
    The sale is written either way — the dialog is about the customer paying, not about the record.
13. **A recorded PIX sale can be shown again** from the history list, rebuilt from that sale's own
    `finalAmount` so a customer who left without paying scans what was actually rung up.
14. **PIX is always configured.** The key, merchant name and city are build constants in `PixDefaults`; a payload is always available when PIX is selected.
15. **The code expands to full screen on tap**, drawn on a white plate — a dark background inverts
    a code's quiet zone and many scanners refuse to read that.
16. **Any cart change rebuilds the payload.** This happens inside `recalculate()`, not at the call sites: a QR still showing the pre-change total is a customer underpaying by exactly the difference.

### PIX payload format

A static EMV merchant-presented QR: `id + two-digit length + value`, closed by a CRC-16/CCITT-FALSE over everything before it including the `6304` header. Merchant name (25 chars) and city (15 — the **city name alone**, never `city/state`) are accent-folded and uppercased — the spec's character set is restricted, and a bank that reads an accented name shows the customer a mangled one. A non-positive amount is omitted rather than written as zero, which banks reject.

`PixPayloadTest` pins the whole thing against a reference payload byte for byte, because a wrong field order, a wrong length or the wrong CRC variant all produce a string a bank silently refuses.

### Product import

17. **The template is an `.xlsx` with `Código, Produto, Preço, Quantidade`** and one example row the operator overwrites.
18. **Images are never part of the import.** A spreadsheet cannot carry them usefully, so the import brings in rows and the operator attaches photos per product afterwards.
19. **A bad row is reported, not thrown.** One typo in a fifty-row catalogue must not cost the other forty-nine. Rejections name the one-based row number the operator sees in the spreadsheet.
20. **A numeric barcode is read as digits**, not `7.891E12` — a code typed into a spreadsheet arrives as a NUMERIC cell.
21. **An import with nothing usable is refused**, never reported as a success.
22. **An existing code is overwritten.** Re-importing a corrected sheet is how an operator fixes a typo.

## Backup

The archive carries an optional `settings` block (including `askCustomerInfo`) and `quantity` on every product, both defaulted so an archive written before either existed still restores. Settings are restored outside the database transaction — DataStore is not covered by it. See [KI-06](KI-06-BACKUP-AND-RESTORE.md).

## Logging

`[BipSale][Settings]` and `[BipSale][Import]` — see [KI-04](KI-04-LOG-FILTERS.md). The PIX key is logged as presence and length only; it identifies the seller. Stock movements ride the existing `[BipSale][Sale][CHECKOUT]` breadcrumbs.

## Test targets

`PixPayloadTest` (15) · `PixDefaultsTest` (1) · `ProductSheetTest` (11) · `SalesViewModelTest` PIX and payment-method cases (9) · `HistoryViewModelTest` recorded-sale PIX cases (3) · `ProductViewModelTest` import and stock cases (7) · `SaleRepositoryImplTest` stock transaction (1).
