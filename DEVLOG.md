# 📐 Aetheris - Diário de Desenvolvimento (DEVLOG)

Registro contínuo da engenharia, decisões arquiteturais (ADRs), modelagem matemática e evolução do ecossistema Aetheris.

---

## 🚀 [Dia 12] - 2026-09-01: Estimativa de Massa por Densidade, Threading OpenGL e Validação Ponta a Ponta em Hardware

### 🎯 Objetivos Concluídos

- [x] Criação do catálogo de materiais e densidades físicas (`MaterialDensity`) abrangendo sólidos comuns (madeira, alumínio, aço, vidro, concreto, plásticos).
- [x] Modelagem da entidade `MassEstimate` contendo massa calculada em quilogramas e gramas, margem de incerteza propagada ($\pm\sigma$) e incerteza relativa percentual.
- [x] Implementação do caso de uso `EstimateMassUseCase` integrando volume tridimensional e densidade volumétrica com propagação rigorosa de incerteza combinada.
- [x] Diagnóstico e resolução de contenção de concorrência e race condition entre a Main Thread/Coroutines e a `GLThread` (render thread) do ARCore.
- [x] Implementação de fila de operações orientada ao frame (`Frame-Affine Placement Queue`) em `SpatialSensorRepositoryImpl`, executando raycasting e criação de âncoras sincronizadas com o frame de renderização mais recente.
- [x] Habilitação de `Instant Placement` (`Config.InstantPlacementMode.LOCAL_Y_UP`) como fallback determinístico para posicionamento imediato de pontos em superfícies com baixa densidade de planos.
- [x] Implementação de throttling na sondagem de superfícies (`hasValidSurfaceAt`) fixado em 5 Hz (200 ms), reduzindo substancialmente o overhead de CPU e consumo de bateria.
- [x] Inclusão de telemetria diagnóstica isolada para builds de Debug sem impactar a performance em Release.
- [x] Atualização completa da suíte de testes unitários (`SpatialSensorRepositoryTest`, `MeasurementViewModelTest`, etc.) totalizando **128/128 testes unitários aprovados na JVM**.
- [x] Validação funcional ponta a ponta em hardware real (Motorola Edge 50 Fusion): toque na tela $\to$ ancoragem espacial $\to$ medição linear $\to$ cálculo volumétrico $\to$ estimativa de massa.
- [x] Registro formal da decisão arquitetural no `ADR-016`.
- [x] Publicação das alterações nos commits `a92b40b`, `a72eed1` e `b18502c`.

### 📐 Modelagem Matemática

A estimativa de massa é obtida a partir do produto do volume tridimensional $V$ pela densidade volumétrica do material $\rho$:

$$M = V \times \rho$$

onde:
- $V$ representa o volume estimado da caixa delimitadora (AABB) em $\text{m}^3$;
- $\rho$ representa a densidade do material em $\text{kg/m}^3$.

A propagação da incerteza combinada $u_M$ considera a incerteza volumétrica $u_V$ e a incerteza intrínseca da densidade do material $u_\rho$:

$$u_M = \sqrt{(\rho \times u_V)^2 + (V \times u_\rho)^2}$$

Essa modelagem assegura que a incerteza final reflita tanto a precisão do sensor espacial quanto a tolerância na composição física do material selecionado.

### 🛠️ Desafios de Engenharia & Diagnóstico em Hardware

1. **Concorrência entre Coroutines e a GLThread do ARCore:**
- *Causa:* Chamadas assíncronas para `createAnchor` ou `performHitTest` executadas a partir de dispatchers de Coroutine tentavam acessar o estado nativo da `Session` enquanto a thread OpenGL estava no meio do ciclo de renderização de frame, gerando exceções nativas transitórias e falha silenciosa na fixação de pontos.
- *Solução:* Criação de uma fila de comandos atômica (`ConcurrentLinkedQueue`) no repositório, consumida de forma síncrona dentro de `onFrameUpdate(frame)` na `GLThread`, garantindo que todas as interações com o ARCore ocorram estritamente no frame ativo.
2. **Superfícies Pouco Texturizadas e Falha de Planos Convencionais:**
- *Causa:* Ambientes internos homogêneos demoravam vários segundos para consolidar planos poligonais, bloqueando o usuário de fixar o Ponto A ou B.
- *Solução:* Ativação do modo `Instant Placement` com fallback em cascata: plano delimitado $\to$ ponto ToF/Depth $\to$ ponto Instant Placement (`InstantPlacementPoint`).
3. **Sobrecarga de Raycasting no Retículo (Crosshair):**
- *Causa:* A sondagem de superfície para feedback cromático do retículo executava a 60 FPS, consumindo ciclos desnecessários de CPU.
- *Solução:* Aplicação de throttling por timestamp garantindo intervalo mínimo de 200 ms (5 Hz) entre as verificações de superfície.

### 📊 Métricas de Validação

- **Suíte de Testes Unitários:** **128 testes executados, 0 falhas – BUILD SUCCESSFUL**.
- **Comando de validação:** `./gradlew testDebugUnitTest lintDebug assembleDebug --no-configuration-cache`.
- **Validação em Hardware:** Posicionamento de âncoras verificado com taxa de sucesso de 100% em múltiplos testes de iluminação e distância no Motorola Edge 50 Fusion.

### 🏛️ Decisões de Arquitetura (ADR)

- **ADR-016: Frame-Affine Placement Queue & Instant Placement Fallback**
  - **Contexto:** A criação de âncoras e o raycasting requerem sincronia absoluta com o frame ativo na thread gráfica, e a dependência exclusiva de planos poligonais tornava a inicialização lenta em superfícies lisas.
  - **Decisão:** Enfileirar requisições de ancoragem para execução direta no frame ativo da `GLThread` e habilitar o `Instant Placement` como fallback imediato, mantendo o domínio desacoplado via contratos reativos.

---

## 🚀 [Dia 11] - 2026-08-31: Resolução Explícita do Koin no Nível da Activity

### 🎯 Objetivo Concluído

- [x] Refatoração isolada do arquivo `MainActivity.kt` para remover a dependência de um contexto Koin adicional dentro da árvore do Jetpack Compose.
- [x] Resolução de `MeasurementViewModel` diretamente no ciclo de vida da `ComponentActivity` por meio do delegate `by viewModel()`.
- [x] Resolução do singleton `ArCoreSessionManager` diretamente na Activity por meio do delegate `by inject()`.
- [x] Passagem explícita das dependências para `MeasurementScreen`.
- [x] Remoção do wrapper `KoinAndroidContext` da composição principal.
- [x] Preservação da inicialização global do Koin realizada pelo `AetherisApplication`.
- [x] Validação completa dos testes unitários, Android Lint e montagem do APK de Debug.
- [x] Publicação da alteração no commit `5352403` (`fix: resolve Koin dependencies at activity level`).
- [x] Sincronização bem-sucedida entre `HEAD`, `main` e `origin/main`, com a árvore de trabalho limpa.

### 🔍 Escopo do Dia

O trabalho foi intencionalmente limitado a um único arquivo de produção:

```text
app/src/main/java/org/aetheris/app/MainActivity.kt
```

A alteração teve como objetivo tratar o aviso observado durante a execução no dispositivo:

```text
No Koin context defined in Compose, fallback to default Koin context.
```

O fallback funcionava corretamente porque o contêiner global já era iniciado em `AetherisApplication`, mas a resolução implícita dentro do Compose gerava uma mensagem desnecessária no Logcat.

### 🛠️ Solução Aplicada

Antes da refatoração, a `MeasurementScreen` resolvia suas dependências por parâmetros padrão durante a composição:

```kotlin
MeasurementScreen()
```

A `MainActivity` também envolvia a interface com um contexto Compose adicional:

```kotlin
KoinAndroidContext {
    MeasurementScreen()
}
```

Depois da refatoração, a Activity passou a possuir explicitamente as dependências associadas ao seu ciclo de vida:

```kotlin
private val measurementViewModel:
    MeasurementViewModel by viewModel()

private val arCoreSessionManager:
    ArCoreSessionManager by inject()
```

Essas instâncias são fornecidas diretamente à tela:

```kotlin
MeasurementScreen(
    viewModel = measurementViewModel,
    sessionManager = arCoreSessionManager
)
```

### 🏛️ Impacto Arquitetural

1. **Ciclo de vida explícito do ViewModel:**
- `MeasurementViewModel` permanece associado à `MainActivity`.
- Mudanças de configuração continuam utilizando o gerenciamento padrão de ViewModel do Android.

2. **Singleton da sessão ARCore preservado:**
- `ArCoreSessionManager` continua sendo fornecido pela mesma definição `single` do módulo Koin.
- Nenhuma segunda sessão ARCore é criada pela refatoração.

3. **Composição mais simples:**
- A árvore Compose recebe dependências prontas.
- A tela permanece testável porque seus parâmetros continuam podendo ser substituídos.

4. **Inicialização centralizada:**
- `AetherisApplication` continua sendo o único ponto responsável por chamar `startKoin` e registrar `appModule`.
- Não existe um segundo contêiner de dependências controlado pela composição.

### 📊 Validação

Pipeline utilizado:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug \
  --no-configuration-cache
```

Resultado:

```text
BUILD SUCCESSFUL
```

A alteração não modificou regras de domínio, cálculos métricos, contratos de repositório, renderização OpenGL ou estado de medição. Por isso, a suíte existente foi utilizada como teste de regressão integral.

### 📦 Controle de Versão

```text
Commit: 5352403
Mensagem: fix: resolve Koin dependencies at activity level
Branch: main
Remoto: origin/main
Status final: working tree clean
```

---

## 📦 [Dia 10] - 2026-08-30: Medição Tridimensional Sequencial, Volume com Incerteza e Validação em Hardware

### 🎯 Objetivos Concluídos

- [x] Criação do modelo `DimensionAxis` para representar e ordenar os três eixos espaciais: `WIDTH`, `HEIGHT` e `DEPTH`.
- [x] Implementação de `SpatialDimensions` como estado imutável das medições confirmadas de largura, altura e profundidade.
- [x] Inclusão de propriedades derivadas para contagem de eixos medidos, identificação do próximo eixo pendente e verificação de conclusão da medição tridimensional.
- [x] Criação do modelo `VolumeMeasurement`, representando volume em metros cúbicos e litros, margem de incerteza, limites mínimo e máximo e incerteza relativa percentual.
- [x] Implementação de formatação métrica automática para apresentação em litros ou metros cúbicos.
- [x] Criação do `CalculateVolumeUseCase` para calcular o volume aproximado da caixa delimitadora espacial a partir de três medições lineares.
- [x] Implementação da propagação independente das incertezas de largura, altura e profundidade no resultado volumétrico.
- [x] Refatoração do `MeasurementUiState` para armazenar dimensões confirmadas, volume calculado, eixo atual e progresso completo da captura tridimensional.
- [x] Refatoração do `MeasurementViewModel` para coordenar o fluxo sequencial `WIDTH → HEIGHT → DEPTH`.
- [x] Implementação da confirmação individual de cada eixo, com limpeza das âncoras entre as etapas e preservação das dimensões já registradas.
- [x] Cálculo automático do volume após a confirmação da profundidade.
- [x] Implementação de reset parcial da dimensão atual e reset completo da medição tridimensional.
- [x] Registro de `CalculateVolumeUseCase` no módulo Koin e atualização da injeção do `MeasurementViewModel`.
- [x] Refatoração completa da `MeasurementScreen` com indicadores de progresso para largura, altura e profundidade.
- [x] Inclusão de controles contextuais para fixar pontos, confirmar dimensões, refazer o eixo atual e reiniciar todo o processo.
- [x] Inclusão do painel final de volume com incerteza e resumo das três dimensões confirmadas.
- [x] Criação e ampliação das suítes de testes de domínio, casos de uso, estado de interface e ViewModel.
- [x] Validação integral por testes unitários, Android Lint e montagem do APK de Debug.
- [x] Publicação da funcionalidade no commit `5a07c5c` (`feat: add three-axis spatial volume measurement`).

### 📐 Modelagem Matemática

O volume aproximado é calculado como uma caixa delimitadora tridimensional:

$$V = w \times h \times d$$

onde:
- $w$ representa a largura;
- $h$ representa a altura;
- $d$ representa a profundidade.

A incerteza volumétrica é propagada considerando as incertezas independentes dos três eixos:

$$u_V = \sqrt{(h \times d \times u_w)^2 + (w \times d \times u_h)^2 + (w \times h \times u_d)^2}$$

Essa formulação evita divisões por zero e continua válida quando uma das dimensões medidas é igual a zero.

O resultado é apresentado como uma estimativa geométrica da caixa delimitadora do objeto, não como seu volume físico exato. Objetos com formas irregulares exigirão segmentação espacial e reconstrução geométrica em etapas futuras.

### 🏛️ Decisões de Arquitetura

1. **Sequenciamento explícito dos eixos:**
- O domínio define a ordem `WIDTH → HEIGHT → DEPTH` sem depender da interface Android.
- O próximo eixo é derivado das dimensões ainda ausentes, reduzindo estados inconsistentes.

2. **Estado imutável das dimensões:**
- Cada confirmação produz uma nova instância de `SpatialDimensions`.
- Medições anteriores são preservadas enquanto as âncoras do eixo atual são liberadas.

3. **Separação entre distância e volume:**
- `DistanceMeasurement` representa medições lineares.
- `VolumeMeasurement` representa o resultado volumétrico e sua incerteza.
- `CalculateVolumeUseCase` concentra a regra matemática sem dependências do Android ou ARCore.

4. **Orquestração no ViewModel:**
- A interface apenas emite eventos de posicionamento, confirmação, repetição e reset.
- O `MeasurementViewModel` controla a transição entre eixos e o cálculo final.

5. **Volume como aproximação AABB:**
- O primeiro estágio usa uma caixa delimitadora formada por largura, altura e profundidade.
- A decisão mantém o fluxo testável e prepara a arquitetura para futura segmentação de objetos e nuvens de pontos.

### 📊 Cobertura e Validação

Foram adicionados ou ampliados testes para:
- ordem e transição dos valores de `DimensionAxis`;
- imutabilidade e progressão de `SpatialDimensions`;
- conversão entre metros cúbicos e litros;
- validação de volumes e incertezas não negativos e finitos;
- limites mínimo e máximo de `VolumeMeasurement`;
- propagação matemática das incertezas dos três eixos;
- comportamento com dimensões ou volume iguais a zero;
- rejeição de conjuntos incompletos de dimensões;
- progressão de largura para altura e profundidade no estado visual;
- confirmação sequencial dos três eixos pelo ViewModel;
- limpeza das âncoras entre dimensões;
- cálculo automático do volume ao concluir a profundidade;
- reset parcial e completo da medição.

Pipeline final executado:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug \
  --no-configuration-cache
```

Resultado:

```text
BUILD SUCCESSFUL
53 actionable tasks: 16 executed, 37 up-to-date
```

### 📱 Validação Inicial no Dispositivo

O APK foi executado em um aparelho físico com ARCore. A sessão:
- iniciou corretamente;
- carregou todas as dependências do Koin;
- processou aproximadamente 1.216 frames da câmera;
- encerrou com `Session::PauseWithAnalytics returning OK`;
- não apresentou `FATAL EXCEPTION`, ANR ou crash do processo;
- não apresentou exceções Kotlin relacionadas às novas dimensões ou ao cálculo de volume.

O teste funcional completo não pôde ser concluído devido à baixa luminosidade do ambiente. O ARCore registrou dificuldade para encontrar pontos visuais consistentes e refinar planos físicos:
- 246 ocorrências internas de refinamento de plano sem inliers suficientes;
- 5 ocorrências internas de `ComputeDisparity` no serviço nativo do ARCore;
- uma ocorrência de extração de características acima do tempo esperado;
- atraso perceptível na inicialização e no encerramento da sessão.

Apesar dessas mensagens nativas, não houve encerramento anormal. A validação funcional será repetida em ambiente bem iluminado e com superfícies texturizadas antes de novas alterações no pipeline de medição.

### 🛠️ Diagnóstico Técnico

1. **Baixa luminosidade e poucos marcos visuais:**
- *Efeito:* dificuldade para estabilizar planos e habilitar a mira de posicionamento.
- *Ação definida:* repetir o teste com iluminação uniforme, movimento lento da câmera e superfícies com textura.
2. **Mensagens internas de `ComputeDisparity`:**
- *Observação:* continuaram presentes no Google Play Services for AR mesmo com `DepthMode.DISABLED` na configuração pública da sessão.
- *Decisão:* manter o fallback sem Depth API e não alterar o domínio ou o ViewModel com base apenas em mensagens internas do serviço nativo.
3. **Aviso de contexto Compose do Koin:**
- *Efeito:* o Koin utilizou corretamente o contexto global iniciado pelo `AetherisApplication`.
- *Prioridade:* baixa; não afetou a resolução das dependências nem o funcionamento do aplicativo.
4. **Encerramento OpenGL/ARCore:**
- *Observação:* ocorreu uma mensagem isolada de chamada OpenGL sem contexto corrente durante a desmontagem.
- *Resultado:* a sessão retornou `OK` e o processo encerrou normalmente.
- *Ação definida:* repetir ciclos de abrir, minimizar, restaurar e fechar o aplicativo para verificar recorrência.

### 🏛️ Decisão de Arquitetura (ADR)

- **ADR-015: Sequential Axis Capture and Uncertainty-Aware AABB Volume**
  - **Contexto:** A medição de apenas uma distância não representa as dimensões espaciais necessárias para estimar volume e, futuramente, massa por densidade.
  - **Decisão:** Capturar largura, altura e profundidade como medições lineares independentes, associar cada uma a um eixo explícito, liberar as âncoras entre etapas e calcular uma estimativa volumétrica AABB com propagação das incertezas.
  - **Consequência:** O domínio permanece puro e testável, enquanto a apresentação ganha um fluxo progressivo capaz de evoluir posteriormente para segmentação automática, reconstrução 3D e modelos físicos específicos por material.

---

## 🛡️ [Dia 09] - 2026-08-29: Hardening do Pipeline ARCore, Segurança Numérica e Regressão Completa

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

## 🔮 Próximos Passos Definidos para o Dia 13

- [ ] Implementação de seletor visual de materiais com chips de densidade no HUD do Compose.
- [ ] Renderização de badge contextual dinâmico exibindo simultaneamente volume, massa e incerteza $(\pm\sigma)$.
- [ ] Estruturação da persistência local de medições espaciais e exportação de relatórios metrológicos (JSON/CSV).
- [ ] Expansão da modelagem geométrica para cálculo de área de polígonos coplanares 3D (Fórmula de Shoelace 3D / Teorema de Stokes).
- [ ] Validação dos novos fluxos no dispositivo com pipeline completo de testes e Lint.