# Changelog — BipSale

All notable changes to this project. Governed by [`conductor/rules/CORE_RULES.md`](conductor/rules/CORE_RULES.md) — one line per work unit (not per commit), appended to `## Unreleased` in the same turn the work lands:

```
- TEXT_OF_THE_FIX (CODE_OF_THE_FIX)
```

Omit the `(CODE)` suffix when no ticket exists. On a release cut, retitle `## Unreleased` to `## [X.Y.Z] YYYY-MM-DD` and add a fresh empty `## Unreleased` above it.

> History before 2026-07-21 lives in git only — this file starts here.

## Unreleased

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
- Added unit coverage for price parsing, product save validation, and A4 label sheet pagination (25 tests, the project's first)
- Fixed detekt failing on `:app` (wildcard imports, missing trailing newlines, over-long nav host) and taught it that `@Preview` functions are not dead code
