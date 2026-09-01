# Aetheris

[![Android CI](https://github.com/maurizioprizzi/aetheris/actions/workflows/android.yml/badge.svg)](https://github.com/maurizioprizzi/aetheris/actions/workflows/android.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-purple.svg)](https://kotlinlang.org)
[![ARCore](https://img.shields.io/badge/ARCore-1.46.0-blue.svg)](https://developers.google.com/ar)
[![OpenGL ES](https://img.shields.io/badge/OpenGL_ES-3.0-orange.svg)](https://developer.android.com/guide/topics/graphics/opengl)
[![16 KB Page Size](https://img.shields.io/badge/16_KB_Page_Size-Configured-brightgreen.svg)](https://developer.android.com/16kb-page-size)
[![Min SDK](https://img.shields.io/badge/Min_SDK-26-green.svg)](https://developer.android.com)

**Aetheris** is an open-source experimental spatial metrology platform for Android.

It combines ARCore tracking, native anti-drift anchors, spatial raycasting, point-cloud filtering, OpenGL ES 3.0 graphics, and Jetpack Compose to measure distances, calculate 3D bounding-box volumes, and estimate physical mass through material density in real-world environments.

The project is an exploration of mobile spatial computing, real-time graphics pipelines, Unidirectional Data Flow (UDF), and uncertainty-aware physical modeling.

> [!IMPORTANT]
> Aetheris is an experimental spatial computing system and not a certified metrological instrument. Measurements depend on device hardware, camera calibration, lighting, surface texture, and ARCore tracking stability.

---

## Current Capabilities

| Capability | Status | Description |
| :--- | :--- | :--- |
| **ARCore Camera Background** | Implemented | Zero-copy `GL_TEXTURE_EXTERNAL_OES` rendering pipeline. |
| **OpenGL ES 3.0 Graphics** | Implemented | Real-time 3D rendering for lines (`GL_LINES`) and anchor points (`GL_POINTS`). |
| **Point-Cloud Filtering** | Implemented | Strict confidence thresholding, NaN/infinite value discarding, and safe `AutoCloseable` lifecycle. |
| **Raycasting & Surface Gating** | Implemented | Plane polygon gating (`isPoseInPolygon`), oriented feature points, and ToF point validation. |
| **Instant Placement Fallback** | Implemented | Deterministic point estimation on untextured/low-plane surfaces via `LOCAL_Y_UP`. |
| **Frame-Affine Placement Queue**| Implemented | Thread-safe raycasting and anchor attachment synchronized on the active render frame. |
| **Anti-Drift Anchor Tracking** | Implemented | Persistent native ARCore `Anchor` binding corrected frame-by-frame by SLAM pose graphs. |
| **Linear Distance Measurement** | Implemented | High-precision Euclidean norm calculation with dynamic metrological uncertainty ($\pm\sigma$). |
| **Sequential 3-Axis Volume** | Implemented | Guided $W \to H \to D$ capture with AABB volume calculation in $\text{m}^3$ and liters. |
| **Mass Estimation by Density** | Implemented | Physical mass calculation across standard materials with combined uncertainty propagation. |
| **World-to-Screen Projection** | Implemented | Full MVP projective transformation with frustum clipping guard ($w_c \le 0$). |
| **Floating Compose Badge** | Implemented | Screen-space reactive badge tracking spatial midpoints in real time. |
| **Depth API** | Fallback / Disabled | Intentionally disabled (`DepthMode.DISABLED`) to avoid native `ComputeDisparity` driver faults. |
| **Multipoint Polylines** | Planned | Sequential multi-node boundary tracking. |
| **3D Polygon Area (Shoelace)** | Planned | Coplanar surface area calculation and translucent mesh rendering. |
| **Metrological Export** | Planned | Structured spatial dataset export in JSON and CSV formats. |

---

## Spatial Measurement Pipeline

```text
Touch / Viewport Input (Normalized NDC)
    │
    ▼
Frame-Affine Request Queue (ConcurrentLinkedQueue)
    │
    ▼ [Render Thread - GLThread]
Raycasting Execution (Plane Polygon → Oriented Point → Instant Placement)
    │
    ▼
Native Anchor Creation & Binding (com.google.ar.core.Anchor)
    │
    ▼
Continuous SLAM Pose Graph Correction (Pose.translation)
    │
    ├──► OpenGL ES 3.0 Geometry Pass (Lines & Points)
    │
    ├──► Domain Mathematical Engine (Distance → Volume → Mass)
    │
    ▼
World-to-Screen Projection (M_proj × M_view × M_model) + Frustum Culling
    │
    ▼
Reactive Compose HUD Overlay (Floating Measurement Badge & Telemetry)
```

1. **VIO & Tracking:** ARCore tracks 6-DOF camera pose via visual-inertial odometry.
2. **Buffer Ingestion:** Point cloud is acquired, validated, and filtered via `ArCoreFrameProcessor`.
3. **Synchronized Placement:** Screen taps are queued and processed synchronously on the `GLThread` against the active frame.
4. **Hit Testing & Fallback:** Raycasting validates tracked plane polygons first, oriented feature points second, and Instant Placement points as fallback.
5. **Dynamic Anchoring:** Native `Anchor` instances are bound to spatial nodes. As the SLAM map optimizes, poses are updated frame-by-frame.
6. **Domain Calculation:** The pure Kotlin domain calculates Euclidean distance, bounding-box volume, or mass along with statistical uncertainty.
7. **Spatial Rendering:** `SpatialLineRenderer` draws the 3D geometry directly onto the EGL context.
8. **Projection & HUD:** `ProjectWorldToScreenUseCase` maps 3D points to 2D pixel coordinates, rendering floating Compose badges over the physical target.

---

## Mathematical Models

### 1. Euclidean Distance & Metric Uncertainty

Distance between two 3D spatial points $P_1(x_1, y_1, z_1)$ and $P_2(x_2, y_2, z_2)$ is computed using double-precision Euclidean norm:

$$d = \sqrt{(x_2 - x_1)^2 + (y_2 - y_1)^2 + (z_2 - z_1)^2}$$

Dynamic metrological uncertainty ($u_d$) scales based on distance and tracking confidence:

$$u_d = u_{\text{base}} \cdot \frac{1 + \frac{d}{d_{\text{ref}}}}{c_{\text{effective}}}$$

### 2. Bounding-Box (AABB) Volume & Uncertainty Propagation

Volume from sequential axis capture ($w$: width, $h$: height, $d$: depth) is defined as:

$$V = w \times h \times d$$

Uncertainty ($u_V$) is propagated from independent axis variances without singularity risks:

$$u_V = \sqrt{(h \cdot d \cdot u_w)^2 + (w \cdot d \cdot u_h)^2 + (w \cdot h \cdot u_d)^2}$$

### 3. Mass Estimation by Physical Density

Mass ($M$) combines calculated volume with material density ($\rho$):

$$M = V \times \rho$$

Combined standard uncertainty ($u_M$) accounts for spatial volume variance and material density tolerance ($u_\rho$):

$$u_M = \sqrt{(\rho \cdot u_V)^2 + (V \cdot u_\rho)^2}$$

---

## Clean Architecture & System Structure

The application strictly separates pure Kotlin business logic from Android and ARCore dependencies.

```text
org.aetheris.app
├── data
│   ├── arcore
│   │   ├── ArCoreFrameProcessor.kt
│   │   ├── ArCoreHitTestProcessor.kt
│   │   └── ArCoreSessionManager.kt
│   ├── opengl
│   │   ├── BackgroundRenderer.kt
│   │   └── SpatialLineRenderer.kt
│   └── repository
│       └── SpatialSensorRepositoryImpl.kt
├── domain
│   ├── math
│   │   └── SpatialLineMath.kt
│   ├── model
│   │   ├── AnchorSlot.kt
│   │   ├── BoundingBox3D.kt
│   │   ├── DimensionAxis.kt
│   │   ├── DistanceMeasurement.kt
│   │   ├── MassEstimate.kt
│   │   ├── MaterialDensity.kt
│   │   ├── Point3D.kt
│   │   ├── ScreenPoint2D.kt
│   │   ├── SpatialDimensions.kt
│   │   ├── SpatialFrameData.kt
│   │   ├── TrackingStatus.kt
│   │   └── VolumeMeasurement.kt
│   ├── repository
│   │   └── SpatialSensorRepository.kt
│   └── usecase
│       ├── CalculateDistanceUseCase.kt
│       ├── CalculateVolumeUseCase.kt
│       ├── EstimateMassUseCase.kt
│       ├── EstimateSpatialDimensionsUseCase.kt
│       └── ProjectWorldToScreenUseCase.kt
├── presentation
│   ├── components
│   │   ├── ArCameraFeed.kt
│   │   ├── FloatingMeasurementBadge.kt
│   │   └── MeasurementCrosshair.kt
│   ├── measurement
│   │   ├── MeasurementScreen.kt
│   │   ├── MeasurementUiState.kt
│   │   └── MeasurementViewModel.kt
│   └── theme
├── di
│   └── AppModule.kt
└── MainActivity.kt
```

---

## Technology Stack

- **Core & Runtime:** Kotlin 2.x, Android SDK (Min SDK 26, Target SDK 35), 16 KB Memory Page Aligned.
- **UI & Presentation:** Jetpack Compose, Material 3, Unidirectional Data Flow (UDF), `StateFlow`.
- **Spatial Computing & Graphics:** ARCore SDK 1.46.0, OpenGL ES 3.0, GLSL ES 3.0, EGL context isolation.
- **Dependency Injection:** Koin (Clean Activity-level resolution, zero code-generation).
- **Testing & Quality:** JUnit 4, MockK, Google Truth, Coroutines Test (`kotlinx-coroutines-test`), Android Lint.
- **DevOps & CI:** GitHub Actions, Gradle Kotlin DSL (`libs.versions.toml`).

---

## Testing & Quality Assurance

Aetheris maintains a deterministic green test baseline across all mathematical use cases, repository doubles, and ViewModel state transitions.

```text
128 tests executed
0 failures
BUILD SUCCESSFUL
```

Run the complete local verification pipeline:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug --no-configuration-cache
```

---

## Architecture Decision Records (ADRs)

All core engineering and architectural decisions are formalized in [`docs/adr`](docs/adr):

- `ADR-001`: Adoption of Koin over Hilt/Dagger for pure Kotlin portability.
- `ADR-002`: Apache 2.0 Licensing and Open-Core Strategy.
- `ADR-003`: Pure Kotlin Mathematical Domain Isolation.
- `ADR-004`: First-Class Metrological Uncertainty Modeling.
- `ADR-005`: Reactive Sensor Pipeline via `StateFlow` Decoupling.
- `ADR-006`: Raycasting & Hit-Testing Spatial Abstraction.
- `ADR-007`: Point-Cloud Noise Filtering & `FloatBuffer` Confidence Gating.
- `ADR-008`: Unidirectional Data Flow (UDF) in Compose HUD.
- `ADR-009`: Declarative Camera Permission Lifecycle Management.
- `ADR-010`: ARCore Lifecycle and OpenGL Context Isolation.
- `ADR-011`: Spatial Raycasting and Convex Polygon Gating.
- `ADR-012`: Zero-Copy OES Camera Texture & OpenGL ES 3.0 Geometry Pipeline.
- `ADR-013`: Native ARCore Anchor Tracking & Pose Graph Correction.
- `ADR-014`: Defensive ARCore/OpenGL Resource Management and Depth Fallback.
- `ADR-015`: Sequential Axis Capture & Uncertainty-Aware AABB Volume.
- `ADR-016`: Frame-Affine Placement Queue & Instant Placement Fallback.

---

## Development Log

Detailed day-by-day development logs, mathematical derivations, device diagnostic benchmarks, and regression analyses are maintained in [`DEVLOG.md`](DEVLOG.md).

---

## Build & Installation

### Prerequisites

- JDK 17.
- Android Studio Ladybug | 2024.2.1+ or compatible.
- Physical ARCore-compatible Android device (USB Debugging enabled).

### Commands

```bash
# Clone the repository
git clone [https://github.com/maurizioprizzi/aetheris.git](https://github.com/maurizioprizzi/aetheris.git)
cd aetheris

# Ensure executable permissions
chmod +x gradlew

# Run unit test suite
./gradlew testDebugUnitTest --no-configuration-cache

# Build debug APK
./gradlew assembleDebug --no-configuration-cache

# Install directly to connected device
./gradlew installDebug
```

---

## License

Aetheris is distributed under the [Apache License 2.0](LICENSE).

```text
Copyright 2026 Maurizio Prizzi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)
```