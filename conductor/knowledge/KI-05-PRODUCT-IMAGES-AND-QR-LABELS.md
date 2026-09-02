# KI-05: Product Images & QR Label Printing

**Scope:** `:feature:products`, `:core:qrcode`, `core/data/image/`, `core/domain/image/`
**Last verified:** 2026-09-02

## Problem

Products carry a photo and print as cut-out QR labels. Both paths have failure modes that are cheap to reintroduce: an image stored by absolute path breaks on restore, and a label sheet composed as bitmaps exhausts the heap. This KI is the contract for both.

## Files

| File | Owns |
|---|---|
| `core/domain/model/Product.kt` | `imageFileName: String?` — a **file name**, never a path |
| `core/domain/image/ProductImageStore.kt` | `save` / `delete` / `resolvePath` seam; pure Kotlin, takes the picker URI as a `String` |
| `core/data/image/ProductImageStoreImpl.kt` | Copy-in, downscale, EXIF rotation, delete; all on `Dispatchers.IO` |
| `core/domain/util/PriceInput.kt` | `parsePriceInput` — the only place a typed price becomes a `Double` |
| `core/domain/usecase/SaveProductUseCase.kt` | Validation + `buildQrPayload`, the single QR payload format |
| `core/domain/usecase/SaveProductImageUseCase.kt` | Import a new image and drop the one it replaces |
| `core/domain/usecase/DeleteProductUseCase.kt` | Delete the row **and** its image file |
| `core/qrcode/QrLabelSheetLayout.kt` | Pure grid maths in points; unit-tested |
| `core/qrcode/QrLabelTypography.kt` | Configured name/price point sizes; derives line height and the price shrink floor |
| `core/qrcode/AppSettingsExt.kt` | `AppSettings.toQrLabelTypography()` — the one place settings widen into renderer points |
| `core/qrcode/QrLabelSheetRenderer.kt` | Draws labels onto a page canvas and the on-screen preview |
| `core/qrcode/QrGenerator.kt` | ZXing encode to a `Bitmap` |
| `feature/products/print/QrLabelPrintAdapter.kt` | `PrintDocumentAdapter` emitting a real multi-page PDF |
| `feature/products/print/QrLabelPrinter.kt` | Opens the system print dialog with A4 requested |
| `feature/products/components/` | `ProductItem`, `ProductThumbnail`, `ProductImagePicker` |

## Business rules

### Images

1. **Store the file name, not a path.** `filesDir` absolute paths embed the Android user id; after an Auto Backup restore or on a secondary profile they point at nothing. `ProductImageStore.resolvePath` is the only thing that turns a name into a path, and it happens in the ViewModel, not in a composable.
2. **Never persist the picker URI.** `PickVisualMedia` grants a one-shot read permission that is not persistable — `takePersistableUriPermission` throws for photo-picker URIs. The image is copied in on pick.
3. **A new pick gets a new file name** (`<sanitized-code>_<epochMillis>.jpg`). Reusing the name leaves the image loader serving the bitmap it cached under that path.
4. **Product codes are sanitized** to `[A-Za-z0-9-_]` before use as a file name.
5. **Images are downscaled to 1024 px on the long edge** and re-encoded as JPEG at quality 85, with EXIF orientation applied. A full-resolution copy per product would run to hundreds of MB.
6. **Deleting a product deletes its image.** `DeleteProductUseCase` owns both halves.
7. **Bounds decoding null-checks the stream, never the decode result.** `BitmapFactory.decodeStream` returns **null on success** when `inJustDecodeBounds` is set — the dimensions land in the `Options`. Binding an elvis to that result rejects every image ever picked, and does it silently.
8. **Never call `File.exists()` in composition.** `ProductThumbnail` routes a missing file to the loader's error slot instead; probing puts a disk read in every frame of a `LazyColumn`.

### Price

9. **`parsePriceInput` accepts both `,` and `.`.** A pt-BR operator types `130,50`; `String.toDoubleOrNull` alone returns null, and defaulting that to `0.0` sells the product for nothing.
10. **It rejects zero, negatives, non-finite values, and multi-separator input** (`1.234,56` is refused rather than guessed at).
11. **`SaveProductUseCase` re-validates** — the form is not the boundary, the use case is.
12. **Currency is formatted in the ViewModel** with `NumberFormat.getCurrencyInstance(pt-BR)` and arrives on the UI model ready to render.

### Printing

13. **Print through `PrintDocumentAdapter` + `PdfDocument`, never a single composed bitmap.** A full-page ARGB_8888 bitmap at print resolution is ~33 MB, so allocating one per page and concatenating them exhausts the heap past a single page. A printed bitmap is also always *one* page, so that route squashes an entire batch onto a single sheet at unscannable size.
14. **The grid is derived from `PrintAttributes.mediaSize`,** so any paper the operator picks re-fits. Portrait A4 is what `QrLabelPrinter` requests as the default.
14a. **Media is pinned portrait with `asPortrait()`** at both the request and in `onLayout`. `ISO_A4` on its own inherits whatever orientation the print service last used, and a landscape page lays the grid out rotated.
15. **A4 defaults to a 5-column grid** — 25 labels per sheet, dense enough to save paper and still large enough to scan. Two different defaults are in play and they are not the same number: `QrLabelSheetLayout.forPage` **auto-fits** 4 columns on A4 when asked for none, while `AppSettings.DEFAULT_QR_LABEL_COLUMNS` is **5** and is what the app actually requests. The auto-fit is the fallback for paper nobody configured for; the setting is the shop's choice. The operator can pick 1 to 6 in Settings, which scales the label size accordingly.
16. **`forPage` never returns a zero-cell grid.** Columns and rows are floored to at least 1; a zero would make the page loop spin without advancing.
17. **Cut borders are dashed vectors** drawn at page resolution, not raster.
18. **QR bitmaps are drawn unfiltered** (`isFilterBitmap = false`) — smoothing the modules costs scan reliability at label size.
19. **The edit screen preview uses the same `drawLabel`** against a cell built from **the configured column count and typography**, not `QrLabelSheetLayout.a4()`, so the preview is what prints. The column count sets the cell width, so a preview drawn at the auto-fit width is a different label from the one in the tray — and `RealSizeLabelDialog` quotes millimetres the operator holds a ruler against, so it takes the same layout. `ProductViewModel` keeps `State.labelTypography` in step with settings by collecting the settings flow, not by reading it once — a preview that only refreshed on the print path would show the defaults forever.
20. **Type is sized in points, and the operator sets it** — the product name and the price are separate settings, 6–24 pt, defaulting to 10 pt and 13 pt. Points survive any paper size or printer DPI; pixels would not. `QrLabelTypography` is the only carrier: it takes the two configured sizes and **derives** the name line height (×1.2) and the price shrink floor (×0.7), so a size and its dependants can never drift apart. `QrLabelSheetRenderer` holds no type-size constants of its own.
20a. **The two sizes are configured independently.** A name identifies the item on a shelf and a price is the number someone is charged; a shop that wants one bigger rarely wants both, and one shared size makes that impossible to express.
21. **The name block is reserved at `MAX_NAME_LINES` height whether or not the name fills it,** so every QR on a sheet sits at the same offset and the cut lines stay a regular grid. The line count is derived from the cell and the configured type, not fixed.
22. **The name wraps and then ellipsises; the price never wraps.** A price too wide for the cell shrinks by half-points to the derived floor — a wrapped or clipped price is a misread charge.
23. **The real-size preview converts through `DisplayMetrics.xdpi`/`ydpi`, not the density bucket.** A bucket is rounded to the nearest standard density, so a ruler held to the screen would disagree with the printout.

## Log filters

`[BipSale][Product]`, `[BipSale][Product][IMAGE]`, `[BipSale][Product][QR_EXPORT]` — catalogued in [KI-04](KI-04-LOG-FILTERS.md), which is the SOT. The print path logs the resolved media size and grid shape, so a wrong-looking sheet is diagnosable from a capture alone.

## Test targets

| Suite | Pins |
|---|---|
| `core/domain/.../PriceInputTest` | Comma and dot separators, zero/negative rejection, multi-separator refusal |
| `core/domain/.../SaveProductUseCaseTest` | Code/name/price validation, image name pass-through, QR payload shape |
| `core/qrcode/.../QrLabelSheetLayoutTest` | A4 auto-fit grid, the requested-column override, pagination boundaries, cell geometry, tiny-paper guard |
| `core/qrcode/.../QrLabelTypographyTest` | Line height clears the glyphs, the shrink floor sits below its starting size, both derive from the configured size, and the renderer default equals what `AppSettings.EMPTY` hands out |
| `core/qrcode/androidTest/.../QrLabelSheetRendererTest` | Also pins that a larger configured size genuinely renders different ink, and that the QR still scans at the 24 pt maximum |
| `core/data/androidTest/.../ProductImageStoreImplTest` | Import succeeds against a real `ContentResolver` (pins the bounds-decode bug), 1024 px downscale, code sanitising, unique name per pick, delete, unreadable-source failure |

**Not yet covered:** EXIF rotation in `ProductImageStoreImpl` — it needs a fixture photo carrying an orientation tag, and is the one part of the image path with no test. Everything else here is pinned: `ProductViewModel` by its own suite, and `QrLabelSheetRenderer` by the instrumented suite above.

## Backup

`android:allowBackup` is a manifest placeholder set per build type in `android-application-convention.gradle.kts` — `false` on debug, `true` on release.

Auto Backup restores a database written by an older build onto a newer one, and Room aborts on the identity-hash mismatch rather than opening it. On debug that resurrects local schema churn and survives even a full uninstall, which reads as an unkillable crash. Release keeps backup on so sales history survives a device migration; that is safe because `BipSaleDatabase.MIGRATIONS` is registered on the builder and `BipSaleDatabaseMigrationTest` fails in CI if a version is bumped without a migration or an exported schema (`CORE_RULES` §13).

## Failure visibility

Every screen that sends effects hosts a `SnackbarHost` and handles **every** branch of the `Effect` `when` — no `else -> {}`, no partial `if (effect is ...)`.

A screen that collects only the branch it navigates on drops the rest silently: a failed image import and a print request both produce no snackbar, no print dialog, and no visible change, which is indistinguishable from a dead button. Exhaustive handling is what makes the `when` fail to compile when a new effect is added, instead of the new effect quietly going nowhere.

## Gotchas

- `ProductViewModel` is shared by the list and edit screens but each gets its own instance via its own `NavBackStackEntry`, so `editor` state does not leak between them.
- `SaveProductUseCase` recomputes `qrCode` from code + price, so changing a price invalidates any label already printed for that product.
- `Editor.canSave` requires `label != null`, which is itself gated on a parseable price — that is what keeps the Save button off for a malformed amount.
