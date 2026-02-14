# Filesystem Structure

O BipSale é um projeto multi-módulo organizado para escalabilidade e isolamento de contexto.

## 📂 Diretórios Principais

### 🏢 App Module
- `:app`: O orquestrador central. Contém a `MainActivity` e a configuração global do Hilt.

### 🧩 Feature Modules
- `:feature:sales`: Fluxo de vendas e carrinho.
- `:feature:products`: Catálogo e gerenciamento de inventário.
- `:feature:history`: Extrato de vendas realizadas.
- `:feature:qrcode`: Escaneamento e geração de códigos.

### 🛠️ Core Modules (Shared)
- `:core:domain`: Modelos e interfaces agnósticas.
- `:core:data`: Implementação de I/O (Database, API, Prefs).
- `:core:ui`: Temas, Design System (M3) e componentes reutilizáveis.
- `:core:navigation`: Lógica de rotas e navegação centralizada.
- `:core:utils`: Extensões e helpers genéricos.

### 🏗️ Build Logic
- `build-logic/`: Convention plugins para manter a consistência de build entre módulos.
