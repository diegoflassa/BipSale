# BipSale:MANIFEST

## ARCH
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

## RULES
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

## CLI
Build: `./gradlew assembleDebug | assembleRelease | bundleRelease`
Test: `./gradlew test | :feature:sales:test | connectedAndroidTest`
Analysis: `detekt | ktlintCheck | ktlintFormat`
Cov: `koverHtmlReport`

## AUDIT
KI-003 (2026-03-21): Merged to MANIFEST. Pruned redundancy.
