# 📐 Aetheris - Diário de Desenvolvimento (DEVLOG)

Registro contínuo da engenharia, decisões arquiteturais (ADRs), modelagem matemática e evolução do ecossistema Aetheris.

---

## 🚀 [Dia 14] - 2026-09-03: HUD de Procedência, Painel Adaptativo e Automação de Releases

### 🎯 Objetivos Concluídos

- [x] Criação do workflow `.github/workflows/release.yml` para automatizar releases baseadas em tags Git no formato `v*`.
- [x] Configuração do GitHub Actions para executar testes unitários, Android Lint e montagem limpa do APK de Debug antes de publicar uma release.
- [x] Geração automática de APK versionado e arquivo de checksum SHA-256.
- [x] Publicação automática dos dois artefatos em uma GitHub Release.
- [x] Classificação automática de tags `alpha`, `beta` e `rc` como pré-releases.
- [x] Apresentação da procedência espacial das âncoras diretamente no HUD de medição.
- [x] Diferenciação visual entre superfícies convencionais e o fallback por Instant Placement.
- [x] Inclusão de identificação individual das fontes dos pontos A e B.
- [x] Inclusão de aviso explícito quando a medição atual utiliza posicionamento aproximado.
- [x] Preservação da possibilidade de confirmar pontos aproximados, sem apresentá-los como equivalentes a uma superfície convencional.
- [x] Atualização cromática da mira para representar rastreamento indisponível, superfície convencional e fallback aproximado.
- [x] Correção do painel de controles para impedir que seu estado expandido cubra a mira central.
- [x] Limitação adaptativa do painel a 30% da altura disponível, mantendo o conteúdo excedente acessível por rolagem.
- [x] Compilação, instalação e validação funcional em aparelho físico.
- [x] Execução integral dos testes unitários, Android Lint e montagem do APK de Debug.

### 📦 Automação de Releases

O novo workflow é executado quando uma tag compatível é enviada ao repositório:

```text
tag v* -> validação -> APK versionado -> SHA-256 -> GitHub Release
```

Antes da publicação, o runner executa:

```bash
./gradlew clean testDebugUnitTest lintDebug assembleDebug \
  --no-configuration-cache
```

O APK e seu checksum também permanecem disponíveis como artefatos do workflow. A permissão `contents: write` é limitada ao workflow de release e utiliza o token temporário fornecido pelo próprio GitHub Actions.

O workflow foi adicionado e validado estruturalmente no repositório. Sua execução completa de ponta a ponta permanece pendente até o envio da próxima tag de versão; a tag anterior `v0.1.0-alpha` foi criada antes da inclusão dessa automação.

### 🎯 Procedência Apresentada no HUD

O HUD agora transforma a procedência armazenada no estado em informação compreensível durante a medição:

| Estado ou fonte | Apresentação | Significado |
|---|---|---|
| Rastreamento indisponível | Mira vermelha | Não é possível criar uma nova âncora. |
| Superfície convencional | Mira e indicador verdes | Existe geometria convencional reconhecida sob a mira. |
| Fallback aproximado | Mira e indicador amarelos | O próximo ponto pode depender de Instant Placement. |
| `PLANE` | `PLANO` | Ponto apoiado no polígono de um plano rastreado. |
| `FEATURE_POINT` | `PONTO VISUAL` | Ponto visual com normal de superfície estimada. |
| `DEPTH_POINT` | `PROFUNDIDADE` | Ponto proveniente de informação de profundidade. |
| `INSTANT_PLACEMENT` | `INSTANTÂNEO` | Posição inicial aproximada e sujeita a refinamento. |

Quando qualquer ponto ativo utiliza `INSTANT_PLACEMENT`, a interface apresenta o aviso `MEDIÇÃO APROXIMADA`. Esse aviso comunica que o resultado não possui a mesma qualidade espacial de um posicionamento convencional e pode mudar conforme o ARCore refina o mapa do ambiente.

### 📐 Painel de Controles Adaptativo

A inclusão de novos indicadores aumentou a altura do painel e, durante o primeiro teste físico, seu estado expandido cobriu a mira central. Isso prejudicava a relação entre a coordenada visual selecionada e o raio utilizado pelo hit test.

A correção utiliza `BoxWithConstraints` para derivar a altura máxima do painel a partir da viewport. O cartão inteiro é limitado a 30% da altura disponível, enquanto sua coluna interna permanece rolável.

Consequências:
- A mira permanece visível com o painel completamente expandido.
- Uma região da imagem da câmera permanece livre ao redor da mira.
- Todos os controles continuam acessíveis por rolagem.
- Telas com alturas diferentes recebem uma restrição proporcional.
- O ponto visual da mira continua coerente com o centro usado pelo pipeline espacial.

### 📱 Validação em Hardware

O APK foi compilado e instalado em um dispositivo físico com:

```bash
./gradlew compileDebugKotlin installDebug \
  --no-configuration-cache
```

Durante o teste foram confirmados:
- Funcionamento dos botões de posicionamento e confirmação.
- Rolagem do painel de controles.
- Exibição das fontes das âncoras.
- Aviso de medição aproximada.
- Criação de âncoras por `INSTANT_PLACEMENT`.
- Transição para `FEATURE_POINT` quando geometria convencional se tornou válida.
- Permanência da mira central após a correção do painel adaptativo.

O Logcat confirmou a prioridade implementada: resultados convencionais válidos foram selecionados antes do Instant Placement, enquanto o fallback foi utilizado somente quando a geometria convencional estava ausente ou inválida.

### 🛠️ Diagnóstico do Pipeline Nativo

O Logcat registrou explicitamente:

```text
depth_mode: AR_DEPTH_MODE_DISABLED
```

Apesar disso, o runtime do ARCore continuou emitindo mensagens internas relacionadas a `ComputeDisparity` e inicializou componentes auxiliares do `ArDepthCalculator`. Nesta execução, essas mensagens não produziram crash nem ANR e não impediram a criação das âncoras.

Essa observação indica que o SDK pode utilizar processamento interno de profundidade para rastreamento ou Instant Placement mesmo quando a Depth API pública está desabilitada. O comportamento permanece registrado como limitação específica do runtime/dispositivo e não deve ser interpretado como Depth Mode ativo no estado do aplicativo.

As mensagens do Motorola Game Mode tentando localizar o pacote na Play Store foram classificadas como externas ao Aetheris. As mensagens de EGL e MediaPipe observadas depois de `Session::PauseWithAnalytics returning OK` ocorreram durante o encerramento da sessão e não representaram falha funcional durante o teste.

### 📊 Estratégia de Qualidade

Validação final executada:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug \
  --no-configuration-cache
```

Resultado:

```text
BUILD SUCCESSFUL
```

O trabalho foi concluído nos seguintes commits principais:
- `d657d05` - `ci: automate tagged APK releases`
- `6fda6fc` - `feat: expose placement provenance in measurement HUD`

---

## 🏷️ [Dia 13] - 2026-09-02: Procedência Espacial das Âncoras, Integridade Estrutural e Primeira Release Pública

### 🎯 Objetivos Concluídos

- [x] Correção do diretório do workflow de integração contínua de `.github/worklows` para `.github/workflows`, permitindo que o GitHub reconheça corretamente o pipeline Android.
- [x] Restauração de `SpatialLineMath` no source set de produção (`src/main`), eliminando a implementação que havia permanecido incorretamente dentro de `src/test`.
- [x] Substituição do arquivo de teste deslocado por uma suíte real de testes unitários para vértices, ponto médio, magnitude e direção normalizada.
- [x] Limpeza dos artefatos locais do repositório, incluindo caches Kotlin, arquivos do Android Studio e logs de execução.
- [x] Criação do registro de decisões arquiteturais em `docs/adr/README.md`.
- [x] Formalização do `ADR-016`, documentando a fila de posicionamento vinculada ao frame e o fallback por Instant Placement.
- [x] Atualização do `README.md` principal para refletir o estado real da implementação e suas limitações metrológicas.
- [x] Publicação da primeira pré-release pública, `v0.1.0-alpha`, com APK de Debug e checksum SHA-256.
- [x] Criação de `AnchorPlacementSource` para representar explicitamente a origem espacial de cada posicionamento.
- [x] Classificação das fontes de hit test como `PLANE`, `FEATURE_POINT`, `DEPTH_POINT` e `INSTANT_PLACEMENT`.
- [x] Criação de `AnchorPlacement`, associando uma posição tridimensional imutável à sua procedência.
- [x] Evolução de `SpatialFrameData` para transportar a origem das âncoras inicial e final sem remover as propriedades de posição existentes.
- [x] Evolução de `ArCoreHitTestProcessor` para retornar resultados enriquecidos com procedência, preservando as APIs legadas.
- [x] Atualização de `SpatialSensorRepositoryImpl` para armazenar posição e origem de forma consistente durante criação, atualização de pose, pausa, substituição e remoção das âncoras.
- [x] Propagação completa da procedência pelo fluxo `ARCore → Repository → SpatialFrameData → MeasurementViewModel → MeasurementUiState`.
- [x] Inclusão de propriedades derivadas para identificar medições aproximadas, convencionais e estados com procedência completa.
- [x] Preservação da possibilidade de confirmar uma medição aproximada, mantendo sua natureza explicitamente disponível para futura apresentação no HUD.
- [x] Ampliação das suítes de testes de domínio, ARCore, repositório, estado visual e ViewModel.
- [x] Validação integral com testes unitários, Android Lint e montagem do APK de Debug.

### 📐 Modelo de Procedência Espacial

Cada ponto de medição agora pode transportar não apenas sua posição no mundo, mas também a evidência espacial que originou sua âncora:

| Fonte | Classificação | Interpretação |
|---|---|---|
| `PLANE` | Convencional | Pose pertencente ao polígono de um plano rastreado pelo ARCore. |
| `FEATURE_POINT` | Convencional | Ponto visual rastreado com normal de superfície estimada. |
| `DEPTH_POINT` | Convencional | Resultado apoiado pelos dados de profundidade disponíveis. |
| `INSTANT_PLACEMENT` | Aproximada | Posição inicial baseada em distância estimada e sujeita a refinamento posterior. |

Essa classificação evita tratar todos os posicionamentos como equivalentes. O aplicativo passa a possuir informação suficiente para comunicar ao usuário quando uma dimensão contém um ponto aproximado, sem acoplar o domínio às classes nativas do ARCore.

### 🔄 Fluxo Implementado

1. `ArCoreHitTestProcessor` seleciona o melhor resultado disponível, priorizando geometria convencional.
2. O tipo nativo do objeto rastreado é convertido em `AnchorPlacementSource`.
3. `SpatialSensorRepositoryImpl` mantém a âncora nativa e sua origem como uma unidade lógica.
4. `SpatialFrameData` publica posições e procedências pelo mesmo estado imutável.
5. `MeasurementViewModel` transfere os valores de forma atômica para a apresentação.
6. `MeasurementUiState` deriva se a medição atual é convencional ou contém Instant Placement.
7. Operações de confirmação, descarte e reset removem conjuntamente pontos e procedências.

### 🛡️ Compatibilidade e Invariantes

- As APIs anteriores que retornam apenas `Point3D` ou `Anchor` foram preservadas para evitar uma migração abrupta dos consumidores.
- Um ponto ainda pode existir temporariamente sem procedência, mantendo compatibilidade com estados e testes anteriores.
- Uma procedência nunca pode existir sem o ponto correspondente.
- Mudanças de pose não alteram a fonte original da âncora.
- O estado `PAUSED` preserva a última posição conhecida e sua procedência.
- O estado `STOPPED`, a substituição, o rollback e a limpeza removem posição e origem de forma consistente.
- A distância somente é recalculada quando as posições mudam; uma atualização isolada da procedência não provoca cálculo métrico desnecessário.

### 📊 Estratégia de Qualidade

Os novos testes verificam:
- Propriedades e classificação de `AnchorPlacementSource`.
- Associação imutável entre posição e fonte em `AnchorPlacement`.
- Invariantes de `SpatialFrameData` e `MeasurementUiState`.
- Classificação de planos, feature points, depth points e Instant Placement.
- Prioridade dos resultados convencionais sobre o fallback aproximado.
- Preservação da fonte durante atualizações e pausas das âncoras.
- Remoção da procedência durante substituição, encerramento, descarte e reset.
- Propagação ponta a ponta entre repositório, ViewModel e estado visual.
- Regressão dos fluxos existentes de distância, dimensões, volume, material e massa.

Pipeline final executado:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug \
  --no-configuration-cache
```

Resultado:

```text
BUILD SUCCESSFUL in 15s
53 actionable tasks: 12 executed, 41 up-to-date
```

### 🏛️ Resultado Arquitetural

Aetheris agora diferencia a existência de uma coordenada espacial da qualidade do método que a produziu. Essa separação prepara o aplicativo para:
- Exibir avisos de posicionamento aproximado no HUD.
- Impedir que resultados provisórios sejam apresentados como medições convencionais.
- Registrar procedência em futuras exportações JSON e CSV.
- Comparar estabilidade e precisão por tipo de hit test.
- Recalcular indicadores quando um Instant Placement for refinado.
- Estabelecer políticas futuras de aceitação e confiança por fonte espacial.

### 📦 Controle de Versão Relacionado

- `481fd9f` — restauração de `SpatialLineMath` nas fontes de produção.
- `9ffdc67` — exclusão de artefatos locais do controle de versão.
- `689cb29` — criação do registro de ADRs.
- `2ff9b91` — documentação do posicionamento vinculado ao frame.
- `be8b604` — inclusão do ADR-016 no registro.
- `7190a69` — alinhamento do README com a implementação atual.
- `af35e50` — classificação das fontes de hit test e criação de âncoras.
- `v0.1.0-alpha` — primeira pré-release pública do APK.

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

### 🏛️ Decisões de Arquitetura

- **ADR-015: Sequential Axis Capture and Uncertainty-Aware AABB Volume**
  - **Contexto:** A medição de apenas uma distância não representa as dimensões espaciais necessárias para estimar volume e massa.
  - **Decisão:** Capturar largura, altura e profundidade como medições lineares independentes, associar cada uma a um eixo explícito, liberar as âncoras entre etapas e calcular uma estimativa volumétrica AABB com propagação das incertezas.

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

### 🏛️ Decisões de Arquitetura (ADR)

- **ADR-014: Defensive ARCore/OpenGL Resource Management and Depth Fallback**
  - **Contexto:** O pipeline combina objetos nativos de vida curta (`Frame`, `PointCloud`, `Anchor`), recursos de GPU dependentes do contexto EGL e funcionalidades opcionais que podem falhar no hardware.
  - **Decisão:** Tratar indisponibilidades transitórias nas bordas da camada `data`, garantir liberação determinística dos recursos, manter o domínio livre de dependências Android e permitir fallback explícito da Depth API sem interromper o fluxo central.

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

## 🔮 Próximos Passos Definidos para o Dia 15

- [ ] Preservar a procedência das âncoras junto a cada eixo depois da confirmação da dimensão.
- [ ] Modelar a qualidade da medição confirmada sem acoplar o domínio às classes do ARCore.
- [ ] Definir uma política explícita para aceitar, repetir ou sinalizar dimensões aproximadas.
- [ ] Preparar dimensões, incertezas e procedência para futura persistência e exportação.
- [ ] Validar o workflow automatizado com a próxima tag de pré-release.
- [ ] Continuar observando as mensagens nativas de `ComputeDisparity` em diferentes aparelhos e versões do Google Play Services for AR.
- [ ] Repetir testes físicos comparando medidas convencionais e aproximadas contra objetos de dimensões conhecidas.