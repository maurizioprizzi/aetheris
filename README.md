# 🌐 Aetheris

[![Android CI](https://github.com/maurizioprizzi/aetheris/actions/workflows/android.yml/badge.svg)](https://github.com/maurizioprizzi/aetheris/actions/workflows/android.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-purple.svg)](https://kotlinlang.org)
[![Min SDK](https://img.shields.io/badge/Min_SDK-26-green.svg)](https://developer.android.com)

**Aetheris** is an open-source citizen science and spatial metrology platform for Android. By combining real-time computer vision, AR spatial depth mapping, and physical inference models, Aetheris transforms standard smartphones into scientific-grade measurement devices.

---

## 🔬 Scientific Core

- **Spatial Distance:** Real-time range estimation using depth projection with dynamic uncertainty propagation ($\pm\sigma$).
- **3D Dimensions:** Axis-Aligned Bounding Box (AABB) extraction for width, height, depth, and volume in liters/$m^3$.
- **Physical Inference:** Anthropometric mass estimation and density-based volume-to-mass calculations.

---

## 🏗️ Architecture

- **Clean Architecture** with strict isolation of mathematical logic (`domain`, `data`, `presentation`).
- **Unidirectional Data Flow (UDF)** with StateFlow and Jetpack Compose.
- **Koin** for dependency injection.
- **JUnit 4 & Google Truth** for pure JVM unit testing.

---

## 📖 Daily Development Log

Follow our daily engineering progress and Architectural Decision Records (ADRs) in [DEVLOG.md](DEVLOG.md).