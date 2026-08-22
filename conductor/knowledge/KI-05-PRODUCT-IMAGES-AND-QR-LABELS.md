# KI-05: Product Images & QR Label Printing

**Scope:** `:feature:products`, `:core:qrcode`, `core/data/image/`, `core/domain/image/`
**Last verified:** 2026-08-22

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
14. **The grid is derived from `PrintAttributes.mediaSize`,** so any paper the operator picks re-fits. A4 is only what `QrLabelPrinter` requests as the default.
15. **A4 yields a 4x5 grid, 20 labels per sheet,** ~47x55 mm per cell with a ~30 mm QR — dense enough to save paper, large enough to scan.
16. **`forPage` never returns a zero-cell grid.** Columns and rows are floored to at least 1; a zero would make the page loop spin without advancing.
17. **Cut borders are dashed vectors** drawn at page resolution, not raster.
18. **QR bitmaps are drawn unfiltered** (`isFilterBitmap = false`) — smoothing the modules costs scan reliability at label size.
19. **The edit screen preview uses the same `drawLabel`** at the same proportions, so the preview is what prints.

## Log filters

`[BipSale][Product]`, `[BipSale][Product][IMAGE]`, `[BipSale][Product][QR_EXPORT]` — catalogued in [KI-04](KI-04-LOG-FILTERS.md), which is the SOT. The print path logs the resolved media size and grid shape, so a wrong-looking sheet is diagnosable from a capture alone.

## Test targets

| Suite | Pins |
|---|---|
| `core/domain/.../PriceInputTest` | Comma and dot separators, zero/negative rejection, multi-separator refusal |
| `core/domain/.../SaveProductUseCaseTest` | Code/name/price validation, image name pass-through, QR payload shape |
| `core/qrcode/.../QrLabelSheetLayoutTest` | A4 4x5 grid, pagination boundaries, cell geometry, tiny-paper guard |
| `core/data/androidTest/.../ProductImageStoreImplTest` | Import succeeds against a real `ContentResolver` (pins the bounds-decode bug), 1024 px downscale, code sanitising, unique name per pick, delete, unreadable-source failure |

**Not yet covered:** `ProductViewModel` (price validation wiring, image lifecycle), EXIF rotation, `QrLabelSheetRenderer` (needs Android graphics). See [KI-TBD](KI-TBD.md) #3 and #5.

## Failure visibility

Every screen that sends effects hosts a `SnackbarHost` and handles **every** branch of the `Effect` `when` — no `else -> {}`, no partial `if (effect is ...)`.

A screen that collects only the branch it navigates on drops the rest silently: a failed image import and a print request both produce no snackbar, no print dialog, and no visible change, which is indistinguishable from a dead button. Exhaustive handling is what makes the `when` fail to compile when a new effect is added, instead of the new effect quietly going nowhere.

## Gotchas

- `ProductViewModel` is shared by the list and edit screens but each gets its own instance via its own `NavBackStackEntry`, so `editor` state does not leak between them.
- `SaveProductUseCase` recomputes `qrCode` from code + price, so changing a price invalidates any label already printed for that product.
- `Editor.canSave` requires `label != null`, which is itself gated on a parseable price — that is what keeps the Save button off for a malformed amount.
