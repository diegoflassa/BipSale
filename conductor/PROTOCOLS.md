# Protocols: BipSale Development

## 🏛️ Arquitetura (MVI + Clean)
1. **UI Layer**: Compose + Scaffolding + Observação de UI State.
2. **ViewModel**: Gerenciamento de estado, execução de UseCases, hoisting.
3. **Domain Layer**: Entidades puras e interfaces de Repositório.
4. **Data Layer**: Implementação de Repositories, DAO, Network clients, Mappers.

## 🌿 Git & Branching
- **Commits**: [Conventional Commits](https://www.conventionalcommits.org/).
- **Review**: O Agente NUNCA faz commit sem autorização explícita.

## 🛠️ Padrões de Código
- **Imutabilidade**: Sempre `val` e `data class`.
- **Concurrency**: `suspend` functions devem ser `main-safe`.
- **Result Type**: I/O deve retornar `Result<T>` ou Sealed Classes de erro.
