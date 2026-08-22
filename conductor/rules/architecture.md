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
feature:* → core:ui | core:qrcode
app       → all
```

| Module | Owns |
|---|---|
| `:app` | `MainActivity`, Hilt `Application`, export UI |
| `:core:domain` | Entities, repository interfaces, use cases |
| `:core:data` | Room (`BipSaleDatabase`, DAOs), Retrofit, mappers, Hilt DI |
| `:core:ui` | Material 3 theme, shared composables |
| `:core:navigation` | Routes, `BipSaleNavHost` |
| `:core:qrcode` | QR encode/decode, CameraX scanner UI, printable label rendering |
| `:feature:sales` | POS / checkout |
| `:feature:products` | Product CRUD + QR generation |
| `:feature:history` | Sales history + search |
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

- **A testability seam is a named interface, never a bare lambda.** When a collaborator is injected so tests can swap it — a clock, a device-id reader, a sale-id or idempotency-key generator, a randomness source — declare a named `interface XxxProvider` in `core:domain`, `@Binds` the production impl in `RepositoryModule`, and write a fake for it in the consuming module's `src/test`. Never inject `now: () -> Long` or `idGenerator: () -> String`. A lambda binds under no meaningful key, so two `() -> Long` dependencies in one Hilt graph collide unless every one of them gets a `@Qualifier` you then have to keep straight; an interface can grow a second method when a test needs to advance time, where a lambda forces rebuilding its captured state; and the constructor keeps the dependency visible *as* a dependency. This is the same reasoning as the repository interfaces above, applied to the small non-domain collaborators that would otherwise arrive as lambdas. It governs the **shape** of a seam once one is warranted — it is not licence to abstract single-call-site code (`ai_behavior.md` §2).

## Concurrency

- `suspend` for all async work; ViewModels use `viewModelScope` (Main-safe).
- I/O wraps in `withContext(Dispatchers.IO)` inside the repository or use case.
- Errors: `runCatching {}` → `Result<T>` / sealed result. No bare try/catch outside a `runCatching` lambda.
- Immutability: `val` / `data class` everywhere in state.

### Cancellation must never be swallowed (MANDATORY)

`CancellationException` is an `Exception`. A `catch (e: Exception)` / `catch (t: Throwable)` around a suspending call swallows it unless it re-throws — and that exception is how the coroutine machinery tells a parent scope its child actually stopped. Swallow it and the parent believes the work completed: cleanup runs against an already-dead job, a `viewModelScope` cancelled by screen teardown still walks the success branch, and a retry loop keeps spinning after its scope is gone.

**`runCatching {}` has the identical defect** — it catches `Throwable`. Since this project mandates `runCatching` over bare try/catch, that is where the guard belongs:

```kotlin
runCatching { saleRepository.persist(sale) }
    .onFailure { e ->
        if (e is CancellationException) throw e
        Timber.e(e, "[BipSale][Sale][CHECKOUT] persist failed")
    }
```

For any generic `catch` that does survive — inside a `runCatching` lambda, or in a CameraX / Retrofit adapter — the guard is either a `catch (e: CancellationException) { throw e }` clause placed **before** the generic one, or `currentCoroutineContext().ensureActive()` as its first statement (a no-op on the genuine failure path).

- **Not optional on the checkout path.** `FinalizeSale` moves money. A swallowed cancellation there lets the UI report a completed sale for a write whose scope died mid-flight — the receipt says paid and Room has no row, which is exactly the sale nobody can reconstruct that [CORE_RULES §8.2](CORE_RULES.md) is written to prevent.
- **Cancellation is not an error to report.** Never log it at `Timber.e` level, never send it to Crashlytics, never map it to a user-facing failure state — the user navigating away from a screen is not a failed sale.
- **Exception:** a `catch` that only releases a resource and re-throws is already correct. A `catch` inside a `suspendCancellableCoroutine` callback body is not in the suspending context and is out of scope.

## Persistence

**Room is the single source of truth.** `BipSaleDatabase` + DAOs + mappers; reactive UI via `Flow`. Remote access via Retrofit is a cache-fill path, never a second source of truth. Migration rules: [CORE_RULES §13](CORE_RULES.md).

**Recovering an interrupted sale is a persistence concern, not a saved-state one.** After process death, whether a checkout landed is answered by reading the sale row back from Room — never by a restored `isSaleFinished` / "checkout in progress" flag. Saved state and the persisted row must not both own that decision; the row wins, because it is the only one of the two that survived for the right reason. Which UI fields may be saved at all: [`COMPOSE_RULES.md` §2](COMPOSE_RULES.md).

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
- Static analysis: detekt, applied in `app/build.gradle.kts` only. The ktlint plugin is declared `apply false` at the root and never applied — there is no `ktlintCheck` task (KI-TBD #9). Coverage: `koverHtmlReport`.
