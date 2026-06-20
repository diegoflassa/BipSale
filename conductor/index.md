# CONDUCTOR — BipSale

Documentation hub for BipSale across all AI models.

> Entry point is `../AGENTS.md` — this file is the doc map and project context. Load KIs lazily.

---

## Bootstrap (All Models)

1. `../AGENTS.md` — project essentials + critical rules
2. This file — documentation map + project context
3. `guides/bootstrap-ai.md` — agnostic init sequence + escalation matrix

---

## Documentation Map

### Rules (Read First)
- **[ai_behavior.md](rules/ai_behavior.md)** — AI behavior rules (think-first, surgical, simplicity)
- **[CORE_RULES.md](rules/CORE_RULES.md)** — operational + project rules (git safety, token economy, no-inline-FQN, KI/planning discipline, Hilt, Room, Timber, build)
- **[COMPOSE_RULES.md](rules/COMPOSE_RULES.md)** — Compose rules (stability, recomposition, memory, animations, accessibility)
- **[PREVIEW_STANDARD.md](rules/PREVIEW_STANDARD.md)** — `@Preview` standard (device profiles, theme wrapper, localized mock data, ≥2 states)
- **[INSTRUMENTED_TEST_STANDARD.md](rules/INSTRUMENTED_TEST_STANDARD.md)** — screens must be instrumented-test friendly

### Knowledge (SOT) → [Full Index](ki/KI_INDEX.md)

### Skills (Lazy-Load) → `skills/`

> Read a skill only when its exclusive purpose matches the task. Never pre-load.

| Skill | Purpose | Load when |
|---|---|---|
| [testing-setup](skills/testing-setup/SKILL.md) | Test infra (unit / UI / screenshot / E2E) | Adding or auditing test layers |
| [edge-to-edge](skills/edge-to-edge/SKILL.md) | Compose system-bar / IME inset handling | Content obscured by system bars or keyboard |
| [android-cli](skills/android-cli/SKILL.md) | ADB / device orchestration (deploy, logcat, screenshots) | Running on device, capturing screenshots |
| [r8-analyzer](skills/r8-analyzer/SKILL.md) | R8/ProGuard keep-rule audit + APK size | Optimizing size, debugging release minification |
| [perfetto-trace-analysis](skills/perfetto-trace-analysis/SKILL.md) | Runtime jank / latency / memory root-cause | Investigating janky transitions, slow loads, memory spikes |

### Plans & Workflows → [Plans](plannings/INDEX.md) | [Workflows](workflows/INDEX.md) | [Templates](templates/INDEX.md)

---

## Critical Reminders

1. Check relevant KI before code changes.
2. No build / no commit without user authorization.
3. `conductor/` = primary docs. Load KIs lazily via `ki/KI_INDEX.md`.

---

## Project Context

### ARCH

Graph: `feat:*->core:domain<-core:data | feat:*->core:ui | feat:*->core:nav | app->all`
Constraint: `Features isolated. core:domain=Pure Kotlin (0 Android Deps).`

Modules:
- `:app:[MainActivity, Hilt App]`
- `:core:domain:[Entities, Repo Intf, Use Cases]`
- `:core:data:[Room, DAO, Retrofit, Mappers, Hilt DI]`
- `:core:ui:[M3, Shared UI]`
- `:core:navigation:[Routes, BipSaleNavHost]`
- `:core:qrcode:[QR Utils]`
- `:feature:sales:[POS/Checkout]`
- `:feature:products:[CRUD/QR Gen]`
- `:feature:history:[History/Search]`
- `:feature:qrcode:[CameraX/Scan]`
- `build-logic:[Convention Plugins, Configuracoes.kt]`

MVI (per feat):
`XxxContract.kt -> State(Flow), Intent(Sealed), Effect(Channel), VM(state:Flow, effect:Flow, onIntent())`

Flow: `UI->VM.onIntent()->UC->Repo->Room(SSOT)->Flow->StateFlow->UI`

Nav: `Nav 3 + @Serializable (core/navigation/Screen.kt)`

Dom Flows:
- Checkout: `prod->cart(feat:sales)->CPF->SaleRepoImpl->feat:history`
- Products: `Room(Flow)->Reactive UI`

Keys:
- `core/navigation/Screen.kt:Routes`
- `core/data/BipSaleDatabase.kt:Room`
- `core/data/di/DatabaseModule.kt:Hilt DB`
- `core/data/di/RepositoryModule.kt:Hilt Repo`
- `build-logic/Configuracoes.kt:SDK/Cfg`
- `gradle/libs.versions.toml:Catalog`

---

### RULES

Coding:
- Domain: Pure Kotlin/DDD
- DI: Inversion (Outer->Dom Intf)
- Immut: val/data class
- Conc: suspend (MainSafe)
- Err: Result<T>|Sealed via runCatching
- Log: Timber only
- Iso: Features isolated

Build:
- Plugins: build-logic (app/lib conv)
- SSOT: build-logic/Configuracoes.kt
- Ver: version.properties (0.0.2-alpha-build_N)
- Static: detekt/ktlint

---

### CLI

Build: `./gradlew assembleDebug | assembleRelease | bundleRelease`
Test: `./gradlew test | :feature:sales:test | connectedAndroidTest`
Analysis: `detekt | ktlintCheck | ktlintFormat`
Cov: `koverHtmlReport`

---

### AUDIT

KI-003 (2026-03-21): Merged to conductor. Pruned redundancy.

---

### KI

Index: `conductor/ki/KI_INDEX.md` (read first; fetch individual KIs only on match)
