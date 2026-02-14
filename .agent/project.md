# BipSale Project Rules

## 🧠 Princípios de Engenharia
- **DDD**: `core:domain` é o centro da aplicação.
- **Dependency Inversion**: Camadas externas dependem de interfaces do domínio.
- **Fail-Safe**: `Result` e `runCatching` para evitar crashes.

## 🏗️ Módulos
| Módulo | Responsabilidade |
|--------|------------------|
| `:app` | Entry point, Hilt App |
| `:feature:sales` | POS/checkout |
| `:feature:history` | Logs/transações |
| `:feature:products` | Catálogo/inventário |
| `:feature:qrcode` | Scanner/QR |
| `:core:domain` | Pure Kotlin, UseCases, Entities |
| `:core:data` | Repos, Room, Retrofit, Mappers |
| `:core:ui` | Design System, M3 Components |
| `:core:navigation` | Type-safe routes |

## 📏 Constraints
- **Domain Purity**: `core:domain` = Pure Kotlin (NO Android deps).
- **Clean Arch**: UI → VM → Domain → Data (SSOT).
- **Features Isolation**: Features NÃO dependem entre si.
- **Logging**: `Timber` only.

## 🛠️ Tech Stack
- Kotlin 2.3.0 | Compose BOM 2026.01 | Room 2.8.4 | Hilt (KSP)
