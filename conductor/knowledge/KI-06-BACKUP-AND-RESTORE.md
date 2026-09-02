# KI-06: Backup & Restore

**Scope:** `core/domain/backup/`, `core/data/backup/`, `app/ui/backup/`
**Last verified:** 2026-09-02

## Problem

The whole dataset — products, sales, sale items and product images — has to leave the device as one file and come back intact, including onto a different phone. Restore overwrites everything, so the format and the confirmation both carry weight.

## Files

| File | Owns |
|---|---|
| `core/domain/backup/BackupRepository.kt` | `createBackup` / `inspectBackup` / `restoreBackup` / `stageBackupForSharing` seam; URIs cross as `String` so the domain stays Android-free |
| `core/domain/backup/BackupSummary.kt` | What an operation moved |
| `core/domain/backup/BackupMetadata.kt` | What an archive claims to hold, read without applying it |
| `core/domain/usecase/CreateBackupUseCase.kt` etc. | One use case per operation; the ViewModel never touches the repository |
| `core/data/backup/BackupDocument.kt` | `@Serializable` manifest DTOs |
| `core/data/backup/BackupRepositoryImpl.kt` | Zip read/write, DAO access, image store access, share staging |
| `app/ui/backup/` | `BackupScreen`, `BackupViewModel`, `BackupContract`, `components/` |

## Archive format

A `.zip` holding:

```
backup.json          the manifest below
images/<fileName>    one entry per product image, names matching Product.imageFileName
```

`backup.json` carries `formatVersion`, `schemaVersion`, `appVersionName`, `createdAt`, then the full `products`, `sales`, `saleItems` and `images` lists, and a `settings` block (rule 10a).

## Business rules

1. **Describe the data, never copy the SQLite file.** A raw database restored onto a build whose schema has moved on makes Room abort on the identity hash rather than open it — the same failure Auto Backup caused ([KI-05](KI-05-PRODUCT-IMAGES-AND-QR-LABELS.md)). Every older archive would become a landmine. A described document can be read, checked and migrated.
2. **Images travel with the data.** A backup without them restores products whose thumbnails are all placeholders.
3. **`formatVersion` gates the read.** An archive newer than the running app is refused with a message naming both versions, not parsed on a guess.
4. **`inspectBackup` reads only `backup.json` and stops.** The confirmation needs counts, not megabytes of image bytes.
5. **Restore replaces; it never merges.** A leftover row is data the operator did not ask to keep and cannot tell apart from restored data afterwards.
6. **Restore is one transaction.** A half-applied archive would leave sales referencing products that were never written.
7. **The image store is cleared before restored images are written,** so images the archive does not carry do not survive it.
8. **Entry names are flattened with `File(name).name` before writing.** A crafted archive entry would otherwise escape the images folder.
9. **Sharing stages into `cacheDir/backup_share`, wiped each time,** and hands out a `FileProvider` URI. One file per share keeps the cache bounded.
10. **The archive carries an optional `settings` block** and `quantity` on every product, both defaulted so an archive written before either existed still restores. Settings are restored outside the database transaction — DataStore is not covered by it. See [KI-09](KI-09-STOCK-SETTINGS-AND-PIX.md).
10a. **The `settings` block holds every configurable value, not a subset.** `askCustomerInfo`, `pixDiscount`, `qrLabelColumns`, `qrLabelNameTextSizePt` and `qrLabelPriceTextSizePt`. A setting the archive skips comes back at its default after a restore, which reads to the operator as work silently undone — so a new setting is added to `BackupSettings` and to **both** mapping directions in the same turn it is added to `AppSettings`.
10b. **Every field is defaulted in `BackupSettings`, and every range is re-clamped on restore.** Defaulting is what lets an older archive restore at all; clamping is because the archive is a file an operator can copy, edit and hand around, so a value from it is untrusted input, not something the app itself wrote.
10c. **PIX key/name/city are write-only history.** They are still written for older readers, and ignored on restore — those values come from `PixDefaults` constants now.

## Storage and Drive

Backup and restore both go through the system document picker (`CreateDocument` / `OpenDocument`). That is what puts Google Drive, the device, and any other installed storage provider in reach **without the app holding Drive credentials or requesting a storage permission**. Sharing goes through `ACTION_SEND` with the `FileProvider` authority `${applicationId}.fileprovider`, declared in the app manifest against `res/xml/file_paths.xml`.

## Confirmation before restore

`RestoreRequested` does not restore. It inspects the archive, and only then opens `RestoreConfirmationDialog`, which shows the archive's product / sale / item / image counts, its creation date, the app version that wrote it, and what is about to be replaced. The confirm action carries the error colour and reads "Apagar e restaurar", never a bare "OK".

An archive that fails inspection reports that immediately, rather than after the operator has already agreed to wipe their data.

## Test targets

| Suite | Pins |
|---|---|
| `core/data/androidTest/.../BackupRepositoryImplTest` | Round trip of products, sales, items and images; exact product field preservation; replace-not-merge; images absent from the archive dropped; a non-archive rejected; empty database backs up; suggested file name; **every configured setting round-trips**, and a setting carried out of range is clamped on restore |
| `app/test/.../BackupViewModelTest` | Archive write, share staging, the inspect-before-restore gate, cancellation, the unreadable-archive path |
| `app/androidTest/.../BackupScreenContentInstrumentedTest` | Summary card fills the content width, shows only after a run, spinner while busy |

**Not yet covered:** the share staging path end to end, and `formatVersion` rejection.

## Gotchas

- `BackupSummary.createdAt` is the archive's timestamp, not the moment of the restore.
- `stageBackupForSharing` writes a *fresh* archive; it does not reuse whatever the last `createBackup` produced.
- Restoring does not touch `allowBackup` — Android's own Auto Backup is a separate mechanism and stays off on debug ([KI-05](KI-05-PRODUCT-IMAGES-AND-QR-LABELS.md)).
