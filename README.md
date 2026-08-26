# 📐 Aetheris

[![Android CI](https://github.com/maurizioprizzi/aetheris/actions/workflows/android.yml/badge.svg)](https://github.com/maurizioprizzi/aetheris/actions/workflows/android.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-purple.svg)](https://kotlinlang.org)
[![ARCore](https://img.shields.io/badge/ARCore-1.42.0-blue.svg)](https://developers.google.com/ar)
[![Min SDK](https://img.shields.io/badge/Min_SDK-26-green.svg)](https://developer.android.com)

**Aetheris** is an open-source citizen science and spatial metrology platform for Android. By combining low-level optical hardware streams (ARCore & OpenGL ES 3.0), real-time sensor filtering, and deterministic physical inference models, Aetheris transforms commercial smartphones into scientific-grade spatial measurement instruments.

---

## 🔬 Scientific & Metrology Core

- **Spatial Distance:** Real-time range estimation using spatial depth projection with dynamic Gaussian uncertainty propagation ($\pm\sigma$).
- **3D Dimensions:** Axis-Aligned Bounding Box (AABB) extraction for spatial width, height, depth, and volume in liters/$m^3$.
- **Optical Noise Filtering:** Direct Point Cloud parsing with confidence-score thresholding to discard surface scattering.
- **Physical Inference:** Anthropometric mass estimation and density-based volume-to-mass calculations.

---

## 🏛️ Architecture & Tech Stack

The system strictly adheres to **Clean Architecture** and **Unidirectional Data Flow (UDF)**:

- **Pure JVM Domain:** Core mathematical models, metric calculations, and validation completely isolated from Android SDK dependencies.
- **Hardware & Data Pipeline:** Low-level ARCore 1.42.0 lifecycle management, OpenGL ES 3.0 (`GLSurfaceView`), and reactive telemetry streams via `StateFlow`.
- **Declarative UI Layer:** Real-time tactical HUD built natively in **Jetpack Compose** with Material 3.
- **Dependency Injection:** **Koin** for clean module wiring.
- **Testing Suite:** Comprehensive JVM unit testing with **JUnit 4**, **Google Truth**, and **kotlinx-coroutines-test**.

---

## 🛠️ Build & Verification

Ensure you have **JDK 17** configured. To run the full verification pipeline locally:

```bash
# Execute pure JVM unit tests
./gradlew testDebugUnitTest

# Assemble debug artifact
./gradlew assembleDebug