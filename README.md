# Aetheris

[![Android CI](https://github.com/maurizioprizzi/aetheris/actions/workflows/android.yml/badge.svg)](https://github.com/maurizioprizzi/aetheris/actions/workflows/android.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-purple.svg)](https://kotlinlang.org)
[![ARCore](https://img.shields.io/badge/ARCore-1.46.0-blue.svg)](https://developers.google.com/ar)
[![OpenGL ES](https://img.shields.io/badge/OpenGL_ES-3.0-orange.svg)](https://developer.android.com/guide/topics/graphics/opengl)
[![16 KB Compatibility](https://img.shields.io/badge/16_KB_Compatibility-Under_Validation-yellow.svg)](https://developer.android.com/guide/practices/page-sizes)
[![Min SDK](https://img.shields.io/badge/Min_SDK-26-green.svg)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target_SDK-36-green.svg)](https://developer.android.com)

**Aetheris** is an open-source experimental spatial measurement platform for Android.

It combines ARCore tracking, native anchors, spatial raycasting, point-cloud filtering, OpenGL ES 3.0 graphics, and Jetpack Compose to measure distances, calculate approximate three-axis volumes, and estimate object mass from a user-selected material density.

The project explores mobile spatial computing, real-time graphics, Unidirectional Data Flow (UDF), frame-affine ARCore processing, and uncertainty-aware physical modeling.

> [!IMPORTANT]
> Aetheris is an experimental spatial computing system, not a certified metrological instrument or a substitute for a calibrated scale. Results depend on device hardware, camera calibration, lighting, surface texture, ARCore tracking stability, object geometry, placement method, and the selected material density.

---

## Current capabilities

| Capability | Status | Description |
| :--- | :--- | :--- |
| **ARCore camera background** | Implemented | Camera rendering through a zero-copy `GL_TEXTURE_EXTERNAL_OES` pipeline. |
| **OpenGL ES 3.0 graphics** | Implemented | Real-time 3D rendering of measurement lines (`GL_LINES`) and anchor points (`GL_POINTS`). |
| **Point-cloud filtering** | Implemented | Confidence thresholding, non-finite value rejection, and deterministic `PointCloud.close()` lifecycle. |
| **Raycasting and surface gating** | Implemented | Plane polygon gating, oriented feature-point validation, and optional `DepthPoint` recognition. |
| **Instant Placement fallback** | Implemented | Approximate placement through `LOCAL_Y_UP` when explicit placement is requested before conventional geometry converges. |
| **Frame-affine placement queue** | Implemented | UI requests are queued and executed against the current ARCore frame on the OpenGL rendering thread. |
| **Surface-probe throttling** | Implemented | Continuous conventional surface detection is limited to approximately 5 Hz. |
| **Native anchor tracking** | Implemented | ARCore `Anchor` poses are resolved frame by frame as the SLAM map evolves. |
| **Linear distance measurement** | Implemented | Euclidean distance with an explicit heuristic uncertainty model. |
| **Sequential three-axis volume** | Implemented | Guided width, height, and depth capture with approximate volume calculation in cubic meters and liters. |
| **Material-density selection** | Implemented | Explicit material selection using catalogued density and density uncertainty. |
| **Mass estimation by density** | Implemented | Approximate mass calculation with combined volume and density uncertainty. |
| **World-to-screen projection** | Implemented | Pure Kotlin MVP projection with homogeneous clipping and viewport mapping. |
| **Floating Compose badge** | Implemented | Reactive screen-space badge following the current measurement midpoint. |
| **Debug hit-test diagnostics** | Implemented | Debug-only `AetherisHitTest` logs for conventional hits, Instant Placement, and anchor creation. |
| **Depth API** | Disabled | `DepthMode.DISABLED` is retained because the test device reports native `ComputeDisparity` failures. |
| **Placement provenance in HUD** | Planned | Distinction between approximate Instant Placement and confirmed conventional geometry. |
| **Multipoint polylines** | Planned | Sequential multi-node boundary tracking. |
| **3D polygon area** | Planned | Coplanar surface-area calculation and mesh rendering. |
| **Structured export** | Planned | Measurement export in JSON and CSV formats. |

---

## Spatial measurement pipeline

```mermaid
flowchart TD
    A["Compose input<br/>normalized view coordinates"] --> B["Suspended placement request"]
    B --> C["Frame-affine queue"]
    C --> D["Current ARCore frame<br/>render thread"]
    D --> E{"Conventional hit valid?"}
    E -- Yes --> F["Plane, oriented Point,<br/>or DepthPoint"]
    E -- No --> G["Instant Placement fallback"]
    F --> H["Native ARCore Anchor"]
    G --> H
    H --> I["StateFlow spatial state"]
    I --> J["Distance, volume,<br/>and mass models"]
    I --> K["OpenGL geometry"]
    J --> L["Compose HUD"]
    K --> L
```

1. **Tracking:** ARCore estimates the six-degree-of-freedom camera pose through visual-inertial odometry.
2. **Point-cloud ingestion:** `ArCoreFrameProcessor` acquires, validates, filters, and closes each available point cloud.
3. **UI request:** The user requests an anchor using normalized view coordinates in the `[0, 1]` range.
4. **Frame-affine execution:** `SpatialSensorRepositoryImpl` suspends the caller and processes the request during a subsequent `onFrameUpdate(frame)` invocation on the render thread.
5. **Hit-test priority:** `ArCoreHitTestProcessor` prioritizes tracked planes inside their polygons, oriented feature points, and depth points.
6. **Approximate fallback:** When conventional geometry is unavailable, explicit placement may use an `InstantPlacementPoint` with an initial approximate distance.
7. **Native anchoring:** A successful hit is bound to an ARCore `Anchor`, whose pose is resolved on subsequent frames.
8. **Domain calculation:** Pure Kotlin use cases calculate distance, approximate three-axis volume, and density-based mass estimates.
9. **Rendering and projection:** OpenGL renders spatial geometry while the projection use case maps world coordinates into the Compose HUD.

---

## Mathematical models

### 1. Euclidean distance and heuristic uncertainty

For spatial points $P_1(x_1, y_1, z_1)$ and $P_2(x_2, y_2, z_2)$:

$$d = \sqrt{(x_2-x_1)^2 + (y_2-y_1)^2 + (z_2-z_1)^2}$$

The current distance uncertainty model is:

$$u_d = u_{\text{base}}\frac{1 + d/d_{\text{ref}}}{c_{\text{effective}}}$$

This is an engineering heuristic, not a device calibration certificate.

### 2. Three-axis volume and uncertainty propagation

The sequential width ($w$), height ($h$), and depth ($d$) measurements define an approximate bounding volume:

$$V = w \times h \times d$$

Assuming independent axis uncertainties:

$$u_V = \sqrt{(h \cdot d \cdot u_w)^2 + (w \cdot d \cdot u_h)^2 + (w \cdot h \cdot u_d)^2}$$

### 3. Density-based mass estimation

Given volume $V$ and selected material density $\rho$:

$$m = V \times \rho$$

Assuming independent volume and density uncertainties:

$$u_m = \sqrt{(\rho u_V)^2 + (V u_\rho)^2}$$

The result is an approximation. Hollow objects, mixed materials, irregular geometry, and an incorrect density selection can produce substantial differences from a scale measurement.

---

## Clean Architecture and project structure

The pure Kotlin domain is isolated from Android and ARCore APIs. Framework-specific behavior remains in the data and presentation layers.

| Layer | Path | Responsibility | Principal components |
| :--- | :--- | :--- | :--- |
| **Data / ARCore** | `data/arcore` | Session lifecycle, point-cloud processing, raycasting, and anchor creation | `ArCoreSessionManager`, `ArCoreFrameProcessor`, `ArCoreHitTestProcessor` |
| **Data / OpenGL** | `data/opengl` | Camera background and spatial geometry rendering | `BackgroundRenderer`, `SpatialLineRenderer` |
| **Data / Repository** | `data/repository` | Frame-affine request coordination and reactive spatial state | `SpatialSensorRepositoryImpl` |
| **Domain / Math** | `domain/math` | Framework-independent spatial line operations | `SpatialLineMath` |
| **Domain / Models** | `domain/model` | Immutable measurement, geometry, density, volume, and mass models | `Point3D`, `DistanceMeasurement`, `SpatialDimensions`, `VolumeMeasurement`, `MaterialDensity`, `MaterialDensityCatalog`, `MassEstimate` |
| **Domain / Repository** | `domain/repository` | Framework-independent spatial sensor contract | `SpatialSensorRepository` |
| **Domain / Use cases** | `domain/usecase` | Distance, projection, dimensions, volume, and mass calculations | `CalculateDistanceUseCase`, `ProjectWorldToScreenUseCase`, `EstimateSpatialDimensionsUseCase`, `CalculateVolumeUseCase`, `CalculateMassUseCase` |
| **Presentation / Components** | `presentation/components` | AR camera integration and reusable Compose interface elements | `ArCameraFeed`, `FloatingMeasurementBadge`, `MaterialDensitySelector` |
| **Presentation / Measurement** | `presentation/measurement` | UDF state, orchestration, and measurement screen | `MeasurementUiState`, `MeasurementViewModel`, `MeasurementScreen` |
| **Presentation / Permissions** | `presentation/permissions` | Declarative camera-permission flow | `CameraPermissionHandler` |
| **Dependency injection** | `di` | Application dependency graph | `AppModule` |

---

## Technology stack

- **Language and build:** Kotlin 2.0.20, Gradle Kotlin DSL, Android Gradle Plugin, and Version Catalogs.
- **Android configuration:** Min SDK 26, Target SDK 36, and JDK 17.
- **UI and state:** Jetpack Compose, Material 3, UDF, `StateFlow`, and ViewModel.
- **Spatial computing:** ARCore SDK 1.46.0, visual-inertial tracking, planes, point clouds, Instant Placement, and native anchors.
- **Graphics:** OpenGL ES 3.0, GLSL ES 3.0, external OES camera texture, and EGL/`GLSurfaceView` lifecycle management.
- **Dependency injection:** Koin with an application-level container and Activity-level dependency resolution.
- **Testing:** JUnit 4, MockK, Google Truth, and `kotlinx-coroutines-test`.
- **Quality and CI:** Android Lint, GitHub Actions, clean debug builds, and APK artifact generation.

---

## Testing and quality assurance

The current validation baseline includes:

- A passing JVM unit-test suite.
- Dedicated tests for domain models, mathematical operations, use cases, presentation state, ViewModel behavior, ARCore processors, and repository coordination.
- Android Lint passing without blocking errors.
- Successful clean debug APK assembly.
- Physical-device validation of approximate and conventional anchor placement.

Run the complete verification pipeline:

```bash
./gradlew clean testDebugUnitTest lintDebug assembleDebug \
  --no-configuration-cache
```

Physical-device diagnostics confirmed successful approximate placement:

```text
InstantPlacementPoint
tracking=TRACKING
method=SCREENSPACE_WITH_APPROXIMATE_DISTANCE
anchor created successfully
```

After visual tracking matured, ARCore also returned a valid conventional oriented feature point.

---

## Architecture Decision Records

The ADR registry is available in [`docs/adr`](docs/adr/README.md).

The first standalone record is:

- [`ADR-016: Frame-Affine Placement Queue and Instant Placement Fallback`](docs/adr/ADR-016-frame-affine-placement.md)

Decisions ADR-001 through ADR-015 were originally recorded in `DEVLOG.md` and are being migrated gradually into standalone documents. The registry reports their migration status explicitly.

---

## Development log

Detailed development history, device diagnostics, mathematical derivations, and regression notes are available in [`DEVLOG.md`](DEVLOG.md).

---

## Build and installation

### Prerequisites

- JDK 17.
- Android Studio or another Android/Gradle-compatible development environment.
- Android SDK 36.
- A physical ARCore-compatible Android device for spatial functionality.
- USB debugging for command-line installation.

### Commands

```bash
# Clone the repository
git clone https://github.com/maurizioprizzi/aetheris.git
cd aetheris

# Ensure executable permissions
chmod +x gradlew

# Run the complete verification pipeline
./gradlew clean testDebugUnitTest lintDebug assembleDebug \
  --no-configuration-cache

# Install on a connected device
./gradlew installDebug
```

---

## Known limitations

- Measurements depend on ARCore tracking quality and the camera configuration of each device.
- Instant Placement begins with an approximate distance and can update pose or apparent scale as tracking improves.
- The interface does not yet distinguish approximate placement from confirmed conventional geometry.
- The current volume model is a three-axis bounding approximation, not object segmentation or mesh reconstruction.
- Mass depends directly on the selected density and assumes the measured volume is occupied by that material.
- Hollow, articulated, deformable, reflective, transparent, or low-texture objects can produce inaccurate results.
- Depth remains disabled on the current test configuration because of native device/runtime failures.
- 16 KB page-size compatibility is still under explicit release-artifact validation.
- The uncertainty models are engineering estimates and have not yet been validated through a controlled calibration study.

---

## Roadmap

- Propagate placement provenance into the domain and repository state.
- Distinguish approximate placement from confirmed conventional geometry in the HUD.
- Validate repeated Activity and ARCore session pause/resume cycles.
- Run controlled measurements against objects with known dimensions, volume, density, and mass.
- Quantify repeatability, bias, and sensitivity to viewing distance, lighting, and surface texture.
- Validate 16 KB page-size compatibility against the release artifact.
- Add structured measurement export.
- Explore multipoint boundaries, surface area, and mesh-based volume estimation.
- Migrate ADR-001 through ADR-015 into standalone architecture records.

---

## License

Aetheris is distributed under the [Apache License 2.0](LICENSE).

```text
Copyright 2026 Maurizio Prizzi
```