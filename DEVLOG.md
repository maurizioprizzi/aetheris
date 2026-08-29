# 📐 Aetheris - Diário de Desenvolvimento (DEVLOG)

Registro contínuo da engenharia, decisões arquiteturais (ADRs), modelagem matemática e evolução do ecossistema Aetheris.

---

## 🚀 [Dia 09] - 2026-08-29: Hardening do Pipeline ARCore, Segurança Numérica e Regressão Completa

### 🎯 Objetivos Concluídos
- [x] Migração definitiva do estado espacial legado para `SpatialFrameData`, com atualização do contrato `SpatialSensorRepository`, dos repositórios falsos e dos testes do `MeasurementViewModel`.
- [x] Correção da suíte de testes do ARCore, incluindo os mocks de `Frame.camera`, `TrackingState`, `Point.orientationMode`, `Plane.isPoseInPolygon` e criação de âncoras.
- [x] Adequação do ciclo de vida de `PointCloud` ao contrato `AutoCloseable`, garantindo `pointCloud.close()` por meio de `use`, inclusive quando a leitura do buffer falha.
- [x] Estabilização do `SpatialSensorRepositoryImpl` com viewport atualizada atomicamente, conversão segura de coordenadas normalizadas para pixels e liberação defensiva de âncoras.
- [x] Tratamento dos estados `TRACKING`, `PAUSED` e `STOPPED` das âncoras, preservando a última posição conhecida durante pausas temporárias e removendo âncoras encerradas.
- [x] Reforço do `ArCoreHitTestProcessor` para validar câmera, coordenadas, planos, pontos orientados e `DepthPoint`, retornando `null` diante de indisponibilidade transitória do pipeline nativo.
- [x] Reforço do `ArCoreFrameProcessor` com validação de confiança e coordenadas finitas, além de tratamento para `DeadlineExceededException`, `NotYetAvailableException` e `ResourceExhaustedException`.
- [x] Refatoração dos modelos `Point3D`, `BoundingBox3D`, `DistanceMeasurement`, `MassEstimate`, `ScreenPoint2D`, `SpatialFrameData` e `TrackingStatus`.
- [x] Adoção de cálculos intermediários em precisão `Double` para distância, normalização e ponto médio, reduzindo riscos de overflow e perda numérica.
- [x] Consolidação de `ProjectWorldToScreenUseCase`, `CalculateDistanceUseCase`, `EstimateSpatialDimensionsUseCase` e `SpatialLineMath` sem dependências Android na camada de domínio.
- [x] Hardening dos renderizadores `BackgroundRenderer` e `SpatialLineRenderer`, com validação de matrizes, prevenção de criação duplicada, limpeza de shaders e liberação segura de VAO, VBO, programas e texturas.
- [x] Simplificação da inicialização do Koin: `AetherisApplication` mantém o contexto global e `MainActivity` utiliza a injeção automaticamente, aplicando `AetherisTheme`.
- [x] Desativação intencional de `Config.DepthMode.AUTOMATIC` após o diagnóstico de falha nativa em `ComputeDisparity`, preservando planos, hit tests, âncoras e point cloud.
- [x] Estabelecimento de uma baseline verde com **32/32 testes unitários aprovados** por `testDebugUnitTest`.

### 🛠️ Desafios de Engenharia & Diagnóstico
1. **Evolução incompatível do contrato espacial:**
- *Causa:* Os testes ainda utilizavam `SpatialData`, enquanto a produção já expunha `StateFlow<SpatialFrameData>` e novos métodos de hit test e ancoragem.
- *Solução:* Atualização dos doubles de teste, assinaturas e propriedades observadas, mantendo `normalizedX` e `normalizedY` compatíveis com a interface.
2. **Mocks incompletos das classes ARCore:**
- *Causa:* O processador passou a validar `frame.camera.trackingState` e `Point.orientationMode`, mas os mocks não forneciam esses comportamentos e geravam `MockKException`.
- *Solução:* Modelagem explícita do estado da câmera e do modo `ESTIMATED_SURFACE_NORMAL`, preservando as validações usadas em produção.
3. **Liberação incorreta de `PointCloud` nos testes:**
- *Causa:* A implementação utiliza `use`, que encerra o recurso por `close()`, enquanto os testes verificavam a chamada antiga a `release()`.
- *Solução:* Atualização dos testes para verificar exatamente uma chamada a `pointCloud.close()`, inclusive nos fluxos excepcionais.
4. **Falha nativa da Depth API no dispositivo:**
- *Causa:* Embora o aparelho anunciasse suporte a `DepthMode.AUTOMATIC`, o pipeline apresentava falha interna em `ComputeDisparity`.
- *Solução:* Manutenção da detecção de suporte como telemetria e configuração efetiva de `DepthMode.DISABLED`, evitando instabilidade sem remover as funções centrais de medição.
5. **Gerenciamento defensivo de recursos OpenGL e ARCore:**
- *Causa:* Falhas durante compilação de shaders, vinculação de programas, criação de buffers ou encerramento de âncoras poderiam deixar recursos parcialmente inicializados.
- *Solução:* Rotinas idempotentes de destruição, restauração de bindings em blocos `finally`, validação de handles e encapsulamento de `Anchor.detach()`.

### 📊 Métricas de Validação
- **Regressão inicial:** 18 falhas em 32 testes após a evolução dos contratos.
- **Primeira estabilização:** redução para 13 falhas, concentradas nos mocks ARCore e no repositório.
- **Segunda estabilização:** redução para 3 falhas, todas no `SpatialSensorRepositoryTest`.
- **Resultado final registrado:** **32 testes executados, 0 falhas – BUILD SUCCESSFUL**.
- **Comando de validação:** `./gradlew testDebugUnitTest --no-configuration-cache`.

### 🏛️ Decisões de Arquitetura (ADR)
- **ADR-014: Defensive ARCore/OpenGL Resource Management and Depth Fallback**
  - **Contexto:** O pipeline combina objetos nativos de vida curta (`Frame`, `PointCloud`, `Anchor`), recursos de GPU dependentes do contexto EGL e funcionalidades opcionais que podem falhar mesmo quando declaradas como suportadas pelo hardware.
  - **Decisão:** Tratar indisponibilidades transitórias nas bordas da camada `data`, garantir liberação determinística dos recursos, manter o domínio livre de dependências Android e permitir fallback explícito da Depth API sem interromper planos, hit tests e ancoragem.

---

## 🚀 [Dia 08] - 2026-08-29: Projeção World-to-Screen, Badge Flutuante em Compose e Ancoragem Anti-Drift (ARCore Anchor)

### 🎯 Objetivos Concluídos
- [x] Implementação do caso de uso `ProjectWorldToScreenUseCase` realizando a transformação projetiva completa ($3\text{D} \to 2\text{D}$): coordenadas de mundo $\to$ clip space ($M_{proj} \times M_{view}$) $\to$ coordenadas normalizadas de dispositivo (NDC) $\to$ espaço de tela em pixels.
- [x] Adição de guarda de *Frustum Clipping* ($w_c \le 0$) para ocultar instantaneamente o badge quando o vetor de medição estiver atrás do plano da câmera, evitando divisão por zero e artefatos de projeção invertida.
- [x] Renderização da etiqueta flutuante reativa (`FloatingMeasurementBadge`) em Jetpack Compose, acompanhando o ponto médio do vetor espacial com leitura de distância e incerteza ($\pm\sigma$) em tempo real.
- [x] Modelagem de nós espaciais (`AnchorSlot.START`, `AnchorSlot.END`) no domínio e extensão de `SpatialFrameData`.
- [x] Implementação de `createAnchorAt` em `ArCoreHitTestProcessor` com suporte a planos poligonais e pontos ToF/Depth.
- [x] Gerenciamento determinístico do ciclo de vida nativo de âncoras (`createAnchor`, `detach`) no `SpatialSensorRepositoryImpl`, corrigindo automaticamente as coordenadas $(tx, ty, tz)$ a cada otimização do grafo de poses do SLAM.
- [x] Reatividade no `MeasurementViewModel` propagando medições corrigidas continuamente sem provocar *GC churn*.
- [x] Resolução de conflito estrutural de *Class Shadowing* no source set de testes e consolidação de **25/25 testes unitários na JVM** passando com MockK e Google Truth.
- [x] Registro da decisão arquitetural formal no `ADR-013`.

### 🛠️ Desafios de Engenharia & Diagnóstico em Hardware
1. **Class Shadowing no Source Set de Testes:**
- *Causa:* O arquivo `ArCoreHitTestProcessorTest.kt` continha uma declaração acidental de `class ArCoreHitTestProcessor` no diretório `src/test/`, mascarando a classe real de produção em `src/main/` e impedindo a resolução de novos métodos durante a compilação de testes unitários.
- *Solução:* Substituição do stub por uma suíte de testes unitários legítima cobrindo criação de âncoras, hit-testing de planos e validação de superfícies.
2. **Derivação Métrica Espacial (Drift em Medições Longas):**
- *Causa:* Coordenadas euclidianas estáticas $(X, Y, Z)$ salvas no primeiro frame sofriam descolamento visual quando o otimizador SLAM/BA do ARCore recalculava a origem do mundo durante a movimentação do usuário.
- *Solução:* Vinculação dos nós a objetos nativos `com.google.ar.core.Anchor` com consulta dinâmica da `Pose` a cada ciclo de `updateFrameData`.
3. **Frustum Culling de Elementos 2D:**
- *Causa:* Projeções matemáticas convencionais sem validação de $w_c$ geravam posições de tela espelhadas quando o usuário virava de costas para o objeto medido.
- *Solução:* Retorno determinístico de `null` no caso de uso caso $w_c \le 0.001\text{f}$, instruindo o Compose a não desenhar o badge fora do cone de visão da câmera.

### 📊 Métricas de Validação no Dispositivo (Motorola Edge 50 Fusion)
- **Convergência VIO (Visual-Inertial Odometry):** Inicialização recorde atingindo `VIO_TRACKING` em apenas **398,05 ms** (redução de 9,7% em relação aos 441 ms do Dia 07).
- **Consistência Geométrica do SLAM:** Otimização de mapa (`MAP SOLVE: USER_SUCCESS`) reduzindo o custo de $20.349$ para $171$ em 4 iterações, com 26 keyframes e 212 marcos mapeados.
- **Taxa de Inliers Visuais:** **93,1% de inliers consistentes** (94 pontos rastreados simultaneamente).
- **Estabilidade de Ancoragem:** Deslocamento nulo da linha 3D e do badge flutuante após caminhada de 10 metros com perda e recuperação total de linha de visada.
- **Performance de Testes:** 25 testes unitários executados em ~2s na JVM.

### 🏛️ Decisões de Arquitetura (ADR)
- **ADR-013: Native ARCore Anchor Tracking & Pose Graph Correction**
  - **Contexto:** Necessidade de manter pontos de medição milimetricamente fixos em relação aos objetos reais durante movimentações longas no espaço.
  - **Decisão:** Associação dos pontos A e B a nós nativos `Anchor` do ARCore, propagação frame a frame das coordenadas corrigidas pelo grafo de poses via `StateFlow` e invocação determinística de `anchor.detach()` para prevenção de vazamento de memória nativa C++.

---

## 🎨 [Dia 07] - 2026-08-28: Pipeline Gráfico OpenGL ES 3.0, Estabilização EGL e Compatibilidade 16 KB

### 🎯 Objetivos Concluídos
- [x] Criação do `BackgroundRenderer` com shaders GLSL ES 3.0 e suporte a `GL_TEXTURE_EXTERNAL_OES` para projeção com *zero-copy* do feed de vídeo da câmera.
- [x] Implementação do `SpatialLineRenderer` em OpenGL ES 3.0 para traçado dos nós de ancoragem (`GL_POINTS`) e do vetor de medição (`GL_LINES`) no espaço tridimensional.
- [x] Multiplicação matricial Model-View-Projection ($M_{clip} = M_{proj} \times M_{view} \times M_{model}$) em tempo real alimentada pelas matrizes da câmera ARCore.
- [x] Prealocação estática de matrizes e buffers nativos diretos (`FloatBuffer`) garantindo zero alocação de memória no loop de renderização (Zero GC Churn).
- [x] Integração completa dos renderizadores no ciclo do `GLSurfaceView` (`onSurfaceCreated`, `onSurfaceChanged`, `onDrawFrame`) em `ArCameraFeed`.
- [x] Conexão dos pontos A e B do `uiState` à camada gráfica via `rememberUpdatedState`.
- [x] Criação da suíte `SpatialLineMathTest` e atualização de `MeasurementViewModelTest` com 100% dos testes unitários passando na JVM.
- [x] Registro da decisão arquitetural no `ADR-012`.

### 🛠️ Desafios de Engenharia & Diagnóstico em Hardware
1. **Condição de Corrida no Ciclo de Vida do ARCore (`AR_ERROR_SESSION_PAUSED`):**
- *Causa:* A `GLThread` chamava `session.update()` antes da Main Thread executar `session.resume()`, e o encerramento concorrente no `onPause` causava falha de precondição no scheduler do MediaPipe.
- *Solução:* Centralização estrita do ciclo de vida na Main Thread via flag `@Volatile isRunning` e sincronização determinística no `DisposableEffect` (no pause: paralisa a `GLSurfaceView` antes da `Session`; no resume: retoma a `Session` antes da `GLSurfaceView`).
2. **Compatibilidade com Páginas de Memória de 16 KB (Android 15+):**
- *Causa:* O binário nativo legado `libimage_processing_util_jni.so` do CameraX continha segmentos `LOAD` desalinhados.
- *Solução:* Remoção de dependências redundantes do CameraX (câmera gerenciada pelo ARCore), upgrade do ARCore para `1.46.0` e configuração de `jniLibs.useLegacyPackaging = false` no Gradle.
3. **Flickering e Artefatos Cromáticos na GPU Qualcomm Adreno:**
- *Causa:* Chamadas repetidas a `session.setCameraTextureNames()` a 60 FPS no `onDrawFrame` e coordenadas UV não inicializadas no primeiro frame.
- *Solução:* Vinculação atômica única do ID de textura OES, amostragem obrigatória com `GL_CLAMP_TO_EDGE` e transformação contínua de coordenadas normalizadas no `BackgroundRenderer`.

### 📊 Métricas de Validação no Dispositivo (Motorola Edge 50 Fusion)
- **Taxa de Quadros:** 60 FPS contínuos e sustentados ao longo de mais de 850 frames de vídeo renderizados.
- **Convergência VIO (Visual-Inertial Odometry):** Transição para `VIO_TRACKING` em apenas **441 ms**.
- **Mapeamento Espacial 3D:** Construção de mapa ADF contendo 26 keyframes, 252 landmarks físicos e taxa de inliers visuais de **94,6%**.
- **Performance de Testes:** Suíte completa de testes da JVM executada em ~4s.

### 🏛️ Decisões de Arquitetura (ADR)
- **ADR-012: Zero-Copy OES Camera Texture and OpenGL ES 3.0 Spatial Geometry Pipeline**
  - **Contexto:** Necessidade de renderização em alta frequência (60 FPS) do vídeo da câmera e da geometria métrica sem alocações dinâmicas na GPU/CPU.
  - **Decisão:** Adoção de textura externa OES via GLSL ES 3.0, prealocação estática de buffers/matrizes e consumo síncrono do estado do Compose pela thread gráfica EGL.

---

## 🎯 [Dia 06] - 2026-08-27: Spatial Raycasting, Polygon Gating e Testes Unitários de Colisão

### 🎯 Objetivos Concluídos
- [x] Criação do processador de baixo nível `ArCoreHitTestProcessor` para projeção de raios ópticos a partir de coordenadas normalizadas de tela $[0.0, 1.0]$.
- [x] Implementação de filtragem estrita por polígono convexo (`isPoseInPolygon`) para eliminar extrapolações de planos infinitos e falsos positivos no vácuo.
- [x] Estabelecimento de fallback determinístico para pontos ToF / Depth API (`Point`) com rastreamento ativo.
- [x] Refatoração do `SpatialSensorRepositoryImpl`, eliminando a busca heurística 2D em favor do raycasting nativo do ARCore.
- [x] Mapeamento bidirecional de viewport entre `GLSurfaceView` (`onSurfaceChanged`), `ArCameraFeed`, repositório e `MeasurementViewModel`.
- [x] Suíte completa de testes unitários na JVM (`ArCoreHitTestProcessorTest`) cobrindo 6 cenários de colisão, planos fora de limites, clamping de tela e descarte de poses instáveis com MockK e Google Truth.
- [x] Registro da decisão técnica formal no `ADR-011`.
- [x] Validação integral da suíte de testes unitários (`./gradlew testDebugUnitTest`) executada em 4s com cache.

### 🏛️ Decisões de Arquitetura (ADR)
- **ADR-011: Spatial Raycasting and Convex Polygon Gating**
  - **Contexto:** A busca heurística 2D anterior gerava imprecisão métrica cumulativa e não garantia que os pontos ancorados pertencessem a superfícies físicas coplanares ou estáveis.
  - **Decisão:** Adoção do `Frame.hitTest` nativo com priorização de `Plane` dentro do polígono de suporte (`isPoseInPolygon`), fallback para pontos de profundidade ToF e conversão direta da `Pose` do ARCore para a entidade imutável de domínio `Point3D(x, y, z)` sem contaminar a camada `domain` com o SDK Android.

---

## 📱 [Dia 05] - 2026-08-26: Hardware Óptico, Ciclo de Vida ARCore e Testes Unitários de Apresentação

### 🎯 Objetivos Concluídos
- [x] Implementação do gerenciador declarativo de permissões em tempo de execução `CameraPermissionHandler` no Jetpack Compose.
- [x] Criação do `ArCoreSessionManager` para controle do ciclo de vida da sessão AR, ativação do sensor de profundidade (`DepthMode.AUTOMATIC`) e liberação de recursos de memória.
- [x] Construção do componente visual `ArCameraFeed` conectando `GLSurfaceView` (OpenGL ES 3.0) ao ciclo de vida do Compose via `DisposableEffect` e `LifecycleEventObserver`.
- [x] Integração do feed da câmera na `MeasurementScreen` como camada base (`z-index: 0`) sob o HUD tático.
- [x] Tratamento de telemetria de rastreamento (`TrackingState`) no `MeasurementViewModel` sem quebrar o desacoplamento de camadas da Clean Architecture.
- [x] Padronização da biblioteca `kotlinx-coroutines-test` no catálogo de dependências (`gradle/libs.versions.toml` e `app/build.gradle.kts`).
- [x] Criação da suíte de testes unitários `MeasurementViewModelTest` com dublê de repositório (`FakeSpatialSensorRepository`), cobrindo fluxo de ancoragem de Pontos A/B, cálculo determinístico de distância, reset de medição e emissão de telemetria reativa.
- [x] Validação integral da suíte de testes unitários e compilação do APK de Debug (`./gradlew testDebugUnitTest assembleDebug`).

### 🏛️ Decisões de Arquitetura (ADR)
- **ADR-009: Gerenciamento Declarativo de Permissões Ópticas no Compose**
  - **Contexto:** O ARCore exige permissão de câmera em tempo de execução. O fluxo tradicional baseado em callbacks imperativos de `Activity` acopla a camada de apresentação ao framework e dificulta a modularização.
  - **Decisão:** Criação do componente `CameraPermissionHandler` utilizando `rememberLauncherForActivityResult`, garantindo tela de bloqueio e solicitação reativa sob demanda diretamente na árvore do Compose.
- **ADR-010: Isolamento de Ciclo de Vida do ARCore e Contexto EGL**
  - **Contexto:** A `GLSurfaceView` e a sessão ARCore exigem sincronização estrita com o ciclo de vida do Android (`ON_RESUME`, `ON_PAUSE`, `ON_DESTROY`) para evitar vazamentos de memória e corrupção do contexto gráfico.
  - **Decisão:** Encapsulamento da inicialização e destruição no `ArCoreSessionManager`, acoplado ao ciclo de vida da tela via `DisposableEffect` dentro de `ArCameraFeed`.

---

## 📐 [Dia 04] - 2026-08-25: Processamento de Buffers AR e Interface HUD em Jetpack Compose

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

### 🏛️ Decisões de Arquitetura (ADR)
- **ADR-007: Filtragem e Descarte de Ruído em Buffers Brutos (PointCloud)**
  - **Contexto:** Sensores ópticos e de tempo de voo (ToF) geram dispersão de dados e pontos espírios em superfícies reflexivas ou de baixa iluminação.
  - **Decisão:** O `ArCoreFrameProcessor` aplica um limiar de confiança configurável ($\ge 30\%$) diretamente na leitura do `FloatBuffer`, descartando artefatos antes de criar instâncias imutáveis de `Point3D` no domínio.
- **ADR-008: Unidirectional Data Flow (UDF) com StateFlow no HUD de Metrologia**
  - **Contexto:** A interface gráfica precisa renderizar dados de alta frequência da câmera ao mesmo tempo em que reage às interações pontuais do usuário (ancoragem do Ponto A e Ponto B).
  - **Decisão:** Centralização de todo o estado em `MeasurementUiState` imutável, exposto via `StateFlow` pelo `MeasurementViewModel`, garantindo que a UI apenas observe e emita eventos de clique sem conter lógica de negócio.

---

## 📡 [Dia 03] - 2026-08-24: Contrato de Repositório de Sensores e Telemetria Reativa

### 🎯 Objetivos Concluídos
- [x] Criação dos modelos de telemetria espacial (`TrackingStatus`, `SpatialFrameData`).
- [x] Definição do contrato de repositório `SpatialSensorRepository` na camada `domain`.
- [x] Implementação de `SpatialSensorRepositoryImpl` com `StateFlow` na camada `data`.
- [x] Injeção de dependência via Koin como `Single`.
- [x] Testes unitários do repositório garantindo reatividade e integridade de estado.
- [x] Configuração da pipeline de integração contínua (CI) com GitHub Actions (`.github/workflows/android.yml`).

### 🏛️ Decisões de Arquitetura (ADR)
- **ADR-005: Desacoplamento do Pipeline de Sensores via Repositório Reativo**
  - **Contexto:** O hardware emite frames espaciais em 30 a 60 FPS. A camada de domínio não deve ser bloqueada pela taxa de quadros do sensor.
  - **Decisão:** Uso de `StateFlow<SpatialFrameData>` com atualizações atômicas (`.update { ... }`).
- **ADR-006: Abstração de Hit-Testing e Raycasting**
  - **Contexto:** Projeção de coordenadas 2D de tela para coordenadas físicas 3D ($X, Y, Z$).
  - **Decisão:** O repositório expõe o método `performHitTest(x, y)` delegando a interseção geométrica para a nuvem de pontos ou mapa de profundidade denso.

---

## 🧮 [Dia 02] - 2026-08-23: Domínio Matemático Puro e Modelagem Física

### 🎯 Objetivos Concluídos
- [x] Criação das entidades imutáveis: `Point3D`, `BoundingBox3D`, `DistanceMeasurement`, `MassEstimate`.
- [x] Implementação dos casos de uso: `CalculateDistanceUseCase` e `EstimateSpatialDimensionsUseCase`.
- [x] Implementação da propagação dinâmica de incerteza metrológica ($\pm\sigma$).
- [x] Cobertura de 100% em testes unitários com JUnit 4 e Google Truth na JVM.

### 🏛️ Decisões de Arquitetura (ADR)
- **ADR-003: Isolamento do Domínio Matemático em Kotlin Puro**
  - **Decisão:** Zero dependências do Android SDK na camada `domain` para garantir portabilidade e execução instantânea de testes unitários.
- **ADR-004: Incerteza Dinâmica como Entidade de Primeira Classe**
  - **Decisão:** Toda medição física carrega seu desvio padrão de erro intrínseco baseado no modelo físico do sensor.

---

## 🏁 [Dia 01] - 2026-08-22: Fundação, Setup e Governança

### 🎯 Objetivos Concluídos
- [x] Configuração do projeto com Kotlin 2.x, Jetpack Compose (Material 3), Gradle Kotlin DSL e Version Catalogs (`libs.versions.toml`).
- [x] Estruturação da Clean Architecture (`domain`, `data`, `presentation`).
- [x] Injeção de dependência com Koin.
- [x] Publicação do repositório no GitHub com licença Apache 2.0.

### 🏛️ Decisões de Arquitetura (ADR)
- **ADR-001: Adoção do Koin em vez de Hilt/Dagger**
  - **Decisão:** Injeção de dependência 100% Kotlin puro sem geração pesada de código ou problemas com novas versões do compilador K2.
- **ADR-002: Licenciamento Apache 2.0 e Estratégia Open Core**
  - **Decisão:** Núcleo aberto para autoridade técnica e portfólio público, com suporte a extensões proprietárias via contratos de plugin.

---

## 🔮 Próximos Passos (Dia 10)
- [ ] Implementação de medição multiponto e sequenciamento de polilinhas 3D (`Polyline3D`).
- [ ] Cálculo da área de superfícies coplanares poligonais com projeção no plano dominante.
- [ ] Renderização de malha poligonal translúcida com `GL_TRIANGLE_FAN` no OpenGL ES 3.0.
- [ ] Exportação de telemetria espacial e relatórios metrológicos em JSON e CSV.
- [ ] Execução final de `testDebugUnitTest`, `assembleDebug` e validação em dispositivo após o hardening.