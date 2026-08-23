# 🌐 Aetheris

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-purple.svg)](https://kotlinlang.org)
[![Android Min SDK](https://img.shields.io/badge/Min_SDK-26-green.svg)](https://developer.android.com)

**Aetheris** is an open-source citizen science and spatial metrology platform for Android. By combining real-time computer vision, AR spatial depth mapping, and biomechanical/material inference models, Aetheris transforms standard smartphones into scientific-grade measurement devices.

---

## 🔬 Scientific Core & Features

- **Spatial Distance:** Millimeter/centimeter range estimation using ARCore Depth API & point cloud projection.
- **3D Dimensions:** Real-time volumetric bounding box extraction ($X, Y, Z$).
- **Physical Inference:** Anthropometric body mass estimation and material-based volume-to-density calculations.

---

## 🏗️ Architecture

Built adhering to modern Android engineering best practices:
- **Clean Architecture** with strict layer separation (`domain`, `data`, `presentation`).
- **MVI (Model-View-Intent)** with Unidirectional Data Flow (UDF).
- **Jetpack Compose & Material 3** for HUD telemetry rendering.
- **Dagger Hilt** with KSP for Dependency Injection.
- **Coroutines & Flow** for high-throughput sensor stream processing.

---

## 📖 Daily Development Log

Follow our incremental daily engineering progress in [DEVLOG.md](DEVLOG.md).