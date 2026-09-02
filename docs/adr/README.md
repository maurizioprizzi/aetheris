# Architecture Decision Records

This directory contains the Architecture Decision Records (ADRs) for Aetheris.

ADRs document significant technical decisions, their context, consequences, and the alternatives considered during development.

## Status

The architectural decisions listed below were originally recorded inside the project development log.

They are being migrated gradually into standalone ADR documents. Until each migration is completed, the `DEVLOG.md` remains the historical source for the corresponding decision.

## Decision registry

| ADR | Decision | Implementation status | Standalone record |
|---|---|---:|---:|
| ADR-001 | Adoption of Koin over Hilt and Dagger | Implemented | Pending |
| ADR-002 | Apache 2.0 licensing and open-core strategy | Implemented | Pending |
| ADR-003 | Pure Kotlin mathematical domain isolation | Implemented | Pending |
| ADR-004 | First-class metrological uncertainty modeling | Implemented | Pending |
| ADR-005 | Reactive sensor pipeline with `StateFlow` | Implemented | Pending |
| ADR-006 | Spatial raycasting and hit-testing abstraction | Implemented | Pending |
| ADR-007 | Point-cloud noise filtering and confidence gating | Implemented | Pending |
| ADR-008 | Unidirectional Data Flow in the Compose interface | Implemented | Pending |
| ADR-009 | Declarative camera permission management | Implemented | Pending |
| ADR-010 | ARCore lifecycle and EGL context isolation | Implemented | Pending |
| ADR-011 | Convex polygon gating for spatial raycasting | Implemented | Pending |
| ADR-012 | Zero-copy OES camera texture and OpenGL geometry | Implemented | Pending |
| ADR-013 | Native ARCore anchor tracking and pose correction | Implemented | Pending |
| ADR-014 | Defensive ARCore/OpenGL resource management and Depth fallback | Implemented | Pending |
| ADR-015 | Sequential axis capture and uncertainty-aware AABB volume | Implemented | Pending |
| [ADR-016](ADR-016-frame-affine-placement.md) | Frame-affine placement queue and Instant Placement fallback | Implemented | Available |

## ADR lifecycle

Each standalone ADR uses one of the following statuses:

- **Proposed:** under evaluation and not yet adopted.
- **Accepted:** approved for implementation.
- **Implemented:** reflected in the current source code.
- **Superseded:** replaced by a newer architectural decision.
- **Deprecated:** retained for historical context but no longer recommended.

## Naming convention

Standalone records follow this filename format: `ADR-NNN-short-decision-title.md`.

For example: `ADR-016-frame-affine-placement.md`.

## Documentation principles

Aetheris ADRs should:

1. Describe the engineering problem without depending on hidden project history.
2. State the selected decision explicitly.
3. Explain the relevant technical and scientific constraints.
4. Record important alternatives that were rejected.
5. Describe positive and negative consequences.
6. Distinguish implemented behavior from planned work.
7. Avoid presenting experimental measurements as certified metrology.

## Historical record

The chronological development history, hardware experiments, test results, and early architecture notes remain available in the repository root:

- [`DEVLOG.md`](../../DEVLOG.md)
- [`README.md`](../../README.md)
