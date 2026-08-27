# BipSale — Sistema de Vendas por QR Code

BipSale é um aplicativo Android modularizado projetado para facilitar vendas rápidas através da leitura de preços por QR Code. Oferece suporte completo a operações offline com sincronização automática e exportação de dados em Excel.

## ✨ Funcionalidades

- **Leitura de QR Code** — Scan rápido de preços via câmera com CameraX
- **Adição Manual de Produtos** — Interface simplificada para cadastro rápido
- **Imagem do Produto** — Upload de foto do produto via galeria, exibida na lista, no cadastro, no carrinho e na seleção de produtos; toque para ampliar
- **Desconto por Item** — Desconto percentual ou fixo por linha do carrinho, com preço original riscado
- **Controle de Estoque** — Quantidade por produto, decrementada a cada venda na mesma transação
- **Configurações** — Chave PIX, nome e cidade do recebedor, e desconto padrão do PIX (percentual ou valor)
- **Pagamento via PIX** — QR Code gerado com o valor da venda já embutido (padrão BR Code do Banco Central). O código fica na tela até o operador confirmar o pagamento, pode ser ampliado com um toque, e pode ser reexibido depois pelo histórico. Se a configuração estiver incompleta, um card no lugar do QR diz **quais campos** faltam
- **Cidade do PIX** — Somente o nome da cidade, no máximo 15 caracteres (`Campinas`, não `Campinas/SP`)
- **Tela de cliente opcional** — Desligada, a venda abre direto no carrinho e é registrada como anônima
- **Importação em Massa** — Baixe um modelo .xlsx, preencha e importe o catálogo; linhas inválidas são reportadas pelo número da linha e as demais são importadas. As imagens são anexadas por produto depois da importação
- **Desconto PIX Configurável** — Aplicar descontos dinâmicos por operação de venda
- **Histórico de Transações** — Rastreabilidade completa com busca e filtros
- **Exportação para Excel** — Relatórios em `.xlsx` via Apache POI com linha de totais e aba de resumo por forma de pagamento, salvos onde o usuário escolher (Google Drive, Downloads, etc.)
- **Backup e Restauração** — Salvar produtos, vendas e imagens em um único arquivo .zip no Google Drive ou no aparelho, compartilhar, e restaurar com confirmação que mostra o conteúdo do arquivo antes de substituir os dados
- **Funcionamento 100% Offline** — Operação completa sem conexão, com sincronização posterior
- **Geração de QR Code** — Criar e imprimir QR codes com nome do produto (13 pt, quantas linhas couberem) e preço (17 pt), uma etiqueta por unidade em estoque
- **Impressão em Lote** — Imprimir etiquetas QR de todos os produtos (ou apenas os selecionados) em grade otimizada: 20 etiquetas por folha A4 (4x5) com margem tracejada para recorte, adaptando-se a qualquer outro tamanho de papel, com pré-visualização página a página e opção de salvar como PDF

## 🛠️ Tech Stack

- **Kotlin** 2.3.20 — Linguagem moderna e segura para Android
- **Jetpack Compose** (2026.03.00) — UI declarativa com Material 3
- **Kotlin Coroutines** 1.10.2 — Operações assíncronas estruturadas
- **Hilt** 2.59.2 — Injeção de dependência
- **Room** 2.8.4 — Persistência local com SQLite
- **Navigation Compose 3** 1.0.1 — Navegação type-safe
- **CameraX** 1.5.3 — Acesso à câmera e leitura de QR code
- **Apache POI** 5.5.1 — Exportação para Excel (.xlsx)
- **ZXing** 3.5.4 — Geração e leitura de QR code
- **Retrofit 3.0.0** — Cliente HTTP (quando necessário)
- **Coil** 2.7.0 — Carregamento de imagens
- **Timber** 5.0.1 — Logging estruturado

**Ferramentas de Build:**
- Gradle 9.1.0
- Java 21 toolchain
- KSP 2.3.3 para processamento de anotações
- Android Gradle Plugin 9.1.0

## 🏗️ Arquitetura

BipSale utiliza **Clean Architecture** com padrão **MVVM** em estrutura modularizada por feature:

**Estrutura de Módulos:**

```
:app                          — Ponto de entrada e dashboard
├─ :feature:sales             — Fluxo completo de vendas
├─ :feature:products          — Gerenciamento de produtos e QR
├─ :feature:history           — Histórico de transações
└─ :core
   ├─ :core:data              — Persistência com Room
   ├─ :core:navigation        — Navegação centralizada (Nav3)
   ├─ :core:ui                — Temas e componentes compartilhados
   ├─ :core:domain            — Modelos de negócio
   └─ :core:di                — Configuração de injeção de dependência
```

**Padrão de Estado (MVI/MVVM):**
- `XxxUIState` — Estado imutável
- `XxxIntent` — Ações do usuário (sealed class)
- `XxxEffect` — Efeitos colaterais únicos (via Channel)
- `XxxViewModel : ViewModel` — Implementação com Hilt

## 🚀 Começando

### Pré-requisitos

- **Android Studio** Ladybug ou superior
- **Java 21** (obrigatório para toolchain)
- **Gradle 9.1.0** (incluído via wrapper)
- **Android SDK** API 24+ (mínimo) / API 34+ (recomendado)

### Instruções de Build

1. **Clone e abra o projeto:**
   ```bash
   git clone <repository-url>
   cd BipSale
   ```

2. **Sincronize o Gradle:**
   Abra o projeto no Android Studio e deixe o Gradle sincronizar automaticamente.

3. **Build Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   Saída: `app/build/outputs/apk/debug/app-debug.apk`

4. **Build Release APK:**
   ```bash
   ./gradlew assembleRelease
   ```
   _Nota: Requer configuração de assinatura em `local.properties` ou argumentos de build._

### Executar em Dispositivo/Emulador

- **Via Android Studio:** Run > Run 'app'
- **Via Gradle:**
  ```bash
  ./gradlew installDebug
  ```

## 🧪 Testes

### Executar Todos os Testes
```bash
./gradlew test
```

### Testes por Módulo
```bash
./gradlew :feature:sales:test
./gradlew :core:data:test
```

### Teste Específico
```bash
./gradlew :feature:sales:test --tests "...SalesViewModelTest"
```

### Qualidade de Código

**Análise Estática (Detekt):**
```bash
./gradlew detekt
```

**Lint Analysis:**
```bash
./gradlew lint
```

**Code Coverage (Kover):**
```bash
./gradlew koverHtmlReport
```
Relatório: `build/reports/kover/html/index.html`

## 📚 Fluxos de Negócio

### Fluxo de Venda
1. Usuário inicia venda na tela de vendas
2. Escaneia QR code do produto (CameraX) ou adiciona manualmente
3. Sistema busca preço e dados do produto em cache local
4. Usuário pode adicionar quantidade, desconto PIX configurável
5. Venda é persistida no banco local (Room)
6. Comprovante pode ser gerado/impresso

### Gerenciamento de Produtos
1. Cadastro manual ou importação via sincronização
2. Cada produto tem QR code único (gerado via ZXing)
3. Metadados: preço, descrição, categoria
4. Suporte a múltiplas variações por produto

### Exportação de Dados
1. Selecione vendas no histórico ou exporte todas pela tela de exportação
2. O sistema abre o seletor de documentos do Android para escolher o destino
3. Geração de relatório em Excel (Apache POI) com uma linha por item vendido e as colunas: Data, ID Venda, Cliente, CPF, Código Produto, Produto, Qtd, Valor Unit., Desconto Item, Total Item, Desconto Venda (%), Total Venda
4. As colunas de venda (Desconto Venda e Total Venda) aparecem apenas na primeira linha de cada venda, para que somar a coluna não conte a mesma venda duas vezes
5. Linha **TOTAIS** ao final da planilha, somando apenas Qtd, Desconto Item, Total Item e Total Venda — somar preço unitário ou percentual de desconto produziria um número sem significado
6. Aba **Resumo**: período, número de vendas, itens vendidos, ticket médio, subtotal bruto, descontos por item, descontos por venda, total de descontos, receita líquida e o total por forma de pagamento
7. Feedback via Snackbar: sucesso (com contagem), falha, dados vazios ou cancelamento

## 🛠️ Desenvolvimento

### Estrutura de Código

**Domain Layer (Puro Kotlin):**
- Modelos de negócio (entidades, value objects)
- Use cases (lógica de negócio)
- Interfaces de repositório
- **Zero dependências Android**

**Data Layer:**
- Room entities e DAOs
- Implementação de repositórios
- Mapeamento de dados (Domain ↔ Data)

**UI Layer (Compose):**
- Screens por feature
- ViewModels com MVI/MVVM
- Componentes reutilizáveis em `:core:ui`

### Padrões Obrigatórios

- **Strings:** Sempre em `res/strings.xml` — suporte a PT, EN, ES, DE
- **Cores/Temas:** Usar `BipSaleTheme` tokens — nunca hardcoded
- **Logging:** `Timber.d()`, `Timber.e()` com tags descritivas
- **IO:** `DocumentFile` + `runCatching` para storage externo
- **Use Cases:** Interface + implementação separadas
- **Navegação:** Nav3 com `@Serializable` em `core/navigation/Screen.kt`

### Padrões de Teste

- **Unit Tests:** Lógica pura, mocks de repositórios
- **Integration Tests:** Room database com `RoomDatabase.Builder(inMemoryDatabaseBuilder)`
- **UI Tests:** Compose Test API para interações de UI

## 📋 Dependências Principais

| Dependência | Versão | Propósito |
|------------|--------|----------|
| Jetpack Compose | 2026.03.00 | UI |
| Hilt | 2.59.2 | DI |
| Room | 2.8.4 | Banco Local |
| CameraX | 1.5.3 | Scanner QR |
| Apache POI | 5.5.1 | Excel |
| ZXing | 3.5.4 | QR Code Gen |
| Retrofit | 3.0.0 | HTTP Client |

Para versões detalhadas, ver `gradle/libs.versions.toml`.

## 🔧 Troubleshooting

**Câmera não funciona:**
- Verifique permissões em `AndroidManifest.xml`
- `android.permission.CAMERA` é obrigatório
- Em runtime, solicite permissão com PermissionLauncher

**QR Code não é lido:**
- Certifique-se que o código não está danificado
- Teste com app de câmera nativa primeiro
- Aumente iluminação ambiente

**Exportação Excel falha:**
- O app abre o seletor de documentos do sistema — escolha um local acessível (Downloads, Drive)
- Garanta espaço livre no dispositivo
- Se aparecer "Nenhuma venda para exportar", registre vendas primeiro
- Logs via Timber (`[BipSale][Export]`) para diagnosticar

**Sync offline não funciona:**
- Verifique conectividade (Airplane Mode desligado)
- Limpe cache do app: Configurações > Apps > BipSale > Armazenamento > Limpar Cache
- Reinicie o aplicativo

## 📱 Requisitos de Permissões

```xml
<!-- Obrigatórias -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />

<!-- Storage (Android 12+) -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

<!-- Hardware -->
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

## 📦 Distribuição

### Firebase App Distribution (Debug)
```bash
powershell -File ./appDistributionUploadDebug.ps1
```

### Firebase App Distribution (Release)
```bash
powershell -File ./appDistributionUploadRelease.ps1
```

## ⚙️ Configuração de Build

Todos os níveis de SDK, versões e o application id ficam em **um único objeto Kotlin**, não nos
`build.gradle.kts` dos módulos: `build-logic/src/main/java/dev/diegoflassa/buildLogic/Configuracoes.kt`.
Altere lá, não no módulo.

| Configuração | Valor |
|---|---|
| `APPLICATION_ID` | `dev.diegoflassa.bipsale` |
| `MINIMUM_SDK` | 29 (Android 10) |
| `COMPILE_SDK` / `TARGET_SDK` | 37 |
| Java / JVM target | 21 |
| Kotlin | 2.4.10 |
| Room | 2.8.4 |
| Compose BOM | 2026.08.00 |

**O `VERSION_CODE` é incrementado automaticamente a cada build** e gravado em um arquivo de
properties — não é uma constante para editar à mão, e um build vai alterá-la sem aviso. Os convention
plugins em `build-logic/` (`android-application-convention`, `android-library-convention`,
`detekt-convention`) aplicam isso a todos os módulos, e é por isso que os arquivos de build individuais
são quase vazios.

## 📚 Documentação (`conductor/`)

A documentação viva do projeto está em `conductor/`, e ela é **index-first por design** — não leia tudo.

1. [`AGENTS.md`](AGENTS.md) — identidade do projeto, stack, restrições inegociáveis
2. [`conductor/index.md`](conductor/index.md) — o mapa da documentação
3. [`conductor/rules/ai_behavior.md`](conductor/rules/ai_behavior.md) +
   [`CORE_RULES.md`](conductor/rules/CORE_RULES.md) — carregue os dois para qualquer mudança não trivial
4. [`conductor/rules/architecture.md`](conductor/rules/architecture.md) — só quando a tarefa mexe em estrutura
5. [`conductor/knowledge/INDEX.md`](conductor/knowledge/INDEX.md) — case a tarefa e carregue **apenas** os
   KIs indicados

### Knowledge Items

| KI | Assunto |
|---|---|
| [KI-03](conductor/knowledge/KI-03-TOKEN-AUDIT-AND-PRUNING.md) | Auditoria e poda de contexto |
| [KI-04](conductor/knowledge/KI-04-LOG-FILTERS.md) | Catálogo de **todos** os filtros de log — todo filtro novo entra aqui no mesmo turno |
| [KI-05](conductor/knowledge/KI-05-PRODUCT-IMAGES-AND-QR-LABELS.md) | Imagens de produto e etiquetas QR |
| [KI-06](conductor/knowledge/KI-06-BACKUP-AND-RESTORE.md) | Backup e restauração |
| [KI-07](conductor/knowledge/KI-07-SALES-CART-AND-DISCOUNTS.md) | Carrinho de vendas e descontos |
| [KI-08](conductor/knowledge/KI-08-SALES-EXPORT-REPORT.md) | Exportação e relatório de vendas |
| [KI-09](conductor/knowledge/KI-09-STOCK-SETTINGS-AND-PIX.md) | Estoque, configurações e PIX |

Também em `conductor/knowledge/`: [`KI-TBD.md`](conductor/knowledge/KI-TBD.md) (tudo que está adiado ou
não construído), [`TEST_COVERAGE.md`](conductor/knowledge/TEST_COVERAGE.md) (inventário por módulo e
lacunas conhecidas) e [`KI-AUTHORING.md`](conductor/knowledge/KI-AUTHORING.md) (o template — um KI é uma
especificação em tempo presente, nunca um changelog).

Planejamentos ficam em `conductor/plannings/`; os concluídos vão para `archived/` e **nunca** são
apagados.

## 🧭 Família de Projetos

BipSale, **Slotify** e **Comiqueta** são mantidos pelo mesmo desenvolvedor e compartilham **um único
conjunto de regras de workflow**. O [`conductor/rules/CORE_RULES.md`](conductor/rules/CORE_RULES.md) §14
é obrigatório: quando uma regra **compartilhada** muda em um repositório, a mesma mudança entra nos
outros dois no mesmo turno. As três árvores são estruturalmente idênticas, então o mesmo caminho
relativo é o equivalente em cada uma.

| Projeto | O que é | Plataforma | Pacote |
|---|---|---|---|
| **Slotify** | Agendamento para salões/clínicas — agenda, clientes, pacotes, estoque | Kotlin Multiplatform (Android + iOS) | `br.com.slotify` |
| **Comiqueta** | Leitor de quadrinhos com suporte a múltiplos formatos de arquivo | Android | `dev.diegoflassa.comiqueta` |
| **BipSale** | PDV por QR Code, offline-first com exportação em Excel | Android | `dev.diegoflassa.bipsale` |

**Compartilhado** — muda em um, muda nos três: estabilidade, segurança no git, economia de tokens,
estilo de código, disciplina de sync de KI, protocolo de planejamento, formato dos filtros de log,
extração de composables, propriedade das strings, a regra de teste de regressão, a regra do changelog, e
tudo em `ai_behavior.md` e `GRADLE_RULES.md`.

**Não compartilhado** — adapte ou omita, nunca copie: grafo de módulos, framework de DI, API de log,
persistência, build types, conjuntos de locale, e tudo em `architecture.md`. O Slotify é Kotlin
Multiplatform com Koin; Comiqueta e BipSale são Android puro com Hilt.

## 📄 Licença

> ⚠️ **Divergência a resolver.** Esta seção dizia **MIT**, mas o arquivo [`LICENSE`](LICENSE) na raiz do
> repositório é a **Apache License 2.0**. As duas não são intercambiáveis — a Apache 2.0 exige aviso de
> alterações e concede patentes explicitamente, a MIT não faz nenhum dos dois. **O arquivo `LICENSE` é o
> que vale juridicamente**, então o texto abaixo reflete o arquivo. Se a intenção era MIT, troque o
> arquivo; se era Apache 2.0, esta linha já está correta.

Este projeto está sob a **Apache License 2.0** — veja [`LICENSE`](LICENSE).

## 👨‍💻 Contribuindo

Siga os padrões de código acima e ensure que todos os testes passam (`./gradlew test`) antes de submeter mudanças.

## 📞 Suporte

Para questões ou relatório de bugs, abra uma issue no repositório.
