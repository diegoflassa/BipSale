# BipSale - Sistema de Vendas por QR Code

BipSale é um aplicativo Android modularizado projetado para facilitar vendas rápidas através da leitura de preços por QR Code.

## 🚀 Começando

Para compilar o projeto, abra no Android Studio (Ladybug ou superior) e execute a sincronização do Gradle.

## 🏗️ Arquitetura

O projeto utiliza **Clean Architecture** com **MVVM** e uma estrutura modularizada por feature:

- `:app`: Ponto de entrada e dashboard.
- `:feature:sales`: Fluxo completo de vendas.
- `:feature:products`: Gerenciamento de produtos e geração de QR.
- `:feature:history`: Histórico de transações e busca.
- `:feature:qrcode`: Scanner via CameraX e geração via ZXing.
- `:core:data`: Persistência local com Room.
- `:core:navigation`: Navegação centralizada (Navigation Compose 3).
- `:core:ui`: Temas e componentes compartilhados.

## 📱 Features

- Scan de QR Code para adição rápida de produtos.
- Adição manual de produtos.
- Desconto PIX configurável por venda.
- Exportação de histórico para Excel (.xlsx).
- Funcionamento 100% offline.

## 🛠️ Tecnologias

- **Kotlin** 2.3.0
- **Jetpack Compose** (Material 3)
- **Hilt** para DI
- **Room** para persistência
- **CameraX** para leitura de QR
- **Apache POI** para exportação Excel
- **Navigation Compose 3.x**

## 📄 Licença

Este projeto está sob a licença MIT.
