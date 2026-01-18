---
trigger: always_on
---

# Project Rules
(Specific to BipSale's Implementation & Libraries)
- **Rules File Location**: This file is located at .\.agent\bipsale-rules.md
- **Rules File Updates**: Please update this file when you think a new rule should be added
- **When Update the Rules File**: Please verify for any possible new rule after every task performed and add it this file, if needed
- **Rules File Context**: Please keep the global rules (GEMINI.md file) separated of the project rules (this file)
- **Rules File Format**: Keep this file format consistent, when edited
- **Rules Change Notification**: Notify me of every added rule, and the reason for it

## Architecture & Layers
- **Module Hierarchy**: `feature` modules depend on `core:domain` (Logic) and `core:ui` (Design). NEVER depend on `core:data`.
- **Data Layering**: `core:data` implements `core:domain` repositories. Do NOT duplicate Repository interfaces in `core:data` (Single Source of Truth).
- **MVI Pattern**: ViewModels must implement MVI via a Contract defining `State` (Data Class), `Intent` (Sealed Interface), and `Effect` (Sealed Interface).

## Resources & UI
- **Localization**: Features MUST have their own `res/values/strings.xml`. Do NOT use `app` module strings in features.
- **Components**: All Composables must be wrapped in `BipSaleTheme` (from `core:ui`).
- **Previews**: `@Preview` must use `PreviewParameterProvider` or mock data. No real connections.
- **Image Loading**: Use `Coil` via `AsyncImage`.
- **Text Handling**: Use `UiText` (from `core:ui`) to pass string resources or dynamic strings from ViewModel to UI (e.g., for logic-dependent strings or One-Off events). Avoid resolving Strings in ViewModel using Context when possible.

## Dependency Injection (Hilt)
- **Repositories**: Install in `SingletonComponent`. Use `@Binds` for interface implementation.
- **ViewModels**: Use `@HiltViewModel` and `@Inject`.

## Data & Persistence
- **Room**: Use `Flow` for reactive queries. Always map `Entity` to `DomainModel` before returning to Domain layer.
- **Retrofit/Network**: Use `Moshi` for JSON parsing. Wrap network calls in Repository and handle `Result/Exceptions` gracefully.

## Libraries & Sensors
- **CameraX**: Handle permissions (Manifest + Runtime) before invoking Camera components. Use `PreviewView` with proper lifecycle binding.
- **Firebase**: Use `Timber` for reporting Crashlytics errors (e.g. `Timber.e(e)`).

## Quality & Standards
- **Static Analysis**: Run `detekt` and `ktlint` before pushing.
- **Testing**: Use `runTest` for Coroutines. Map Domain models in tests.
- **Logging**: Use `TimberLogger` for ALL logs. Report exceptions to `FirebaseCrashlytics`.
- **Tracing**: Log the 'Start' and 'End' of important business logic flows (e.g., Scanning, Loading Stats).

## Build & Gradle
- **Version Catalog**: Use `libs.versions.toml` for all dependencies.
- **Plugins**: Use Convention Plugins (in `build-logic`) to share build configuration.