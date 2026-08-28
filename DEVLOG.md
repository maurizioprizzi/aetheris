# 🛰️ Aetheris - Diário de Desenvolvimento (DEVLOG)

Registro contínuo da engenharia, decisões arquiteturais (ADRs), modelagem matemática e evolução do ecossistema Aetheris.

---

## 🔬 [Dia 07] - 2026-08-28: Pipeline Gráfico OpenGL ES 3.0, Shaders OES e Renderização de Linhas 3D

### 🎯 Objetivos Concluídos
- [x] Criação do `BackgroundRenderer` com shaders GLSL ES 3.0 e suporte a `GL_TEXTURE_EXTERNAL_OES` para projeção com *zero-copy* do feed de vídeo da câmera.
- [x] Implementação do `SpatialLineRenderer` em OpenGL ES 3.0 para traçado dos nós de ancoragem (`GL_POINTS`) e do vetor de medição (`GL_LINES`) no espaço tridimensional.
- [x] Multiplicação matricial Model-View-Projection ($M_{clip} = M_{proj} \times M_{view} \times M_{model}$) em tempo real alimentada pelas matrizes da câmera ARCore.
- [x] Prealocação estática de matrizes e buffers nativos diretos (`FloatBuffer`) garantindo zero alocação de memória no loop de renderização (Zero GC Churn).
- [x] Integração completa dos renderizadores no ciclo do `GLSurfaceView` (`onSurfaceCreated`, `onSurfaceChanged`, `onDrawFrame`) em `ArCameraFeed`.
- [x] Conexão dos pontos A e B do `uiState` à camada gráfica via `rememberUpdatedState`.
- [x] Criação da suíte `SpatialLineMathTest` e atualização de `MeasurementViewModelTest` com 100% dos testes unitários passando na JVM.
- [x] Registro da decisão arquitetural no `ADR-012`.

### 📐 Decisões de Arquitetura (ADR)
- **ADR-012: Zero-Copy OES Camera Texture and OpenGL ES 3.0 Spatial Geometry Pipeline**
  - **Contexto:** Necessidade de renderização em alta frequência (60 FPS) do vídeo da câmera e da geometria métrica sem alocações dinâmicas na GPU/CPU.
  - **Decisão:** Adoção de textura externa OES via GLSL ES 3.0, prealocação estática de buffers/matrizes e consumo síncrono do estado do Compose pela thread gráfica EGL.

---

## 🔬 [Dia 06] - 2026-08-27: Spatial Raycasting, Polygon Gating e Testes Unitários de Colisão

### 🎯 Objetivos Concluídos
- [x] Criação do processador de baixo nível `ArCoreHitTestProcessor` para projeção de raios ópticos a partir de coordenadas normalizadas de tela $[0.0, 1.0]$.
- [x] Implementação de filtragem estrita por polígono convexo (`isPoseInPolygon`) para eliminar extrapolações de planos infinitos e falsos positivos no vácuo.
- [x] Estabelecimento de fallback determinístico para pontos ToF / Depth API (`Point`) com rastreamento ativo.
- [x] Refatoração do `SpatialSensorRepositoryImpl`, eliminando a busca heurística 2D em favor do raycasting nativo do ARCore.
- [x] Mapeamento bidirecional de viewport entre `GLSurfaceView` (`onSurfaceChanged`), `ArCameraFeed`, repositório e `MeasurementViewModel`.
- [x] Suíte completa de testes unitários na JVM (`ArCoreHitTestProcessorTest`) cobrindo 6 cenários de colisão, planos fora de limites, clamping de tela e descarte de poses instáveis com MockK e Google Truth.
- [x] Registro da decisão técnica formal no `ADR-011`.
- [x] Validação integral da suíte de testes unitários (`./gradlew testDebugUnitTest`) executada em 4s com cache.

### 📐 Decisões de Arquitetura (ADR)
- **ADR-011: Spatial Raycasting and Convex Polygon Gating**
  - **Contexto:** A busca heurística 2D anterior gerava imprecisão métrica cumulativa e não garantia que os pontos ancorados pertencessem a superfícies físicas coplanares ou estáveis.
  - **Decisão:** Adoção do `Frame.hitTest` nativo com priorização de `Plane` dentro do polígono de suporte (`isPoseInPolygon`), fallback para pontos de profundidade ToF e conversão direta da `Pose` do ARCore para a entidade imutável de domínio `Point3D(x, y, z)` sem contaminar a camada `domain` com o SDK Android.

---

## 🔬 [Dia 05] - 2026-08-26: Hardware Óptico, Ciclo de Vida ARCore e Testes Unitários de Apresentação

### 🎯 Objetivos Concluídos
- [x] Implementação do gerenciador declarativo de permissões em tempo de execução `CameraPermissionHandler` no Jetpack Compose.
- [x] Criação do `ArCoreSessionManager` para controle do ciclo de vida da sessão AR, ativação do sensor de profundidade (`DepthMode.AUTOMATIC`) e liberação de recursos de memória.
- [x] Construção do componente visual `ArCameraFeed` conectando `GLSurfaceView` (OpenGL ES 3.0) ao ciclo de vida do Compose via `DisposableEffect` e `LifecycleEventObserver`.
- [x] Integração do feed da câmera na `MeasurementScreen` como camada base (`z-index: 0`) sob o HUD tático.
- [x] Tratamento de telemetria de rastreamento (`TrackingState`) no `MeasurementViewModel` sem quebrar o desacoplamento de camadas da Clean Architecture.
- [x] Padronização da biblioteca `kotlinx-coroutines-test` no catálogo de dependências (`gradle/libs.versions.toml` e `app/build.gradle.kts`).
- [x] Criação da suíte de testes unitários `MeasurementViewModelTest` com dublê de repositório (`FakeSpatialSensorRepository`), cobrindo fluxo de ancoragem de Pontos A/B, cálculo determinístico de distância, reset de medição e emissão de telemetria reativa.
- [x] Validação integral da suíte de testes unitários e compilação do APK de Debug (`./gradlew testDebugUnitTest assembleDebug`).

### 📐 Decisões de Arquitetura (ADR)
- **ADR-009: Gerenciamento Declarativo de Permissões Ópticas no Compose**
  - **Contexto:** O ARCore exige permissão de câmera em tempo de execução. O fluxo tradicional baseado em callbacks imperativos de `Activity` acopla a camada de apresentação ao framework e dificulta a modularização.
  - **Decisão:** Criação do componente `CameraPermissionHandler` utilizando `rememberLauncherForActivityResult`, garantindo tela de bloqueio e solicitação reativa sob demanda diretamente na árvore do Compose.
- **ADR-010: Isolamento de Ciclo de Vida do ARCore e Contexto EGL**
  - **Contexto:** A `GLSurfaceView` e a sessão ARCore exigem sincronização estrita com o ciclo de vida do Android (`ON_RESUME`, `ON_PAUSE`, `ON_DESTROY`) para evitar vazamentos de memória e corrupção do contexto gráfico.
  - **Decisão:** Encapsulamento da inicialização e destruição no `ArCoreSessionManager`, acoplado ao ciclo de vida da tela via `DisposableEffect` dentro de `ArCameraFeed`.

---

## 🔬 [Dia 04] - 2026-08-25: Processamento de Buffers AR e Interface HUD em Jetpack Compose

### 🎯 Objetivos Concluídos
- [x] Criação do extrator de baixo nível `ArCoreFrameProcessor` com filtro de confiança para conversão de `FloatBuffer` em `List<Point3D>`.
- [x] Modelagem do estado de interface `MeasurementUiState` e implementação do `MeasurementViewModel` com Unidirectional Data Flow (UDF) sobre `StateFlow`.
- [x] Construção da tela de metrologia espacial `MeasurementScreen` em Jetpack Compose com design estilo HUD científico:
  - Indicadores de telemetria (`TRACKING`, `TOF / DEPTH ON`, contagem de pontos da nuvem).
  - Retículo dinâmico de mira central com feedback cromático de superfície.
  - Painel de leitura de distância com exibição de incerteza métrica ($\pm\sigma$).
- [x] Registro do `ArCoreFrameProcessor` e do `MeasurementViewModel` no módulo Koin (`AppModule.kt`).
- [x] Integração da `MeasurementScreen` na `MainActivity`.
- [x] Validação completa de testes unitários na JVM e compilação bem-sucedida do APK de Debug (`./gradlew assembleDebug`).

### 📐 Decisões de Arquitetura (ADR)
- **ADR-007: Filtragem e Descarte de Ruído em Buffers Brutos (PointCloud)**
  - **Contexto:** Sensores ópticos e de tempo de voo (ToF) geram dispersão de dados e pontos espúrios em superfícies reflexivas ou de baixa iluminação.
  - **Decisão:** O `ArCoreFrameProcessor` aplica um limiar de confiança configurável ($\ge 30\%$) diretamente na leitura do `FloatBuffer`, descartando artefatos antes de criar instâncias imutáveis de `Point3D` no domínio.
- **ADR-008: Unidirectional Data Flow (UDF) com StateFlow no HUD de Metrologia**
  - **Contexto:** A interface gráfica precisa renderizar dados de alta frequência da câmera ao mesmo tempo em que reage às interações pontuais do usuário (ancoragem do Ponto A e Ponto B).
  - **Decisão:** Centralização de todo o estado em `MeasurementUiState` imutável, exposto via `StateFlow` pelo `MeasurementViewModel`, garantindo que a UI apenas observe e emita eventos de clique sem conter lógica de negócio.

---

## 🔬 [Dia 03] - 2026-08-24: Contrato de Repositório de Sensores e Telemetria Reativa

### 🎯 Objetivos Concluídos
- [x] Criação dos modelos de telemetria espacial (`TrackingStatus`, `SpatialFrameData`).
- [x] Definição do contrato de repositório `SpatialSensorRepository` na camada `domain`.
- [x] Implementação de `SpatialSensorRepositoryImpl` com `StateFlow` na camada `data`.
- [x] Injeção de dependência via Koin como `Single`.
- [x] Testes unitários do repositório garantindo reatividade e integridade de estado.
- [x] Configuração da pipeline de integração contínua (CI) com GitHub Actions (`.github/workflows/android.yml`).

### 📐 Decisões de Arquitetura (ADR)
- **ADR-005: Desacoplamento do Pipeline de Sensores via Repositório Reativo**
  - **Contexto:** O hardware emite frames espaciais em 30 a 60 FPS. A camada de domínio não deve ser bloqueada pela taxa de quadros do sensor.
  - **Decisão:** Uso de `StateFlow<SpatialFrameData>` com atualizações atômicas (`.update { ... }`).
- **ADR-006: Abstração de Hit-Testing e Raycasting**
  - **Contexto:** Projeção de coordenadas 2D de tela para coordenadas físicas 3D ($X, Y, Z$).
  - **Decisão:** O repositório expõe o método `performHitTest(x, y)` delegando a interseção geométrica para a nuvem de pontos ou mapa de profundidade denso.

---

## 🔬 [Dia 02] - 2026-08-23: Domínio Matemático Puro e Modelagem Física

### 🎯 Objetivos Concluídos
- [x] Criação das entidades imutáveis: `Point3D`, `BoundingBox3D`, `DistanceMeasurement`, `MassEstimate`.
- [x] Implementação dos casos de uso: `CalculateDistanceUseCase` e `EstimateSpatialDimensionsUseCase`.
- [x] Implementação da propagação dinâmica de incerteza metrológica ($\pm\sigma$).
- [x] Cobertura de 100% em testes unitários com JUnit 4 e Google Truth na JVM.

### 📐 Decisões de Arquitetura (ADR)
- **ADR-003: Isolamento do Domínio Matemático em Kotlin Puro**
  - **Decisão:** Zero dependências do Android SDK na camada `domain` para garantir portabilidade e execução instantânea de testes unitários.
- **ADR-004: Incerteza Dinâmica como Entidade de Primeira Classe**
  - **Decisão:** Toda medição física carrega seu desvio padrão de erro intrínseco baseado no modelo físico do sensor.

---

## 🏛️ [Dia 01] - 2026-08-22: Fundação, Setup e Governança

### 🎯 Objetivos Concluídos
- [x] Configuração do projeto com Kotlin 2.x, Jetpack Compose (Material 3), Gradle Kotlin DSL e Version Catalogs (`libs.versions.toml`).
- [x] Estruturação da Clean Architecture (`domain`, `data`, `presentation`).
- [x] Injeção de dependência com Koin.
- [x] Publicação do repositório no GitHub com licença Apache 2.0.

### 📐 Decisões de Arquitetura (ADR)
- **ADR-001: Adoção do Koin em vez de Hilt/Dagger**
  - **Decisão:** Injeção de dependência 100% Kotlin puro sem geração pesada de código ou problemas com novas versões do compilador K2.
- **ADR-002: Licenciamento Apache 2.0 e Estratégia Open Core**
  - **Decisão:** Núcleo aberto para autoridade técnica e portfólio público, com suporte a extensões proprietárias via contratos de plugin.

---

## 🚀 Próximos Passos (Dia 08)
- [ ] Implementação de projeção World-to-Screen para cálculo do ponto médio 2D do vetor tridimensional.
- [ ] Renderização de etiqueta/badge flutuante em Compose (distância + incerteza $\pm\sigma$) acompanhando a linha 3D em tempo real.
- [ ] Persistência de âncoras nativas do ARCore (`Anchor`) para mitigar *drift* de odometria visual-inercial em medições de longa duração.