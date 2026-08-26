# 📐 Aetheris - Diário de Desenvolvimento (DEVLOG)

Registro contínuo da engenharia, decisões arquiteturais (ADRs), modelagem matemática e evolução do ecossistema Aetheris.

---

## 🚀 [Dia 05] - 2026-08-26: Hardware Óptico, Ciclo de Vida ARCore e Testes Unitários de Apresentação

### ✨ Objetivos Concluídos
- [x] Implementação do gerenciador declarativo de permissões em tempo de execução `CameraPermissionHandler` no Jetpack Compose.
- [x] Criação do `ArCoreSessionManager` para controle do ciclo de vida da sessão AR, ativação do sensor de profundidade (`DepthMode.AUTOMATIC`) e liberação de recursos de memória.
- [x] Construção do componente visual `ArCameraFeed` conectando `GLSurfaceView` (OpenGL ES 3.0) ao ciclo de vida do Compose via `DisposableEffect` e `LifecycleEventObserver`.
- [x] Integração do feed da câmera na `MeasurementScreen` como camada base (`z-index: 0`) sob o HUD tático.
- [x] Tratamento de telemetria de rastreamento (`TrackingState`) no `MeasurementViewModel` sem quebrar o desacoplamento de camadas da Clean Architecture.
- [x] Padronização da biblioteca `kotlinx-coroutines-test` no catálogo de dependências (`gradle/libs.versions.toml` e `app/build.gradle.kts`).
- [x] Criação da suíte de testes unitários `MeasurementViewModelTest` com dublê de repositório (`FakeSpatialSensorRepository`), cobrindo fluxo de ancoragem de Pontos A/B, cálculo determinístico de distância, reset de medição e emissão de telemetria reativa.
- [x] Validação integral da suíte de testes unitários e compilação do APK de Debug (`./gradlew testDebugUnitTest assembleDebug`).

### 🏗️ Decisões de Arquitetura (ADR)
- **ADR-009: Gerenciamento Declarativo de Permissões Ópticas no Compose**
  - **Contexto:** O ARCore exige permissão de câmera em tempo de execução. O fluxo tradicional baseado em callbacks imperativos de `Activity` acopla a camada de apresentação ao framework e dificulta a modularização.
  - **Decisão:** Criação do componente `CameraPermissionHandler` utilizando `rememberLauncherForActivityResult`, garantindo tela de bloqueio e solicitação reativa sob demanda diretamente na árvore do Compose.
- **ADR-010: Isolamento de Ciclo de Vida do ARCore e Contexto EGL**
  - **Contexto:** A `GLSurfaceView` e a sessão ARCore exigem sincronização estrita com o ciclo de vida do Android (`ON_RESUME`, `ON_PAUSE`, `ON_DESTROY`) para evitar vazamentos de memória e corrupção do contexto gráfico.
  - **Decisão:** Encapsulamento da inicialização e destruição no `ArCoreSessionManager`, acoplado ao ciclo de vida da tela via `DisposableEffect` dentro de `ArCameraFeed`.

---

## 🔬 [Dia 04] - 2026-08-25: Processamento de Buffers AR e Interface HUD em Jetpack Compose

### ✨ Objetivos Concluídos
- [x] Criação do extrator de baixo nível `ArCoreFrameProcessor` com filtro de confiança para conversão de `FloatBuffer` em `List<Point3D>`.
- [x] Modelagem do estado de interface `MeasurementUiState` e implementação do `MeasurementViewModel` com Unidirectional Data Flow (UDF) sobre `StateFlow`.
- [x] Construção da tela de metrologia espacial `MeasurementScreen` em Jetpack Compose com design estilo HUD científico:
  - Indicadores de telemetria (`TRACKING`, `TOF / DEPTH ON`, contagem de pontos da nuvem).
  - Retículo dinâmico de mira central com feedback cromático de superfície.
  - Painel de leitura de distância com exibição de incerteza métrica ($\pm\sigma$).
- [x] Registro do `ArCoreFrameProcessor` e do `MeasurementViewModel` no módulo Koin (`AppModule.kt`).
- [x] Integração da `MeasurementScreen` na `MainActivity`.
- [x] Validação completa de testes unitários na JVM e compilação bem-sucedida do APK de Debug (`./gradlew assembleDebug`).

### 🏗️ Decisões de Arquitetura (ADR)
- **ADR-007: Filtragem e Descarte de Ruído em Buffers Brutos (PointCloud)**
  - **Contexto:** Sensores ópticos e de tempo de voo (ToF) geram dispersão de dados e pontos espúrios em superfícies reflexivas ou de baixa iluminação.
  - **Decisão:** O `ArCoreFrameProcessor` aplica um limiar de confiança configurável ($\ge 30\%$) diretamente na leitura do `FloatBuffer`, descartando artefatos antes de criar instâncias imutáveis de `Point3D` no domínio.
- **ADR-008: Unidirectional Data Flow (UDF) com StateFlow no HUD de Metrologia**
  - **Contexto:** A interface gráfica precisa renderizar dados de alta frequência da câmera ao mesmo tempo em que reage às interações pontuais do usuário (ancoragem do Ponto A e Ponto B).
  - **Decisão:** Centralização de todo o estado em `MeasurementUiState` imutável, exposto via `StateFlow` pelo `MeasurementViewModel`, garantindo que a UI apenas observe e emita eventos de clique sem conter lógica de negócio.

---

## 📡 [Dia 03] - 2026-08-24: Contrato de Repositório de Sensores e Telemetria Reativa

### ✨ Objetivos Concluídos
- [x] Criação dos modelos de telemetria espacial (`TrackingStatus`, `SpatialFrameData`).
- [x] Definição do contrato de repositório `SpatialSensorRepository` na camada `domain`.
- [x] Implementação de `SpatialSensorRepositoryImpl` com `StateFlow` na camada `data`.
- [x] Injeção de dependência via Koin como `Single`.
- [x] Testes unitários do repositório garantindo reatividade e integridade de estado.
- [x] Configuração da pipeline de integração contínua (CI) com GitHub Actions (`.github/workflows/android.yml`).

### 🏗️ Decisões de Arquitetura (ADR)
- **ADR-005: Desacoplamento do Pipeline de Sensores via Repositório Reativo**
  - **Contexto:** O hardware emite frames espaciais em 30 a 60 FPS. A camada de domínio não deve ser bloqueada pela taxa de quadros do sensor.
  - **Decisão:** Uso de `StateFlow<SpatialFrameData>` com atualizações atômicas (`.update { ... }`).
- **ADR-006: Abstração de Hit-Testing e Raycasting**
  - **Contexto:** Projeção de coordenadas 2D de tela para coordenadas físicas 3D ($X, Y, Z$).
  - **Decisão:** O repositório expõe o método `performHitTest(x, y)` delegando a interseção geométrica para a nuvem de pontos ou mapa de profundidade denso.

---

## 🧮 [Dia 02] - 2026-08-23: Domínio Matemático Puro e Modelagem Física

### ✨ Objetivos Concluídos
- [x] Criação das entidades imutáveis: `Point3D`, `BoundingBox3D`, `DistanceMeasurement`, `MassEstimate`.
- [x] Implementação dos casos de uso: `CalculateDistanceUseCase` e `EstimateSpatialDimensionsUseCase`.
- [x] Implementação da propagação dinâmica de incerteza metrológica ($\pm\sigma$).
- [x] Cobertura de 100% em testes unitários com JUnit 4 e Google Truth na JVM.

### 🏗️ Decisões de Arquitetura (ADR)
- **ADR-003: Isolamento do Domínio Matemático em Kotlin Puro**
  - **Decisão:** Zero dependências do Android SDK na camada `domain` para garantir portabilidade e execução instantânea de testes unitários.
- **ADR-004: Incerteza Dinâmica como Entidade de Primeira Classe**
  - **Decisão:** Toda medição física carrega seu desvio padrão de erro intrínseco baseado no modelo físico do sensor.

---

## 🏛️ [Dia 01] - 2026-08-22: Fundação, Setup e Governança

### ✨ Objetivos Concluídos
- [x] Configuração do projeto com Kotlin 2.x, Jetpack Compose (Material 3), Gradle Kotlin DSL e Version Catalogs (`libs.versions.toml`).
- [x] Estruturação da Clean Architecture (`domain`, `data`, `presentation`).
- [x] Injeção de dependência com Koin.
- [x] Publicação do repositório no GitHub com licença Apache 2.0.

### 🏗️ Decisões de Arquitetura (ADR)
- **ADR-001: Adoção do Koin em vez de Hilt/Dagger**
  - **Decisão:** Injeção de dependência 100% Kotlin puro sem geração pesada de código ou problemas com novas versões do compilador K2.
- **ADR-002: Licenciamento Apache 2.0 e Estratégia Open Core**
  - **Decisão:** Núcleo aberto para autoridade técnica e portfólio público, com suporte a extensões proprietárias via contratos de plugin.

---

## 🎯 Próximos Passos (Dia 06)
- [ ] Implementação do Hit-Testing espacial real contra planos detectados pelo ARCore (`session.hitTest`).
- [ ] Ancoragem física e persistência de âncoras (`Anchor`) para os Pontos A e B no espaço 3D.
- [ ] Renderização da linha de vetor métrico e distância projetada diretamente sobre o plano detectado.