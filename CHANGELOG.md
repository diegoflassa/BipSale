# Changelog — BipSale

All notable changes to this project. Governed by [`conductor/rules/CORE_RULES.md`](conductor/rules/CORE_RULES.md) — one line per work unit (not per commit), appended to `## Unreleased` in the same turn the work lands:

```
- TEXT_OF_THE_FIX (CODE_OF_THE_FIX)
```

Omit the `(CODE)` suffix when no ticket exists. On a release cut, retitle `## Unreleased` to `## [X.Y.Z] YYYY-MM-DD` and add a fresh empty `## Unreleased` above it.

> Entries before 2026-07-21 were reconstructed from git history after the fact, so they are grouped by month rather than by work unit. Everything from 2026-07-21 onward was written as the work landed.

## Unreleased

- Cart and checkout now derive every total from one domain function, rounded to cents, so the amount displayed and the amount persisted cannot disagree
- Sale-level discount is editable: the bottom bar opens a dialog that refuses anything outside 0-100 instead of accepting it silently
- Per-item discounts, by percentage of the line or by a fixed amount off it, shown on the line with the original price struck through
- Products can be added to a sale by picking from the registered catalogue or by typing a code, not only by scanning a QR label
- A typed or picked code that matches no product is refused rather than rung up as a zero-priced line
- Checkout emits `[BipSale][Sale][CHECKOUT]` breadcrumbs for every cart change and every finalize leg, carrying counts and totals but never a customer field
- Every screen now uses one shared header with a back arrow, keeping its existing title
- detekt runs on every module via a `detekt-convention` plugin rather than only `:app`, and the whole project is clean
- Test coverage extended from 2 modules to all 10 (31 tests to 182), covering the money path, repository failure paths and every ViewModel
- Fixed the history search racing itself: an abandoned query could still overwrite the list with results for a query the field no longer held
- Fixed `UiText.StringResource` comparing by array identity, which made any state holding one look changed on every emission
- Fixed the Excel export writing column indices independently of its header row, so a column change could file values under the wrong heading
- Fixed the Excel export writing to invisible app-private storage on the main thread with no feedback; it now opens the system document picker, writes off the main thread, and shows a snackbar on success, failure or empty data
- Extracted the customer-info screen's hardcoded strings into resources across all four locales
- Aligned `conductor/` structure and rules with the shared cross-project reference layout
- Added seven AI-code-review rules to `conductor/rules/`: cancellation re-throw, named seam interfaces over lambdas, selective saved state, ViewModel-side derivation, `IconButton` for icon-only actions, Material 3 over a hand-rolled `Box`, enum over an all-object sealed hierarchy
- Product image upload: pick a photo from the gallery, downscaled and stored in app-private storage, shown on the edit screen and on product list rows (placeholder icon when absent)
- QR code labels now carry the product name above the code and the price below it
- Batch QR label printing: print all or a selected subset of product labels as a real multi-page document — 20 labels per A4 sheet in a 4x5 grid with dashed cut borders, re-fitting itself to any other paper size chosen in the print dialog, with page-by-page preview and "Save as PDF" from the system print UI
- Fixed batch printing allocating a full-page bitmap per page and concatenating them into one image, which exhausted the heap past a single page and squashed every sheet onto one page at unscannable size
- Fixed a price typed with a comma decimal separator being saved as R$ 0,00; the price is now parsed for both separators, rejected at the domain boundary when not greater than zero, and flagged inline on the form
- Fixed image import, image copying and per-row file existence checks running on the main thread
- Fixed a re-picked product image showing the previous photo, product codes with filesystem-illegal characters breaking image storage, and image files outliving the products that referenced them
- Product images are addressed by file name rather than absolute path, so a backup restore or a secondary user profile no longer orphans every image
- Guarded the label grid against a paper size smaller than one cell, which previously hung the print request in a loop that never advanced
- Extracted `ProductItem`, `ProductThumbnail` and `ProductImagePicker` into their own component files, added test tags and the loading/empty/error states to the product list
- Added unit coverage for price parsing, product save validation, and A4 label sheet pagination, plus instrumented coverage for image import, downscaling, file naming and deletion (31 tests, the project's first)
- Fixed product image import rejecting every photo: bounds decoding runs with `inJustDecodeBounds`, where `decodeStream` returns null on success, and the null check was bound to that result instead of to the input stream
- Fixed the add/edit product screen discarding every snackbar and print request, which made the failed image import look like nothing had happened at all
- Every image-import failure branch now logs what it saw, so the same class of failure is diagnosable from a log capture
- QR labels print the product name at 9 pt and the price at 12 pt, up from 6.5 and 9, with a reserved two-line name block so every code lands at the same offset across a sheet
- Long product names wrap to a second line and ellipsise beyond it; the price is never wrapped, shrinking to fit down to a 9 pt floor instead
- Tapping the label on the product form opens it at true printed size, measured from the panel's physical pixel pitch, with its dimensions in millimetres and a close button
- Removed the dead `:feature:qrcode` module — a duplicate of `QrScannerView`, `QrCodeAnalyzer` and `QrGenerator` that nothing depended on and that built on every compile
- Removed committed IDE build output that shadowed the real sources (`core/domain/bin`, `core/domain/model`, `core/domain/repository`, `build-logic/bin`)
- Dropped six module dependencies that were declared but never imported, including `:core:data` from the feature modules, which the layering rules say must not see the data layer at all
- Removed orphaned string resources and cleared every deprecated API the compiler was flagging: `hiltViewModel`'s moved package, `ContextCompat.startActivity`, `Icons.Filled.ArrowForward`, and `window.statusBarColor`
- Fixed the status bar using dark icons on the dark theme, which made them invisible — the light-appearance flag was set to `darkTheme` rather than its inverse
- Redesigned the QR scanner: full-screen camera with a dimmed overlay and a framed scan window, corner brackets, an animated sweep line, a torch toggle on hardware that has a flash, an explicit close button, and an on-screen aiming hint
- Fixed the scanner rendering as a letterboxed rectangle floating over the sale — the dialog kept the platform's default inset width, so `fillMaxSize` only filled the constrained window
- Fixed a scanned code being reported once per decoded frame: between the first hit and the scanner closing, further frames added duplicate items to the cart. The first decode now latches
- Fixed the camera rebinding on every recomposition; it now binds once and unbinds on dispose, and reports failures instead of printing a stack trace
- A successful scan gives haptic feedback, since at a counter the operator is looking at the product rather than the screen
- Backup and restore: a Backup entry on the dashboard writes every product, sale, sale item and product image into a single .zip, saved through the system document picker (Google Drive, device storage, anywhere else installed) or handed straight to the share sheet
- The archive carries a described JSON manifest rather than a copy of the SQLite file, so an older backup can still be read after the schema moves on instead of making Room abort on the identity hash
- Restore is confirmed against the archive's own manifest, read before anything is touched: product, sale, item and image counts, when it was created and which app version wrote it, alongside what is about to be replaced
- Restore applies in a single transaction and replaces rather than merges, so a half-applied archive cannot leave sales pointing at products that were never written
- Extracted every remaining hardcoded UI string in the dashboard, export, history and sales screens into per-package resources, and added full en/es/de translations across app, history, sales and products (110 keys per locale, verified in parity)
- Fixed the payment method dropdown showing the stored wire name (`CREDIT_CARD`) instead of the translated label
- Print now requests portrait A4 explicitly: `ISO_A4` alone carries whatever orientation the print service last used, so a landscape page produced a rotated grid. The adapter also normalises the chosen media to portrait, and logs the size it resolved
- `android:allowBackup` is now per-variant: off on debug so local schema churn is not resurrected by a restore, on for release
- Fixed Auto Backup restoring a pre-rename `bipsale_database` onto a fresh install, which made Room abort on the identity-hash mismatch and crash every screen that opened the database, surviving even a full uninstall
- Room now exports its schema (`exportSchema = true` + `room.schemaLocation`), unblocking the migration harness `CORE_RULES` §13 requires
- Fixed detekt failing on `:app` (wildcard imports, missing trailing newlines, over-long nav host) and taught it that `@Preview` functions are not dead code

## Reconstructed history (before 2026-07-21)

Rebuilt from git history; grouped by month because the per-work-unit record did not exist yet.

### 2026-06

- Consolidated the `conductor/` rule set and added the skills catalogue, workflows and templates
- Applied the strict Timber log-filter convention across the app (REMEDIATION-01)

### 2026-04

- Resolved dependency and import conflicts in `:core:qrcode`
- Tuned Gradle JVM args for a 32 GB machine (G1GC) and bumped the version catalog

### 2026-03

- Refactored Products and Sales onto Clean Architecture with explicit use cases
- Overhauled the README with project specs and development guidelines

### 2026-02

- Initialised the Conductor documentation framework
- Consolidated agent infrastructure and the build environment

### 2026-01

- First working app version
- Migrated to AGP 9.0; fixed the Kotlin DSL, configuration cache and KSP setup
- Fixed the Compose compiler, ProGuard rules and BuildConfig configuration
- Fixed a serialization crash in `:core:navigation`
