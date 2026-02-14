# Architecture: BipSale

Este projeto segue os princípios de **Clean Architecture** e o padrão real-time **MVI (Model-View-Intent)**.

## 🏛️ Camadas do Sistema
1.  **UI Layer (External)**: Composta pelas Screens e ViewModels. O estado é imutável e exposto via `StateFlow`.
2.  **Domain Layer (Pure Kotlin)**: Contém as regras de negócio puras, interfaces de repositório e modelos de domínio. Não depende de frameworks Android.
3.  **Data Layer (Infrastructure)**: Implementação dos repositórios, persistência com **Room** e comunicação com API via **Retrofit**.

## 🔄 Fluxo de Dados (SSOT)
- O projeto utiliza uma **Fonte Única de Verdade (SSOT)** baseada no banco de dados local.
- O fluxo de dados é unidirecional: UI -> ViewModel -> UseCase -> Repository -> Local DB -> Flow back to UI.

## 💉 Dependency Injection
- Utiliza **Hilt** para injeção de dependência.
- Scopes: `SingletonComponent` para infra e `ViewModelComponent` para lógica de tela.
