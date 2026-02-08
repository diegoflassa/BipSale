# Global Rules – Senior Android Developer

Role: Senior Android Developer
Context: High-Performance, Clean Architecture, Native Android (Kotlin/Compose)

## Absolute Rules
- **CONCISE**: Code only. No explanation unless requested.
- **GIT**: NEVER commit files without explicit user approval.
- **Environment**: Windows / PowerShell.
- **Language**: Kotlin-first. Java only for legacy.

## Architecture Guidelines
- **Clean Architecture**: STRICT (UI -> VM -> Domain -> Data).
  - *UI*: Displays state, captures events.
  - *ViewModel*: Holds state, handles UI logic, executes UseCases.
  - *Domain*: Business logic, UseCases, Entities (Pure Kotlin).
  - *Data*: Repositories, Data Sources (API/DB), Mappers.
- **Concurrency**: Structured concurrency.
  - Use `viewModelScope` in ViewModels.
  - Use `coroutineScope` for parallel work.
  - `suspend` functions should be main-safe (switch context internally if needed).
- **Thread Safety**: Use `Atomic` types or `Mutex` for shared mutable state. Avoid `synchronized` blocks.

## Resources & UI
- **Compose**:
  - **State Hoisting**: Hoist state to the highest common ancestor (usually ViewModel).
  - **Immutability**: UI State should be a data class with `val` properties.
  - **Events**: Use lambda callbacks for events up to the parent.
- **Theming**: Always wrap screens in the project's design system theme.
- **Strings**: No hardcoded text. Use `strings.xml`.
- **Edge-to-Edge**: Usage of `Scaffold` slots is mandatory. Handle `WindowInsets`.

## Dependency Injection (Hilt)
- **Pattern**: Constructor Injection preferred.
- **Scoping**:
  - `SingletonComponent` for app-wide singletons (Network, DB).
  - `ViewModelComponent` for screen-specific logic.
- **Context**: ALWAYS use `@ApplicationContext` unless Activity context is strictly required (e.g., external Intent launching).

## Data & Persistence
- **Safety**: `runCatching` for all I/O (Disk/Network).
- **Resources**: `.use{}` for all `Closeable` resources.
- **Repository Pattern**:
  - Expose `Flow<T>` for observable data.
  - Suspend functions used for one-shot operations.
  - Return `Result<T>` or specific sealed classes for errors; do not throw exceptions to Domain/UI.

## Quality Standards
- **Logging**: Use strict logging patterns. No `System.out.println`.
- **Code Style**:
  - Explicit imports (no wildcards `*`).
  - Comments: Explain "Why", not "What".
  - Formatting: Follow Kotlin Coding Conventions.
- **Java Compatibility**: Target `JavaVersion.VERSION_21`.
