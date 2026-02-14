# Business Workflows

Este documento descreve os fluxos operacionais básicos do aplicativo.

## 🛒 Fluxo de Venda (Checkout)
1.  **Seleção**: O usuário escolhe produtos através da `feature:products`.
2.  **Carrinho**: Os itens são processados na `feature:sales`.
3.  **Identificação**: Entrada opcional de CPF do cliente (preparado para Fiscal).
4.  **Persistência**: Ao finalizar, a venda é salva via `SaleRepositoryImpl` no Room.
5.  **Exibição**: O status da venda é refletido instantaneamente no `history`.

## 🔄 Fluxo de Dados de Produto
- Os produtos são carregados do banco local e expostos como `Flow` para garantir reatividade total na UI quando o inventário mudar.
