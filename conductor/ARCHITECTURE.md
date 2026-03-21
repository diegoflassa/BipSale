# Architecture: BipSale

## Module Graph
```
feature:*  →  core:domain  ←  core:data
feature:*  →  core:ui
feature:*  →  core:navigation
app        →  all
```
Features **never** depend on other features. `core:domain` = pure Kotlin, zero Android deps.

- **`:app`** — `MainActivity`, Hilt app.
- **`:core:domain`** — Entities (`Sale`, `Product`, `SaleItem`, `PaymentMethod`), repo interfaces, use cases.
- **`:core:data`** — Room, DAOs, Retrofit, mappers, Hilt modules.
- **`:core:ui`** — Material 3 design system, shared components.
- **`:core:navigation`** — `Screen` routes, `BipSaleNavHost`.
- **`:core:qrcode`** — QR code utilities.
- **`:feature:sales`** — POS checkout (customer → cart → finalize).
- **`:feature:products`** — Product CRUD + QR generation.
- **`:feature:history`** — Sales history, search, date filter.
- **`:feature:qrcode`** — CameraX QR scanning.
- **`build-logic/`** — Convention plugins + `Configuracoes.kt`.

## MVI Contract (per feature)
Each feature has `XxxContract.kt`:
- `State` — immutable data class via `StateFlow`
- `Intent` — sealed interface (user actions)
- `Effect` — sealed interface (one-shot side effects via `Channel`)
- ViewModel exposes `state: StateFlow<State>`, `effects: Flow<Effect>`; UI calls `onIntent(Intent)`.

## Data Flow (SSOT)
`UI → ViewModel.onIntent() → UseCase → Repository → Room → Flow<T> → StateFlow → UI`

Room is the single source of truth. All repository reads return `Flow<T>`.

## Navigation
Navigation 3 with type-safe `@Serializable` routes. See `core/navigation/.../Screen.kt`.

## Domain Flows
- **Checkout**: product selection → cart (`feature:sales`) → optional CPF → persist via `SaleRepositoryImpl` → reflected in `feature:history`.
- **Products**: loaded from Room as `Flow`, reactive UI updates on inventory changes.

## Key Files
| Path | Purpose |
|------|---------|
| `core/navigation/.../Screen.kt` | Route definitions |
| `core/data/.../BipSaleDatabase.kt` | Room DB |
| `core/data/.../di/DatabaseModule.kt` | Hilt DB singletons |
| `core/data/.../di/RepositoryModule.kt` | Hilt repo bindings |
| `build-logic/.../Configuracoes.kt` | SDK versions, build config |
| `gradle/libs.versions.toml` | Version catalog |
