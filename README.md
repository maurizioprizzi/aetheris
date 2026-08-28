# 🛰️ Aetheris

[![Android CI](https://github.com/maurizioprizzi/aetheris/actions/workflows/android.yml/badge.svg)](https://github.com/maurizioprizzi/aetheris/actions/workflows/android.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-purple.svg)](https://kotlinlang.org)
[![ARCore](https://img.shields.io/badge/ARCore-1.42.0-blue.svg)](https://developers.google.com/ar)
[![OpenGL ES](https://img.shields.io/badge/OpenGL_ES-3.0-orange.svg)](https://developer.android.com/guide/topics/graphics/opengl)
[![Min SDK](https://img.shields.io/badge/Min_SDK-26-green.svg)](https://developer.android.com)

**Aetheris** is an open-source citizen science and spatial metrology platform for Android. By combining low-level optical hardware streams (ARCore & OpenGL ES 3.0), real-time sensor filtering, and deterministic physical inference models, Aetheris transforms commercial smartphones into scientific-grade spatial measurement instruments.

---

## 🔬 Scientific & Metrology Core

- **Spatial Distance & Vector Tracking:** Real-time range estimation using spatial raycasting with dynamic Gaussian uncertainty propagation ($\pm\sigma$).
- **Hardware-Accelerated 3D Rendering:** Native OpenGL ES 3.0 shaders rendering real-time metric vectors (`GL_LINES`) and anchor nodes (`GL_POINTS`) with zero GC overhead.
- **Zero-Copy Camera Pipeline:** Direct GPU decoding of the camera stream via `GL_TEXTURE_EXTERNAL_OES` and dynamic display geometry mapping (`transformCoordinates2d`).
- **Convex Polygon Gating:** Hardware hit-testing constrained to physical support planes (`isPoseInPolygon`), with deterministic fallback to active ToF/Depth points.
- **3D Dimensions:** Axis-Aligned Bounding Box (AABB) extraction for spatial width, height, depth, and volume in liters/$m^3$.
- **Optical Noise Filtering:** Direct Point Cloud parsing with confidence-score thresholding to discard surface scattering.
- **Physical Inference:** Anthropometric mass estimation and density-based volume-to-mass calculations.

---

## 🏛️ Architecture & Tech Stack

The system strictly adheres to **Clean Architecture** and **Unidirectional Data Flow (UDF)**:

- **Pure JVM Domain:** Core mathematical models, metric calculations, and uncertainty propagation completely isolated from Android SDK dependencies.
- **Hardware & Graphics Pipeline:** Low-level ARCore 1.42.0 lifecycle management, OpenGL ES 3.0 (`GLSurfaceView` with `rememberUpdatedState`), and reactive telemetry streams via `StateFlow`.
- **Declarative UI Layer:** Real-time tactical HUD built natively in **Jetpack Compose** with Material 3.
- **Dependency Injection:** **Koin** for clean, lightweight module wiring.
- **Testing Suite:** Comprehensive JVM unit testing with **JUnit 4**, **MockK**, **Google Truth**, and **kotlinx-coroutines-test**.

---

## 📐 Architecture Decision Records (ADR)

All technical and algorithmic decisions are documented under `/docs/adr`:

- `ADR-001`: Adoption of Koin over Hilt/Dagger
- `ADR-002`: Apache 2.0 Open Core Licensing Strategy
- `ADR-003`: Pure Kotlin Mathematical Domain Isolation
- `ADR-004`: First-Class Metrological Uncertainty ($\pm\sigma$)
- `ADR-005`: Decoupled Sensor Stream via Reactive Repository
- `ADR-006`: Hit-Testing & Raycasting Abstraction
- `ADR-007`: Point Cloud Noise Thresholding
- `ADR-008`: Unidirectional Data Flow (UDF) with StateFlow
- `ADR-009`: Declarative Optical Permissions in Jetpack Compose
- `ADR-010`: EGL Context & ARCore Lifecycle Isolation
- `ADR-011`: Spatial Raycasting and Convex Polygon Gating
- `ADR-012`: Zero-Copy OES Camera Texture and OpenGL ES 3.0 Spatial Geometry Pipeline

---

## 🚀 Build & Verification

Ensure you have **JDK 17** configured. To run the full verification pipeline locally:

```bash
# Execute pure JVM unit tests
./gradlew testDebugUnitTest

# Assemble debug artifact
./gradlew assembleDebug