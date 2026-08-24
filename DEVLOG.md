# 📓 Aetheris — Developer Log & Scientific Journal

> Projeto aberto de metrologia espacial e inferência física em tempo real para Android.

---

## 📅 [Dia 03] - 2026-08-24: Contrato de Repositório de Sensores e Telemetria Reativa

### 🎯 Objetivo do Dia
- [x] Criação dos modelos de telemetria espacial (`TrackingStatus`, `SpatialFrameData`).
- [x] Definição do contrato de repositório `SpatialSensorRepository` na camada `domain`.
- [x] Implementação de `SpatialSensorRepositoryImpl` com `StateFlow` na camada `data`.
- [x] Injeção de dependência via Koin como `Single`.
- [x] Testes unitários do repositório garantindo reatividade e integridade de estado.

### 📐 Decisões de Arquitetura (ADR)
- **ADR-005: Desacoplamento do Pipeline de Sensores via Repositório Reativo**
  - **Contexto:** ARCore e CameraX emitem eventos em alta frequência (30 a 60 FPS) na thread de renderização. O domínio precisa consumir apenas o estado mais recente sem travar a UI.
  - **Decisão:** Utilização de `StateFlow<SpatialFrameData>` com buffer conflated atômico (`.update { ... }`).
- **ADR-006: Abstração de Hit-Testing e Raycasting**
  - **Contexto:** Projeção de coordenadas 2D de tela para coordenadas espaciais 3D ($X, Y, Z$) precisa funcionar independentemente da presença de sensor ToF dedicado.
  - **Decisão:** O contrato do repositório expõe `performHitTest(x, y)` delegando para nuvem de pontos esparsa ou mapa de profundidade denso.

### 🚀 Próximos Passos (Dia 04)
- [ ] Implementação do `ArCoreFrameProcessor` para extração de `FloatBuffer` da Depth API.
- [ ] Construção do `SpatialMeasurementViewModel` e interface de UI em Jetpack Compose.

---

## 📅 [Dia 02] - 2026-08-23: Núcleo Matemático de Domínio e Testes Unitários

### 🎯 Objetivo do Dia
- [x] Modelagem de entidades matemáticas em Kotlin puro (`Point3D`, `DistanceMeasurement`, `BoundingBox3D`, `MassEstimate`).
- [x] Implementação dos casos de uso de medição espacial (`CalculateDistanceUseCase`, `EstimateSpatialDimensionsUseCase`).
- [x] Configuração da injeção de dependência dos UseCases via Koin.
- [x] Cobertura de testes unitários com JUnit 4 e Google Truth na JVM (execução rápida e isolada de Android).

### 📐 Decisões de Arquitetura (ADR)
- **ADR-003: Modelagem de Incerteza Dinâmica**
  - **Contexto:** Sensores ópticos perdem precisão conforme a distância aumenta e a iluminação varia.
  - **Decisão:** O cálculo de distância retorna um objeto `DistanceMeasurement` contendo o valor medido e a margem de erro estimada ($\pm\sigma$), em vez de um `Float` primitivo.
- **ADR-004: Cálculo de Dimensões por Bounding Box AABB**
  - **Contexto:** Necessidade de inferir largura, altura, profundidade e volume a partir de uma nuvem esparsa de pontos tridimensionais.
  - **Decisão:** Implementação de cálculo de *Axis-Aligned Bounding Box* (AABB) direto na camada de domínio.

### 🧪 Desafios & Soluções
- *Desafio:* Garantir que a lógica geométrica e vetorial seja completamente agnóstica de SDK ou bibliotecas gráficas pesadas.
- *Solução:* Criação de operadores vetoriais e cálculos euclidianos em Kotlin puro no pacote `domain.model`.

### 🚀 Próximos Passos (Dia 03)
- [ ] Definição do contrato de repositório `SpatialSensorRepository` na camada de domínio.
- [ ] Implementação do `ArCoreSpatialDataSource` e pipeline de captura de frames na camada `data`.

---

## 📅 [Dia 01] - 2026-08-23: Fundação Arquitetural e Configuração Base

### 🎯 Objetivo do Dia
- [x] Definição do escopo do projeto (**Aetheris**).
- [x] Estruturação modular com Clean Architecture.
- [x] Setup do Gradle moderno com Kotlin 2.0, Jetpack Compose, CameraX e ARCore.
- [x] Configuração da injeção de dependência com Koin.
- [x] Criação do repositório Git e governança de documentação.