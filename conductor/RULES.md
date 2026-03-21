# Rules: BipSale

## Coding
- Domain: `core:domain` = Pure Kotlin. Zero Android deps. Center of the app (DDD).
- Dependency Inversion: outer layers depend on domain interfaces.
- Immutability: always `val` and `data class`.
- Concurrency: `suspend` functions must be main-safe.
- Error handling: `Result<T>` or sealed error classes at domain/data boundaries. Use `runCatching`.
- Logging: Timber only — no `Log.d/e/w`.
- Features isolation: features never depend on each other.

## Build
- Convention plugins in `build-logic/` — `android-application-convention`, `android-library-convention`.
- SDK/Kotlin versions defined in `build-logic/.../Configuracoes.kt` (single source of truth).
- Build count auto-incremented in `version.properties`; format `0.0.2-alpha-build_N`.
- Static analysis: `./gradlew detekt`, `./gradlew ktlintCheck`.
