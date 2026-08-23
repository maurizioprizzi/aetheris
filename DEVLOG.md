# 📓 Aetheris — Developer Log & Scientific Journal

> Projeto aberto de metrologia espacial e inferência física em tempo real para Android.

---

## 📅 [Dia 01] - 2026-08-23: Fundação Arquitetural e Configuração Base

### 🎯 Objetivo do Dia
- [x] Definição do nome do projeto (**Aetheris**).
- [x] Estruturação modular com Clean Architecture + MVI.
- [x] Padronização do Version Catalog (`libs.versions.toml`) com Jetpack Compose, Coroutines, Hilt e ARCore.
- [x] Criação do repositório Git e governança de documentação.

### 📐 Decisões de Arquitetura (ADR)
- **ADR-001: Separação Estrita de Domínio Matemático**
    - **Contexto:** Cálculos de projeção 3D e estimativas físicas não devem depender do framework Android para permitir testes unitários rápidos na JVM.
    - **Decisão:** A camada `domain` conterá apenas código Kotlin puro com estruturas vetoriais e casos de uso sem referências ao SDK Android.

### 🧪 Desafios & Soluções
- *Desafio:* Garantir injeção de dependência moderna sem o uso do KAPT legado.
- *Solução:* Adoção de KSP (*Kotlin Symbol Processing*) com Hilt 2.51+.

### 🚀 Próximos Passos (Dia 02)
- [ ] Modelagem das entidades matemáticas em Kotlin puro na camada `domain` (`Point3D`, `DistanceMeasurement`, `BoundingBox3D`).
- [ ] Implementação de casos de uso com testes unitários puros na JVM.