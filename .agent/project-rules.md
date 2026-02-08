# BipSale Project Rules (CRITICAL)

**Context**: Sale Management Software.

## Cheatsheet (High Priority)
- **Architecture**: `feature:*` modules depend on `core:domain` and `core:ui`.
- **UI**: Use `BipSaleTheme`. No hardcoded strings.
- **DI**: Use Hilt (`@HiltViewModel`, `@SingletonComponent`) + KSP.
- **IO**: All Database/Network operations in `runCatching` on `Dispatchers.IO`.

## Absolute Constraints
- **Domain Purity**: `core:domain` must be Pure Kotlin. NO Android dependencies (Context, R, Parcelable).
- **Navigation**: Use Type-Safe Navigation in `core:navigation`.
- **Logging**: `Timber` only.
  - Debug: `Timber.d("MSG")`
  - Error: `Timber.e(e, "MSG")`
- **Module Isolation**: Features (Sales, History, etc.) MUST NOT depend on each other directly.

## Technology Stack (Strict)
- **Kotlin**: 2.3.0
- **Compose**: BOM 2026.01.00 (Material 3).
- **Persistence**: Room 2.8.4 + DataStore.
- **Network**: Retrofit 2 + Moshi.

## Data Handling
- **Fiscal Logic**: Tax/Price calculations MUST be in `core:domain` use cases.
- **Images**: Use `coil-compose` for loading.
- **Resources**: Manage all strings/assets in `core:ui` or `app` (if global).
