# Aetheris — Diário de Desenvolvimento (DEVLOG)

Registro contínuo da engenharia, decisões arquiteturais (ADRs), modelagem matemática e evolução do ecossistema Aetheris.

---

## [Dia 13] — 2026-09-02: Procedência Espacial das Âncoras, Integridade Estrutural e Primeira Release Pública

### Objetivos concluídos

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

### Modelo de procedência espacial

Cada ponto de medição agora pode transportar não apenas sua posição no mundo, mas também a evidência espacial que originou sua âncora:

| Fonte | Classificação | Interpretação |
|---|---|---|
| `PLANE` | Convencional | Pose pertencente ao polígono de um plano rastreado pelo ARCore. |
| `FEATURE_POINT` | Convencional | Ponto visual rastreado com normal de superfície estimada. |
| `DEPTH_POINT` | Convencional | Resultado apoiado pelos dados de profundidade disponíveis. |
| `INSTANT_PLACEMENT` | Aproximada | Posição inicial baseada em distância estimada e sujeita a refinamento posterior. |

Essa classificação evita tratar todos os posicionamentos como equivalentes. O aplicativo passa a possuir informação suficiente para comunicar ao usuário quando uma dimensão contém um ponto aproximado, sem acoplar o domínio às classes nativas do ARCore.

### Fluxo implementado

1. `ArCoreHitTestProcessor` seleciona o melhor resultado disponível, priorizando geometria convencional.
2. O tipo nativo do objeto rastreado é convertido em `AnchorPlacementSource`.
3. `SpatialSensorRepositoryImpl` mantém a âncora nativa e sua origem como uma unidade lógica.
4. `SpatialFrameData` publica posições e procedências pelo mesmo estado imutável.
5. `MeasurementViewModel` transfere os valores de forma atômica para a apresentação.
6. `MeasurementUiState` deriva se a medição atual é convencional ou contém Instant Placement.
7. Operações de confirmação, descarte e reset removem conjuntamente pontos e procedências.

### Compatibilidade e invariantes

- As APIs anteriores que retornam apenas `Point3D` ou `Anchor` foram preservadas para evitar uma migração abrupta dos consumidores.
- Um ponto ainda pode existir temporariamente sem procedência, mantendo compatibilidade com estados e testes anteriores.
- Uma procedência nunca pode existir sem o ponto correspondente.
- Mudanças de pose não alteram a fonte original da âncora.
- O estado `PAUSED` preserva a última posição conhecida e sua procedência.
- O estado `STOPPED`, a substituição, o rollback e a limpeza removem posição e origem de forma consistente.
- A distância somente é recalculada quando as posições mudam; uma atualização isolada da procedência não provoca cálculo métrico desnecessário.

### Estratégia de qualidade

Os novos testes verificam:

- propriedades e classificação de `AnchorPlacementSource`;
- associação imutável entre posição e fonte em `AnchorPlacement`;
- invariantes de `SpatialFrameData` e `MeasurementUiState`;
- classificação de planos, feature points, depth points e Instant Placement;
- prioridade dos resultados convencionais sobre o fallback aproximado;
- preservação da fonte durante atualizações e pausas das âncoras;
- remoção da procedência durante substituição, encerramento, descarte e reset;
- propagação ponta a ponta entre repositório, ViewModel e estado visual;
- regressão dos fluxos existentes de distância, dimensões, volume, material e massa.

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

### Resultado arquitetural

Aetheris agora diferencia a existência de uma coordenada espacial da qualidade do método que a produziu. Essa separação prepara o aplicativo para:

- exibir avisos de posicionamento aproximado no HUD;
- impedir que resultados provisórios sejam apresentados como medições convencionais;
- registrar procedência em futuras exportações JSON e CSV;
- comparar estabilidade e precisão por tipo de hit test;
- recalcular indicadores quando um Instant Placement for refinado;
- estabelecer políticas futuras de aceitação e confiança por fonte espacial.

### Controle de versão relacionado

- `481fd9f` — restauração de `SpatialLineMath` nas fontes de produção.
- `9ffdc67` — exclusão de artefatos locais do controle de versão.
- `689cb29` — criação do registro de ADRs.
- `2ff9b91` — documentação do posicionamento vinculado ao frame.
- `be8b604` — inclusão do ADR-016 no registro.
- `7190a69` — alinhamento do README com a implementação atual.
- `af35e50` — classificação das fontes de hit test e criação de âncoras.
- `v0.1.0-alpha` — primeira pré-release pública do APK.

---

## ? [Dia 12] - 2026-09-01: Estimativa de Massa por Densidade, Threading OpenGL e Valida��o Ponta a Ponta em Hardware

### ? Objetivos Conclu�dos

- [x] Cria��o do cat�logo de materiais e densidades f�sicas (`MaterialDensity`) abrangendo s�lidos comuns (madeira, alum�nio, a�o, vidro, concreto, pl�sticos).
- [x] Modelagem da entidade `MassEstimate` contendo massa calculada em quilogramas e gramas, margem de incerteza propagada ($\pm\sigma$) e incerteza relativa percentual.
- [x] Implementa��o do caso de uso `EstimateMassUseCase` integrando volume tridimensional e densidade volum�trica com propaga��o rigorosa de incerteza combinada.
- [x] Diagn�stico e resolu��o de conten��o de concorr�ncia e race condition entre a Main Thread/Coroutines e a `GLThread` (render thread) do ARCore.
- [x] Implementa��o de fila de opera��es orientada ao frame (`Frame-Affine Placement Queue`) em `SpatialSensorRepositoryImpl`, executando raycasting e cria��o de �ncoras sincronizadas com o frame de renderiza��o mais recente.
- [x] Habilita��o de `Instant Placement` (`Config.InstantPlacementMode.LOCAL_Y_UP`) como fallback determin�stico para posicionamento imediato de pontos em superf�cies com baixa densidade de planos.
- [x] Implementa��o de throttling na sondagem de superf�cies (`hasValidSurfaceAt`) fixado em 5 Hz (200 ms), reduzindo substancialmente o overhead de CPU e consumo de bateria.
- [x] Inclus�o de telemetria diagn�stica isolada para builds de Debug sem impactar a performance em Release.
- [x] Atualiza��o completa da su�te de testes unit�rios (`SpatialSensorRepositoryTest`, `MeasurementViewModelTest`, etc.) totalizando **128/128 testes unit�rios aprovados na JVM**.
- [x] Valida��o funcional ponta a ponta em hardware real (Motorola Edge 50 Fusion): toque na tela $\to$ ancoragem espacial $\to$ medi��o linear $\to$ c�lculo volum�trico $\to$ estimativa de massa.
- [x] Registro formal da decis�o arquitetural no `ADR-016`.
- [x] Publica��o das altera��es nos commits `a92b40b`, `a72eed1` e `b18502c`.

### ? Modelagem Matem�tica

A estimativa de massa � obtida a partir do produto do volume tridimensional $V$ pela densidade volum�trica do material $\rho$:

$$M = V \times \rho$$

onde:
- $V$ representa o volume estimado da caixa delimitadora (AABB) em $\text{m}^3$;
- $\rho$ representa a densidade do material em $\text{kg/m}^3$.

A propaga��o da incerteza combinada $u_M$ considera a incerteza volum�trica $u_V$ e a incerteza intr�nseca da densidade do material $u_\rho$:

$$u_M = \sqrt{(\rho \times u_V)^2 + (V \times u_\rho)^2}$$

Essa modelagem assegura que a incerteza final reflita tanto a precis�o do sensor espacial quanto a toler�ncia na composi��o f�sica do material selecionado.

### ?? Desafios de Engenharia & Diagn�stico em Hardware

1. **Concorr�ncia entre Coroutines e a GLThread do ARCore:**
- *Causa:* Chamadas ass�ncronas para `createAnchor` ou `performHitTest` executadas a partir de dispatchers de Coroutine tentavam acessar o estado nativo da `Session` enquanto a thread OpenGL estava no meio do ciclo de renderiza��o de frame, gerando exce��es nativas transit�rias e falha silenciosa na fixa��o de pontos.
- *Solu��o:* Cria��o de uma fila de comandos at�mica (`ConcurrentLinkedQueue`) no reposit�rio, consumida de forma s�ncrona dentro de `onFrameUpdate(frame)` na `GLThread`, garantindo que todas as intera��es com o ARCore ocorram estritamente no frame ativo.
2. **Superf�cies Pouco Texturizadas e Falha de Planos Convencionais:**
- *Causa:* Ambientes internos homog�neos demoravam v�rios segundos para consolidar planos poligonais, bloqueando o usu�rio de fixar o Ponto A ou B.
- *Solu��o:* Ativa��o do modo `Instant Placement` com fallback em cascata: plano delimitado $\to$ ponto ToF/Depth $\to$ ponto Instant Placement (`InstantPlacementPoint`).
3. **Sobrecarga de Raycasting no Ret�culo (Crosshair):**
- *Causa:* A sondagem de superf�cie para feedback crom�tico do ret�culo executava a 60 FPS, consumindo ciclos desnecess�rios de CPU.
- *Solu��o:* Aplica��o de throttling por timestamp garantindo intervalo m�nimo de 200 ms (5 Hz) entre as verifica��es de superf�cie.

### ? M�tricas de Valida��o

- **Su�te de Testes Unit�rios:** **128 testes executados, 0 falhas ? BUILD SUCCESSFUL**.
- **Comando de valida��o:** `./gradlew testDebugUnitTest lintDebug assembleDebug --no-configuration-cache`.
- **Valida��o em Hardware:** Posicionamento de �ncoras verificado com taxa de sucesso de 100% em m�ltiplos testes de ilumina��o e dist�ncia no Motorola Edge 50 Fusion.

### ?? Decis�es de Arquitetura (ADR)

- **ADR-016: Frame-Affine Placement Queue & Instant Placement Fallback**
  - **Contexto:** A cria��o de �ncoras e o raycasting requerem sincronia absoluta com o frame ativo na thread gr�fica, e a depend�ncia exclusiva de planos poligonais tornava a inicializa��o lenta em superf�cies lisas.
  - **Decis�o:** Enfileirar requisi��es de ancoragem para execu��o direta no frame ativo da `GLThread` e habilitar o `Instant Placement` como fallback imediato, mantendo o dom�nio desacoplado via contratos reativos.

---

## ? [Dia 11] - 2026-08-31: Resolu��o Expl�cita do Koin no N�vel da Activity

### ? Objetivo Conclu�do

- [x] Refatora��o isolada do arquivo `MainActivity.kt` para remover a depend�ncia de um contexto Koin adicional dentro da �rvore do Jetpack Compose.
- [x] Resolu��o de `MeasurementViewModel` diretamente no ciclo de vida da `ComponentActivity` por meio do delegate `by viewModel()`.
- [x] Resolu��o do singleton `ArCoreSessionManager` diretamente na Activity por meio do delegate `by inject()`.
- [x] Passagem expl�cita das depend�ncias para `MeasurementScreen`.
- [x] Remo��o do wrapper `KoinAndroidContext` da composi��o principal.
- [x] Preserva��o da inicializa��o global do Koin realizada pelo `AetherisApplication`.
- [x] Valida��o completa dos testes unit�rios, Android Lint e montagem do APK de Debug.
- [x] Publica��o da altera��o no commit `5352403` (`fix: resolve Koin dependencies at activity level`).
- [x] Sincroniza��o bem-sucedida entre `HEAD`, `main` e `origin/main`, com a �rvore de trabalho limpa.

### ? Escopo do Dia

O trabalho foi intencionalmente limitado a um �nico arquivo de produ��o:

```text
app/src/main/java/org/aetheris/app/MainActivity.kt
```

A altera��o teve como objetivo tratar o aviso observado durante a execu��o no dispositivo:

```text
No Koin context defined in Compose, fallback to default Koin context.
```

O fallback funcionava corretamente porque o cont�iner global j� era iniciado em `AetherisApplication`, mas a resolu��o impl�cita dentro do Compose gerava uma mensagem desnecess�ria no Logcat.

### ?? Solu��o Aplicada

Antes da refatora��o, a `MeasurementScreen` resolvia suas depend�ncias por par�metros padr�o durante a composi��o:

```kotlin
MeasurementScreen()
```

A `MainActivity` tamb�m envolvia a interface com um contexto Compose adicional:

```kotlin
KoinAndroidContext {
    MeasurementScreen()
}
```

Depois da refatora��o, a Activity passou a possuir explicitamente as depend�ncias associadas ao seu ciclo de vida:

```kotlin
private val measurementViewModel:
    MeasurementViewModel by viewModel()

private val arCoreSessionManager:
    ArCoreSessionManager by inject()
```

Essas inst�ncias s�o fornecidas diretamente � tela:

```kotlin
MeasurementScreen(
    viewModel = measurementViewModel,
    sessionManager = arCoreSessionManager
)
```

### ?? Impacto Arquitetural

1. **Ciclo de vida expl�cito do ViewModel:**
- `MeasurementViewModel` permanece associado � `MainActivity`.
- Mudan�as de configura��o continuam utilizando o gerenciamento padr�o de ViewModel do Android.

2. **Singleton da sess�o ARCore preservado:**
- `ArCoreSessionManager` continua sendo fornecido pela mesma defini��o `single` do m�dulo Koin.
- Nenhuma segunda sess�o ARCore � criada pela refatora��o.

3. **Composi��o mais simples:**
- A �rvore Compose recebe depend�ncias prontas.
- A tela permanece test�vel porque seus par�metros continuam podendo ser substitu�dos.

4. **Inicializa��o centralizada:**
- `AetherisApplication` continua sendo o �nico ponto respons�vel por chamar `startKoin` e registrar `appModule`.
- N�o existe um segundo cont�iner de depend�ncias controlado pela composi��o.

### ? Valida��o

Pipeline utilizado:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug \
  --no-configuration-cache
```

Resultado:

```text
BUILD SUCCESSFUL
```

A altera��o n�o modificou regras de dom�nio, c�lculos m�tricos, contratos de reposit�rio, renderiza��o OpenGL ou estado de medi��o. Por isso, a su�te existente foi utilizada como teste de regress�o integral.

### ? Controle de Vers�o

```text
Commit: 5352403
Mensagem: fix: resolve Koin dependencies at activity level
Branch: main
Remoto: origin/main
Status final: working tree clean
```

---

## ? [Dia 10] - 2026-08-30: Medi��o Tridimensional Sequencial, Volume com Incerteza e Valida��o em Hardware

### ? Objetivos Conclu�dos

- [x] Cria��o do modelo `DimensionAxis` para representar e ordenar os tr�s eixos espaciais: `WIDTH`, `HEIGHT` e `DEPTH`.
- [x] Implementa��o de `SpatialDimensions` como estado imut�vel das medi��es confirmadas de largura, altura e profundidade.
- [x] Inclus�o de propriedades derivadas para contagem de eixos medidos, identifica��o do pr�ximo eixo pendente e verifica��o de conclus�o da medi��o tridimensional.
- [x] Cria��o do modelo `VolumeMeasurement`, representando volume em metros c�bicos e litros, margem de incerteza, limites m�nimo e m�ximo e incerteza relativa percentual.
- [x] Implementa��o de formata��o m�trica autom�tica para apresenta��o em litros ou metros c�bicos.
- [x] Cria��o do `CalculateVolumeUseCase` para calcular o volume aproximado da caixa delimitadora espacial a partir de tr�s medi��es lineares.
- [x] Implementa��o da propaga��o independente das incertezas de largura, altura e profundidade no resultado volum�trico.
- [x] Refatora��o do `MeasurementUiState` para armazenar dimens�es confirmadas, volume calculado, eixo atual e progresso completo da captura tridimensional.
- [x] Refatora��o do `MeasurementViewModel` para coordenar o fluxo sequencial `WIDTH ? HEIGHT ? DEPTH`.
- [x] Implementa��o da confirma��o individual de cada eixo, com limpeza das �ncoras entre as etapas e preserva��o das dimens�es j� registradas.
- [x] C�lculo autom�tico do volume ap�s a confirma��o da profundidade.
- [x] Implementa��o de reset parcial da dimens�o atual e reset completo da medi��o tridimensional.
- [x] Registro de `CalculateVolumeUseCase` no m�dulo Koin e atualiza��o da inje��o do `MeasurementViewModel`.
- [x] Refatora��o completa da `MeasurementScreen` com indicadores de progresso para largura, altura e profundidade.
- [x] Inclus�o de controles contextuais para fixar pontos, confirmar dimens�es, refazer o eixo atual e reiniciar todo o processo.
- [x] Inclus�o do painel final de volume com incerteza e resumo das tr�s dimens�es confirmadas.
- [x] Cria��o e amplia��o das su�tes de testes de dom�nio, casos de uso, estado de interface e ViewModel.
- [x] Valida��o integral por testes unit�rios, Android Lint e montagem do APK de Debug.
- [x] Publica��o da funcionalidade no commit `5a07c5c` (`feat: add three-axis spatial volume measurement`).

### ? Modelagem Matem�tica

O volume aproximado � calculado como uma caixa delimitadora tridimensional:

$$V = w \times h \times d$$

onde:
- $w$ representa a largura;
- $h$ representa a altura;
- $d$ representa a profundidade.

A incerteza volum�trica � propagada considerando as incertezas independentes dos tr�s eixos:

$$u_V = \sqrt{(h \times d \times u_w)^2 + (w \times d \times u_h)^2 + (w \times h \times u_d)^2}$$

Essa formula��o evita divis�es por zero e continua v�lida quando uma das dimens�es medidas � igual a zero.

O resultado � apresentado como uma estimativa geom�trica da caixa delimitadora do objeto, n�o como seu volume f�sico exato. Objetos com formas irregulares exigir�o segmenta��o espacial e reconstru��o geom�trica em etapas futuras.

### ?? Decis�es de Arquitetura

1. **Sequenciamento expl�cito dos eixos:**
- O dom�nio define a ordem `WIDTH ? HEIGHT ? DEPTH` sem depender da interface Android.
- O pr�ximo eixo � derivado das dimens�es ainda ausentes, reduzindo estados inconsistentes.

2. **Estado imut�vel das dimens�es:**
- Cada confirma��o produz uma nova inst�ncia de `SpatialDimensions`.
- Medi��es anteriores s�o preservadas enquanto as �ncoras do eixo atual s�o liberadas.

3. **Separa��o entre dist�ncia e volume:**
- `DistanceMeasurement` representa medi��es lineares.
- `VolumeMeasurement` representa o resultado volum�trico e sua incerteza.
- `CalculateVolumeUseCase` concentra a regra matem�tica sem depend�ncias do Android ou ARCore.

4. **Orquestra��o no ViewModel:**
- A interface apenas emite eventos de posicionamento, confirma��o, repeti��o e reset.
- O `MeasurementViewModel` controla a transi��o entre eixos e o c�lculo final.

5. **Volume como aproxima��o AABB:**
- O primeiro est�gio usa uma caixa delimitadora formada por largura, altura e profundidade.
- A decis�o mant�m o fluxo test�vel e prepara a arquitetura para futura segmenta��o de objetos e nuvens de pontos.

### ? Cobertura e Valida��o

Foram adicionados ou ampliados testes para:
- ordem e transi��o dos valores de `DimensionAxis`;
- imutabilidade e progress�o de `SpatialDimensions`;
- convers�o entre metros c�bicos e litros;
- valida��o de volumes e incertezas n�o negativos e finitos;
- limites m�nimo e m�ximo de `VolumeMeasurement`;
- propaga��o matem�tica das incertezas dos tr�s eixos;
- comportamento com dimens�es ou volume iguais a zero;
- rejei��o de conjuntos incompletos de dimens�es;
- progress�o de largura para altura e profundidade no estado visual;
- confirma��o sequencial dos tr�s eixos pelo ViewModel;
- limpeza das �ncoras entre dimens�es;
- c�lculo autom�tico do volume ao concluir a profundidade;
- reset parcial e completo da medi��o.

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

### ? Valida��o Inicial no Dispositivo

O APK foi executado em um aparelho f�sico com ARCore. A sess�o:
- iniciou corretamente;
- carregou todas as depend�ncias do Koin;
- processou aproximadamente 1.216 frames da c�mera;
- encerrou com `Session::PauseWithAnalytics returning OK`;
- n�o apresentou `FATAL EXCEPTION`, ANR ou crash do processo;
- n�o apresentou exce��es Kotlin relacionadas �s novas dimens�es ou ao c�lculo de volume.

O teste funcional completo n�o p�de ser conclu�do devido � baixa luminosidade do ambiente. O ARCore registrou dificuldade para encontrar pontos visuais consistentes e refinar planos f�sicos:
- 246 ocorr�ncias internas de refinamento de plano sem inliers suficientes;
- 5 ocorr�ncias internas de `ComputeDisparity` no servi�o nativo do ARCore;
- uma ocorr�ncia de extra��o de caracter�sticas acima do tempo esperado;
- atraso percept�vel na inicializa��o e no encerramento da sess�o.

Apesar dessas mensagens nativas, n�o houve encerramento anormal. A valida��o funcional ser� repetida em ambiente bem iluminado e com superf�cies texturizadas antes de novas altera��es no pipeline de medi��o.

### ?? Diagn�stico T�cnico

1. **Baixa luminosidade e poucos marcos visuais:**
- *Efeito:* dificuldade para estabilizar planos e habilitar a mira de posicionamento.
- *A��o definida:* repetir o teste com ilumina��o uniforme, movimento lento da c�mera e superf�cies com textura.
2. **Mensagens internas de `ComputeDisparity`:**
- *Observa��o:* continuaram presentes no Google Play Services for AR mesmo com `DepthMode.DISABLED` na configura��o p�blica da sess�o.
- *Decis�o:* manter o fallback sem Depth API e n�o alterar o dom�nio ou o ViewModel com base apenas em mensagens internas do servi�o nativo.
3. **Aviso de contexto Compose do Koin:**
- *Efeito:* o Koin utilizou corretamente o contexto global iniciado pelo `AetherisApplication`.
- *Prioridade:* baixa; n�o afetou a resolu��o das depend�ncias nem o funcionamento do aplicativo.
4. **Encerramento OpenGL/ARCore:**
- *Observa��o:* ocorreu uma mensagem isolada de chamada OpenGL sem contexto corrente durante a desmontagem.
- *Resultado:* a sess�o retornou `OK` e o processo encerrou normalmente.
- *A��o definida:* repetir ciclos de abrir, minimizar, restaurar e fechar o aplicativo para verificar recorr�ncia.

### ?? Decis�o de Arquitetura (ADR)

- **ADR-015: Sequential Axis Capture and Uncertainty-Aware AABB Volume**
  - **Contexto:** A medi��o de apenas uma dist�ncia n�o representa as dimens�es espaciais necess�rias para estimar volume e, futuramente, massa por densidade.
  - **Decis�o:** Capturar largura, altura e profundidade como medi��es lineares independentes, associar cada uma a um eixo expl�cito, liberar as �ncoras entre etapas e calcular uma estimativa volum�trica AABB com propaga��o das incertezas.
  - **Consequ�ncia:** O dom�nio permanece puro e test�vel, enquanto a apresenta��o ganha um fluxo progressivo capaz de evoluir posteriormente para segmenta��o autom�tica, reconstru��o 3D e modelos f�sicos espec�ficos por material.

---

## ?? [Dia 09] - 2026-08-29: Hardening do Pipeline ARCore, Seguran�a Num�rica e Regress�o Completa

### ? Objetivos Conclu�dos

- [x] Migra��o definitiva do estado espacial legado para `SpatialFrameData`, com atualiza��o do contrato `SpatialSensorRepository`, dos reposit�rios falsos e dos testes do `MeasurementViewModel`.
- [x] Corre��o da su�te de testes do ARCore, incluindo os mocks de `Frame.camera`, `TrackingState`, `Point.orientationMode`, `Plane.isPoseInPolygon` e cria��o de �ncoras.
- [x] Adequa��o do ciclo de vida de `PointCloud` ao contrato `AutoCloseable`, garantindo `pointCloud.close()` por meio de `use`, inclusive quando a leitura do buffer falha.
- [x] Estabiliza��o do `SpatialSensorRepositoryImpl` com viewport atualizada atomicamente, convers�o segura de coordenadas normalizadas para pixels e libera��o defensiva de �ncoras.
- [x] Tratamento dos estados `TRACKING`, `PAUSED` e `STOPPED` das �ncoras, preservando a �ltima posi��o conhecida durante pausas tempor�rias e removendo �ncoras encerradas.
- [x] Refor�o do `ArCoreHitTestProcessor` para validar c�mera, coordenadas, planos, pontos orientados e `DepthPoint`, retornando `null` diante de indisponibilidade transit�ria do pipeline nativo.
- [x] Refor�o do `ArCoreFrameProcessor` com valida��o de confian�a e coordenadas finitas, al�m de tratamento para `DeadlineExceededException`, `NotYetAvailableException` e `ResourceExhaustedException`.
- [x] Refatora��o dos modelos `Point3D`, `BoundingBox3D`, `DistanceMeasurement`, `MassEstimate`, `ScreenPoint2D`, `SpatialFrameData` e `TrackingStatus`.
- [x] Ado��o de c�lculos intermedi�rios em precis�o `Double` para dist�ncia, normaliza��o e ponto m�dio, reduzindo riscos de overflow e perda num�rica.
- [x] Consolida��o de `ProjectWorldToScreenUseCase`, `CalculateDistanceUseCase`, `EstimateSpatialDimensionsUseCase` e `SpatialLineMath` sem depend�ncias Android na camada de dom�nio.
- [x] Hardening dos renderizadores `BackgroundRenderer` e `SpatialLineRenderer`, com valida��o de matrizes, preven��o de cria��o duplicada, limpeza de shaders e libera��o segura de VAO, VBO, programas e texturas.
- [x] Simplifica��o da inicializa��o do Koin: `AetherisApplication` mant�m o contexto global e `MainActivity` utiliza a inje��o automaticamente, aplicando `AetherisTheme`.
- [x] Desativa��o intencional de `Config.DepthMode.AUTOMATIC` ap�s o diagn�stico de falha nativa em `ComputeDisparity`, preservando planos, hit tests, �ncoras e point cloud.
- [x] Estabelecimento de uma baseline verde com **32/32 testes unit�rios aprovados** por `testDebugUnitTest`.

### ?? Desafios de Engenharia & Diagn�stico

1. **Evolu��o incompat�vel do contrato espacial:**
- *Causa:* Os testes ainda utilizavam `SpatialData`, enquanto a produ��o j� expunha `StateFlow<SpatialFrameData>` e novos m�todos de hit test e ancoragem.
- *Solu��o:* Atualiza��o dos doubles de teste, assinaturas e propriedades observadas, mantendo `normalizedX` e `normalizedY` compat�veis com a interface.
2. **Mocks incompletos das classes ARCore:**
- *Causa:* O processador passou a validar `frame.camera.trackingState` e `Point.orientationMode`, mas os mocks n�o forneciam esses comportamentos e geravam `MockKException`.
- *Solu��o:* Modelagem expl�cita do estado da c�mera e do modo `ESTIMATED_SURFACE_NORMAL`, preservando as valida��es usadas em produ��o.
3. **Libera��o incorreta de `PointCloud` nos testes:**
- *Causa:* A implementa��o utiliza `use`, que encerra o recurso por `close()`, enquanto os testes verificavam a chamada antiga a `release()`.
- *Solu��o:* Atualiza��o dos testes para verificar exatamente uma chamada a `pointCloud.close()`, inclusive nos fluxos excepcionais.
4. **Falha nativa da Depth API no dispositivo:**
- *Causa:* Embora o aparelho anunciasse suporte a `DepthMode.AUTOMATIC`, o pipeline apresentava falha interna em `ComputeDisparity`.
- *Solu��o:* Manuten��o da detec��o de suporte como telemetria e configura��o efetiva de `DepthMode.DISABLED`, evitando instabilidade sem remover as fun��es centrais de medi��o.
5. **Gerenciamento defensivo de recursos OpenGL e ARCore:**
- *Causa:* Falhas durante compila��o de shaders, vincula��o de programas, cria��o de buffers ou encerramento de �ncoras poderiam deixar recursos parcialmente inicializados.
- *Solu��o:* Rotinas idempotentes de destrui��o, restaura��o de bindings em blocos `finally`, valida��o de handles e encapsulamento de `Anchor.detach()`.

### ? M�tricas de Valida��o

- **Regress�o inicial:** 18 falhas em 32 testes ap�s a evolu��o dos contratos.
- **Primeira estabiliza��o:** redu��o para 13 falhas, concentradas nos mocks ARCore e no reposit�rio.
- **Segunda estabiliza��o:** redu��o para 3 falhas, todas no `SpatialSensorRepositoryTest`.
- **Resultado final registrado:** **32 testes executados, 0 falhas ? BUILD SUCCESSFUL**.
- **Comando de valida��o:** `./gradlew testDebugUnitTest --no-configuration-cache`.

### ?? Decis�es de Arquitetura (ADR)

- **ADR-014: Defensive ARCore/OpenGL Resource Management and Depth Fallback**
  - **Contexto:** O pipeline combina objetos nativos de vida curta (`Frame`, `PointCloud`, `Anchor`), recursos de GPU dependentes do contexto EGL e funcionalidades opcionais que podem falhar mesmo quando declaradas como suportadas pelo hardware.
  - **Decis�o:** Tratar indisponibilidades transit�rias nas bordas da camada `data`, garantir libera��o determin�stica dos recursos, manter o dom�nio livre de depend�ncias Android e permitir fallback expl�cito da Depth API sem interromper planos, hit tests e ancoragem.

---

## ? [Dia 08] - 2026-08-29: Proje��o World-to-Screen, Badge Flutuante em Compose e Ancoragem Anti-Drift (ARCore Anchor)

### ? Objetivos Conclu�dos

- [x] Implementa��o do caso de uso `ProjectWorldToScreenUseCase` realizando a transforma��o projetiva completa ($3\text{D} \to 2\text{D}$): coordenadas de mundo $\to$ clip space ($M_{proj} \times M_{view}$) $\to$ coordenadas normalizadas de dispositivo (NDC) $\to$ espa�o de tela em pixels.
- [x] Adi��o de guarda de *Frustum Clipping* ($w_c \le 0$) para ocultar instantaneamente o badge quando o vetor de medi��o estiver atr�s do plano da c�mera, evitando divis�o por zero e artefatos de proje��o invertida.
- [x] Renderiza��o da etiqueta flutuante reativa (`FloatingMeasurementBadge`) em Jetpack Compose, acompanhando o ponto m�dio do vetor espacial com leitura de dist�ncia e incerteza ($\pm\sigma$) em tempo real.
- [x] Modelagem de n�s espaciais (`AnchorSlot.START`, `AnchorSlot.END`) no dom�nio e extens�o de `SpatialFrameData`.
- [x] Implementa��o de `createAnchorAt` em `ArCoreHitTestProcessor` com suporte a planos poligonais e pontos ToF/Depth.
- [x] Gerenciamento determin�stico do ciclo de vida nativo de �ncoras (`createAnchor`, `detach`) no `SpatialSensorRepositoryImpl`, corrigindo automaticamente as coordenadas $(tx, ty, tz)$ a cada otimiza��o do grafo de poses do SLAM.
- [x] Reatividade no `MeasurementViewModel` propagando medi��es corrigidas continuamente sem provocar *GC churn*.
- [x] Resolu��o de conflito estrutural de *Class Shadowing* no source set de testes e consolida��o de **25/25 testes unit�rios na JVM** passando com MockK e Google Truth.
- [x] Registro da decis�o arquitetural formal no `ADR-013`.

### ?? Desafios de Engenharia & Diagn�stico em Hardware

1. **Class Shadowing no Source Set de Testes:**
- *Causa:* O arquivo `ArCoreHitTestProcessorTest.kt` continha uma declara��o acidental de `class ArCoreHitTestProcessor` no diret�rio `src/test/`, mascarando a classe real de produ��o em `src/main/` e impedindo a resolu��o de novos m�todos durante a compila��o de testes unit�rios.
- *Solu��o:* Substitui��o do stub por uma su�te de testes unit�rios leg�tima cobrindo cria��o de �ncoras, hit-testing de planos e valida��o de superf�cies.
2. **Deriva��o M�trica Espacial (Drift em Medi��es Longas):**
- *Causa:* Coordenadas euclidianas est�ticas $(X, Y, Z)$ salvas no primeiro frame sofriam descolamento visual quando o otimizador SLAM/BA do ARCore recalculava a origem do mundo durante a movimenta��o do usu�rio.
- *Solu��o:* Vincula��o dos n�s a objetos nativos `com.google.ar.core.Anchor` com consulta din�mica da `Pose` a cada ciclo de `updateFrameData`.
3. **Frustum Culling de Elementos 2D:**
- *Causa:* Proje��es matem�ticas convencionais sem valida��o de $w_c$ geravam posi��es de tela espelhadas quando o usu�rio virava de costas para o objeto medido.
- *Solu��o:* Retorno determin�stico de `null` no caso de uso caso $w_c \le 0.001\text{f}$, instruindo o Compose a n�o desenhar o badge fora do cone de vis�o da c�mera.

### ? M�tricas de Valida��o no Dispositivo (Motorola Edge 50 Fusion)

- **Converg�ncia VIO (Visual-Inertial Odometry):** Inicializa��o recorde atingindo `VIO_TRACKING` em apenas **398,05 ms** (redu��o de 9,7% em rela��o aos 441 ms do Dia 07).
- **Consist�ncia Geom�trica do SLAM:** Otimiza��o de mapa (`MAP SOLVE: USER_SUCCESS`) reduzindo o custo de $20.349$ para $171$ em 4 itera��es, com 26 keyframes e 212 marcos mapeados.
- **Taxa de Inliers Visuais:** **93,1% de inliers consistentes** (94 pontos rastreados simultaneamente).
- **Estabilidade de Ancoragem:** Deslocamento nulo da linha 3D e do badge flutuante ap�s caminhada de 10 metros com perda e recupera��o total de linha de visada.
- **Performance de Testes:** 25 testes unit�rios executados em ~2s na JVM.

### ?? Decis�es de Arquitetura (ADR)

- **ADR-013: Native ARCore Anchor Tracking & Pose Graph Correction**
  - **Contexto:** Necessidade de manter pontos de medi��o milimetricamente fixos em rela��o aos objetos reais durante movimenta��es longas no espa�o.
  - **Decis�o:** Associa��o dos pontos A e B a n�s nativos `Anchor` do ARCore, propaga��o frame a frame das coordenadas corrigidas pelo grafo de poses via `StateFlow` e invoca��o determin�stica de `anchor.detach()` para preven��o de vazamento de mem�ria nativa C++.

---

## ? [Dia 07] - 2026-08-28: Pipeline Gr�fico OpenGL ES 3.0, Estabiliza��o EGL e Compatibilidade 16 KB

### ? Objetivos Conclu�dos

- [x] Cria��o do `BackgroundRenderer` com shaders GLSL ES 3.0 e suporte a `GL_TEXTURE_EXTERNAL_OES` para proje��o com *zero-copy* do feed de v�deo da c�mera.
- [x] Implementa��o do `SpatialLineRenderer` em OpenGL ES 3.0 para tra�ado dos n�s de ancoragem (`GL_POINTS`) e do vetor de medi��o (`GL_LINES`) no espa�o tridimensional.
- [x] Multiplica��o matricial Model-View-Projection ($M_{clip} = M_{proj} \times M_{view} \times M_{model}$) em tempo real alimentada pelas matrizes da c�mera ARCore.
- [x] Prealoca��o est�tica de matrizes e buffers nativos diretos (`FloatBuffer`) garantindo zero aloca��o de mem�ria no loop de renderiza��o (Zero GC Churn).
- [x] Integra��o completa dos renderizadores no ciclo do `GLSurfaceView` (`onSurfaceCreated`, `onSurfaceChanged`, `onDrawFrame`) em `ArCameraFeed`.
- [x] Conex�o dos pontos A e B do `uiState` � camada gr�fica via `rememberUpdatedState`.
- [x] Cria��o da su�te `SpatialLineMathTest` e atualiza��o de `MeasurementViewModelTest` com 100% dos testes unit�rios passando na JVM.
- [x] Registro da decis�o arquitetural no `ADR-012`.

### ?? Desafios de Engenharia & Diagn�stico em Hardware

1. **Condi��o de Corrida no Ciclo de Vida do ARCore (`AR_ERROR_SESSION_PAUSED`):**
- *Causa:* A `GLThread` chamava `session.update()` antes da Main Thread executar `session.resume()`, e o encerramento concorrente no `onPause` causava falha de precondi��o no scheduler do MediaPipe.
- *Solu��o:* Centraliza��o estrita do ciclo de vida na Main Thread via flag `@Volatile isRunning` e sincroniza��o determin�stica no `DisposableEffect` (no pause: paralisa a `GLSurfaceView` antes da `Session`; no resume: retoma a `Session` antes da `GLSurfaceView`).
2. **Compatibilidade com P�ginas de Mem�ria de 16 KB (Android 15+):**
- *Causa:* O bin�rio nativo legado `libimage_processing_util_jni.so` do CameraX continha segmentos `LOAD` desalinhados.
- *Solu��o:* Remo��o de depend�ncias redundantes do CameraX (c�mera gerenciada pelo ARCore), upgrade do ARCore para `1.46.0` e configura��o de `jniLibs.useLegacyPackaging = false` no Gradle.
3. **Flickering e Artefatos Crom�ticos na GPU Qualcomm Adreno:**
- *Causa:* Chamadas repetidas a `session.setCameraTextureNames()` a 60 FPS no `onDrawFrame` e coordenadas UV n�o inicializadas no primeiro frame.
- *Solu��o:* Vincula��o at�mica �nica do ID de textura OES, amostragem obrigat�ria com `GL_CLAMP_TO_EDGE` e transforma��o cont�nua de coordenadas normalizadas no `BackgroundRenderer`.

### ? M�tricas de Valida��o no Dispositivo (Motorola Edge 50 Fusion)

- **Taxa de Quadros:** 60 FPS cont�nuos e sustentados ao longo de mais de 850 frames de v�deo renderizados.
- **Converg�ncia VIO (Visual-Inertial Odometry):** Transi��o para `VIO_TRACKING` em apenas **441 ms**.
- **Mapeamento Espacial 3D:** Constru��o de mapa ADF contendo 26 keyframes, 252 landmarks f�sicos e taxa de inliers visuais de **94,6%**.
- **Performance de Testes:** Su�te completa de testes da JVM executada em ~4s.

### ?? Decis�es de Arquitetura (ADR)

- **ADR-012: Zero-Copy OES Camera Texture and OpenGL ES 3.0 Spatial Geometry Pipeline**
  - **Contexto:** Necessidade de renderiza��o em alta frequ�ncia (60 FPS) do v�deo da c�mera e da geometria m�trica sem aloca��es din�micas na GPU/CPU.
  - **Decis�o:** Ado��o de textura externa OES via GLSL ES 3.0, prealoca��o est�tica de buffers/matrizes e consumo s�ncrono do estado do Compose pela thread gr�fica EGL.

---

## ? [Dia 06] - 2026-08-27: Spatial Raycasting, Polygon Gating e Testes Unit�rios de Colis�o

### ? Objetivos Conclu�dos

- [x] Cria��o do processador de baixo n�vel `ArCoreHitTestProcessor` para proje��o de raios �pticos a partir de coordenadas normalizadas de tela $[0.0, 1.0]$.
- [x] Implementa��o de filtragem estrita por pol�gono convexo (`isPoseInPolygon`) para eliminar extrapola��es de planos infinitos e falsos positivos no v�cuo.
- [x] Estabelecimento de fallback determin�stico para pontos ToF / Depth API (`Point`) com rastreamento ativo.
- [x] Refatora��o do `SpatialSensorRepositoryImpl`, eliminando a busca heur�stica 2D em favor do raycasting nativo do ARCore.
- [x] Mapeamento bidirecional de viewport entre `GLSurfaceView` (`onSurfaceChanged`), `ArCameraFeed`, reposit�rio e `MeasurementViewModel`.
- [x] Su�te completa de testes unit�rios na JVM (`ArCoreHitTestProcessorTest`) cobrindo 6 cen�rios de colis�o, planos fora de limites, clamping de tela e descarte de poses inst�veis com MockK e Google Truth.
- [x] Registro da decis�o t�cnica formal no `ADR-011`.
- [x] Valida��o integral da su�te de testes unit�rios (`./gradlew testDebugUnitTest`) executada em 4s com cache.

### ?? Decis�es de Arquitetura (ADR)

- **ADR-011: Spatial Raycasting and Convex Polygon Gating**
  - **Contexto:** A busca heur�stica 2D anterior gerava imprecis�o m�trica cumulativa e n�o garantia que os pontos ancorados pertencessem a superf�cies f�sicas coplanares ou est�veis.
  - **Decis�o:** Ado��o do `Frame.hitTest` nativo com prioriza��o de `Plane` dentro do pol�gono de suporte (`isPoseInPolygon`), fallback para pontos de profundidade ToF e convers�o direta da `Pose` do ARCore para a entidade imut�vel de dom�nio `Point3D(x, y, z)` sem contaminar a camada `domain` com o SDK Android.

---

## ? [Dia 05] - 2026-08-26: Hardware �ptico, Ciclo de Vida ARCore e Testes Unit�rios de Apresenta��o

### ? Objetivos Conclu�dos

- [x] Implementa��o do gerenciador declarativo de permiss�es em tempo de execu��o `CameraPermissionHandler` no Jetpack Compose.
- [x] Cria��o do `ArCoreSessionManager` para controle do ciclo de vida da sess�o AR, ativa��o do sensor de profundidade (`DepthMode.AUTOMATIC`) e libera��o de recursos de mem�ria.
- [x] Constru��o do componente visual `ArCameraFeed` conectando `GLSurfaceView` (OpenGL ES 3.0) ao ciclo de vida do Compose via `DisposableEffect` e `LifecycleEventObserver`.
- [x] Integra��o do feed da c�mera na `MeasurementScreen` como camada base (`z-index: 0`) sob o HUD t�tico.
- [x] Tratamento de telemetria de rastreamento (`TrackingState`) no `MeasurementViewModel` sem quebrar o desacoplamento de camadas da Clean Architecture.
- [x] Padroniza��o da biblioteca `kotlinx-coroutines-test` no cat�logo de depend�ncias (`gradle/libs.versions.toml` e `app/build.gradle.kts`).
- [x] Cria��o da su�te de testes unit�rios `MeasurementViewModelTest` com dubl� de reposit�rio (`FakeSpatialSensorRepository`), cobrindo fluxo de ancoragem de Pontos A/B, c�lculo determin�stico de dist�ncia, reset de medi��o e emiss�o de telemetria reativa.
- [x] Valida��o integral da su�te de testes unit�rios e compila��o do APK de Debug (`./gradlew testDebugUnitTest assembleDebug`).

### ?? Decis�es de Arquitetura (ADR)

- **ADR-009: Gerenciamento Declarativo de Permiss�es �pticas no Compose**
  - **Contexto:** O ARCore exige permiss�o de c�mera em tempo de execu��o. O fluxo tradicional baseado em callbacks imperativos de `Activity` acopla a camada de apresenta��o ao framework e dificulta a modulariza��o.
  - **Decis�o:** Cria��o do componente `CameraPermissionHandler` utilizando `rememberLauncherForActivityResult`, garantindo tela de bloqueio e solicita��o reativa sob demanda diretamente na �rvore do Compose.
- **ADR-010: Isolamento de Ciclo de Vida do ARCore e Contexto EGL**
  - **Contexto:** A `GLSurfaceView` e a sess�o ARCore exigem sincroniza��o estrita com o ciclo de vida do Android (`ON_RESUME`, `ON_PAUSE`, `ON_DESTROY`) para evitar vazamentos de mem�ria e corrup��o do contexto gr�fico.
  - **Decis�o:** Encapsulamento da inicializa��o e destrui��o no `ArCoreSessionManager`, acoplado ao ciclo de vida da tela via `DisposableEffect` dentro de `ArCameraFeed`.

---

## ? [Dia 04] - 2026-08-25: Processamento de Buffers AR e Interface HUD em Jetpack Compose

### ? Objetivos Conclu�dos

- [x] Cria��o do extrator de baixo n�vel `ArCoreFrameProcessor` com filtro de confian�a para convers�o de `FloatBuffer` em `List<Point3D>`.
- [x] Modelagem do estado de interface `MeasurementUiState` e implementa��o do `MeasurementViewModel` com Unidirectional Data Flow (UDF) sobre `StateFlow`.
- [x] Constru��o da tela de metrologia espacial `MeasurementScreen` em Jetpack Compose com design estilo HUD cient�fico:
  - Indicadores de telemetria (`TRACKING`, `TOF / DEPTH ON`, contagem de pontos da nuvem).
  - Ret�culo din�mico de mira central com feedback crom�tico de superf�cie.
  - Painel de leitura de dist�ncia com exibi��o de incerteza m�trica ($\pm\sigma$).
- [x] Registro do `ArCoreFrameProcessor` e do `MeasurementViewModel` no m�dulo Koin (`AppModule.kt`).
- [x] Integra��o da `MeasurementScreen` na `MainActivity`.
- [x] Valida��o completa de testes unit�rios na JVM e compila��o bem-sucedida do APK de Debug (`./gradlew assembleDebug`).

### ?? Decis�es de Arquitetura (ADR)

- **ADR-007: Filtragem e Descarte de Ru�do em Buffers Brutos (PointCloud)**
  - **Contexto:** Sensores �pticos e de tempo de voo (ToF) geram dispers�o de dados e pontos esp�rios em superf�cies reflexivas ou de baixa ilumina��o.
  - **Decis�o:** O `ArCoreFrameProcessor` aplica um limiar de confian�a configur�vel ($\ge 30\%$) diretamente na leitura do `FloatBuffer`, descartando artefatos antes de criar inst�ncias imut�veis de `Point3D` no dom�nio.
- **ADR-008: Unidirectional Data Flow (UDF) com StateFlow no HUD de Metrologia**
  - **Contexto:** A interface gr�fica precisa renderizar dados de alta frequ�ncia da c�mera ao mesmo tempo em que reage �s intera��es pontuais do usu�rio (ancoragem do Ponto A e Ponto B).
  - **Decis�o:** Centraliza��o de todo o estado em `MeasurementUiState` imut�vel, exposto via `StateFlow` pelo `MeasurementViewModel`, garantindo que a UI apenas observe e emita eventos de clique sem conter l�gica de neg�cio.

---

## ? [Dia 03] - 2026-08-24: Contrato de Reposit�rio de Sensores e Telemetria Reativa

### ? Objetivos Conclu�dos

- [x] Cria��o dos modelos de telemetria espacial (`TrackingStatus`, `SpatialFrameData`).
- [x] Defini��o do contrato de reposit�rio `SpatialSensorRepository` na camada `domain`.
- [x] Implementa��o de `SpatialSensorRepositoryImpl` com `StateFlow` na camada `data`.
- [x] Inje��o de depend�ncia via Koin como `Single`.
- [x] Testes unit�rios do reposit�rio garantindo reatividade e integridade de estado.
- [x] Configura��o da pipeline de integra��o cont�nua (CI) com GitHub Actions (`.github/workflows/android.yml`).

### ?? Decis�es de Arquitetura (ADR)

- **ADR-005: Desacoplamento do Pipeline de Sensores via Reposit�rio Reativo**
  - **Contexto:** O hardware emite frames espaciais em 30 a 60 FPS. A camada de dom�nio n�o deve ser bloqueada pela taxa de quadros do sensor.
  - **Decis�o:** Uso de `StateFlow<SpatialFrameData>` com atualiza��es at�micas (`.update { ... }`).
- **ADR-006: Abstra��o de Hit-Testing e Raycasting**
  - **Contexto:** Proje��o de coordenadas 2D de tela para coordenadas f�sicas 3D ($X, Y, Z$).
  - **Decis�o:** O reposit�rio exp�e o m�todo `performHitTest(x, y)` delegando a interse��o geom�trica para a nuvem de pontos ou mapa de profundidade denso.

---

## ? [Dia 02] - 2026-08-23: Dom�nio Matem�tico Puro e Modelagem F�sica

### ? Objetivos Conclu�dos

- [x] Cria��o das entidades imut�veis: `Point3D`, `BoundingBox3D`, `DistanceMeasurement`, `MassEstimate`.
- [x] Implementa��o dos casos de uso: `CalculateDistanceUseCase` e `EstimateSpatialDimensionsUseCase`.
- [x] Implementa��o da propaga��o din�mica de incerteza metrol�gica ($\pm\sigma$).
- [x] Cobertura de 100% em testes unit�rios com JUnit 4 e Google Truth na JVM.

### ?? Decis�es de Arquitetura (ADR)

- **ADR-003: Isolamento do Dom�nio Matem�tico em Kotlin Puro**
  - **Decis�o:** Zero depend�ncias do Android SDK na camada `domain` para garantir portabilidade e execu��o instant�nea de testes unit�rios.
- **ADR-004: Incerteza Din�mica como Entidade de Primeira Classe**
  - **Decis�o:** Toda medi��o f�sica carrega seu desvio padr�o de erro intr�nseco baseado no modelo f�sico do sensor.

---

## ? [Dia 01] - 2026-08-22: Funda��o, Setup e Governan�a

### ? Objetivos Conclu�dos

- [x] Configura��o do projeto com Kotlin 2.x, Jetpack Compose (Material 3), Gradle Kotlin DSL e Version Catalogs (`libs.versions.toml`).
- [x] Estrutura��o da Clean Architecture (`domain`, `data`, `presentation`).
- [x] Inje��o de depend�ncia com Koin.
- [x] Publica��o do reposit�rio no GitHub com licen�a Apache 2.0.

### ?? Decis�es de Arquitetura (ADR)

- **ADR-001: Ado��o do Koin em vez de Hilt/Dagger**
  - **Decis�o:** Inje��o de depend�ncia 100% Kotlin puro sem gera��o pesada de c�digo ou problemas com novas vers�es do compilador K2.
- **ADR-002: Licenciamento Apache 2.0 e Estrat�gia Open Core**
  - **Decis�o:** N�cleo aberto para autoridade t�cnica e portf�lio p�blico, com suporte a extens�es propriet�rias via contratos de plugin.

---

## Próximos passos definidos para o Dia 14

- [ ] Apresentar no HUD a fonte das âncoras da dimensão atual.
- [ ] Exibir aviso visual quando uma medição utilizar `INSTANT_PLACEMENT`.
- [ ] Definir uma política explícita para confirmação de resultados aproximados.
- [ ] Preservar a procedência das âncoras junto às dimensões já confirmadas.
- [ ] Preparar a procedência para futura persistência e exportação de relatórios.
- [ ] Repetir a validação física comparando posicionamentos convencionais e aproximados.
- [ ] Executar novamente testes unitários, Android Lint e montagem do APK após a integração visual.
