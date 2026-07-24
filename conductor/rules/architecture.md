# Architecture SOT — BipSale

Single home for the module graph, layer rules, and package structure. `index.md` and `CORE_RULES.md` point here rather than restating it.

---

## Governing Principle

```
UI (Screen + ViewModel) → UseCase → Repository → Room (SSOT) / Retrofit
```

Dependencies flow inward only. Outer layers depend on **domain interfaces**, never on concrete implementations.

| Layer | Package | Allowed dependencies |
|---|---|---|
| **UI** | `feature/<name>/…` | its own ViewModel, `core:ui`, `core:navigation`, Compose |
| **ViewModel** | `feature/<name>/…` | use cases, domain models, Timber. Never a repository directly, never a `NavController` |
| **Domain** | `core:domain` | Nothing. **Pure Kotlin, zero Android deps.** Holds entities, repository interfaces, use cases. |
| **Data** | `core:data` | Room, Retrofit, mappers; implements `core:domain` interfaces |

Rules:
- **`core:domain` is pure Kotlin/DDD.** No `Context`, no `Log`, no Room, no Retrofit types.
- **Dependency inversion is the rule, not a preference.** `core:data` depends on `core:domain`, never the reverse.
- **Entities are Room-only.** Map at the data/domain seam; domain models are the contract.
- **Features are isolated** — never `feature → feature`. Shared surface goes through `core:*`.

## Module Graph

```
feature:* → core:domain ← core:data
feature:* → core:ui | core:navigation | core:qrcode
app       → all
```

| Module | Owns |
|---|---|
| `:app` | `MainActivity`, Hilt `Application`, export UI |
| `:core:domain` | Entities, repository interfaces, use cases |
| `:core:data` | Room (`BipSaleDatabase`, DAOs), Retrofit, mappers, Hilt DI |
| `:core:ui` | Material 3 theme, shared composables |
| `:core:navigation` | Routes, `BipSaleNavHost` |
| `:core:qrcode` | QR encode/decode utilities |
| `:feature:sales` | POS / checkout |
| `:feature:products` | Product CRUD + QR generation |
| `:feature:history` | Sales history + search |
| `:feature:qrcode` | CameraX scanning |
| `build-logic` | Convention plugins, `Configuracoes.kt` |

## MVI Contract

```
XxxContract.kt → State (exposed as StateFlow) | Intent (sealed) | Effect (Channel)
XxxViewModel   : state: StateFlow, effect: Flow, onIntent()
```

Screens are stateless: `XxxScreen(state, onIntent)`. Wiring lives in the route-level composable.

Flow: `UI → VM.onIntent() → UseCase → Repository → Room (SSOT) → Flow → StateFlow → UI`

## Domain Flows

| Flow | Path |
|---|---|
| Checkout | product → cart (`feature:sales`) → CPF → `SaleRepositoryImpl` → `feature:history` |
| Products | Room `Flow` → reactive UI |

## Navigation

Nav 3, type-safe. `@Serializable` routes declared in `core/navigation/Screen.kt`. Screens never hold a `NavController` — navigation is triggered through `Effect` or injected `() -> Unit` callbacks.

## Dependency Injection

**Hilt.** Modules live in `core/data/di/` (`DatabaseModule`, `RepositoryModule`). Bind interfaces to implementations with `@Binds`; ViewModels via `@HiltViewModel` + `hiltViewModel()`.

## Concurrency

- `suspend` for all async work; ViewModels use `viewModelScope` (Main-safe).
- I/O wraps in `withContext(Dispatchers.IO)` inside the repository or use case.
- Errors: `runCatching {}` → `Result<T>` / sealed result. No bare try/catch outside a `runCatching` lambda.
- Immutability: `val` / `data class` everywhere in state.

## Persistence

**Room is the single source of truth.** `BipSaleDatabase` + DAOs + mappers; reactive UI via `Flow`. Remote access via Retrofit is a cache-fill path, never a second source of truth. Migration rules: [CORE_RULES §13](CORE_RULES.md).

## Key Files

| Concern | File |
|---|---|
| Routes | `core/navigation/Screen.kt` |
| Room DB | `core/data/BipSaleDatabase.kt` |
| Hilt DB module | `core/data/di/DatabaseModule.kt` |
| Hilt repo module | `core/data/di/RepositoryModule.kt` |
| SDK / build config SSOT | `build-logic/Configuracoes.kt` |
| Dependency catalog | `gradle/libs.versions.toml` |

## Package Structure (per feature module)

```
feature/<name>/src/main/java/.../
├── <Name>Contract.kt      # State | Intent | Effect
├── <Name>ViewModel.kt
├── <Name>Screen.kt        # stateless layout
├── components/            # feature-local composables
└── di/                    # Hilt module(s)
```

Shared widgets go in `core:ui`. Domain models shared across features live in `core:domain`.

## Build

- Convention plugins in `build-logic/` (app + library variants).
- Config SSOT: `build-logic/Configuracoes.kt`.
- Versioning: `version.properties` (`0.0.2-alpha-build_N`).
- Static analysis: detekt + ktlint. Coverage: `koverHtmlReport`.
