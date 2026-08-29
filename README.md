# Aetheris

[![Android CI](https://github.com/maurizioprizzi/aetheris/actions/workflows/android.yml/badge.svg)](https://github.com/maurizioprizzi/aetheris/actions/workflows/android.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-purple.svg)](https://kotlinlang.org)
[![ARCore](https://img.shields.io/badge/ARCore-1.46.0-blue.svg)](https://developers.google.com/ar)
[![OpenGL ES](https://img.shields.io/badge/OpenGL_ES-3.0-orange.svg)](https://developer.android.com/guide/topics/graphics/opengl)
[![16 KB Page Size](https://img.shields.io/badge/16_KB_Page_Size-Configured-brightgreen.svg)](https://developer.android.com/16kb-page-size)
[![Min SDK](https://img.shields.io/badge/Min_SDK-26-green.svg)](https://developer.android.com)

**Aetheris** is an open-source experimental spatial measurement platform for Android.

It combines ARCore tracking, native anchors, spatial raycasting, point-cloud filtering, OpenGL ES 3.0 rendering and Jetpack Compose to measure and visualize distances in three-dimensional environments.

The project is also an exploration of mobile spatial computing, real-time graphics, reactive architecture and uncertainty-aware physical measurements.

> [!IMPORTANT]
> Aetheris is not a certified metrological instrument. Measurements depend on device hardware, camera calibration, environmental conditions and ARCore tracking quality.

---

## Current capabilities

| Capability | Status |
| --- | --- |
| ARCore camera background | Implemented |
| OpenGL ES 3.0 rendering | Implemented |
| Point-cloud confidence filtering | Implemented |
| Plane and feature-point hit testing | Implemented |
| Native ARCore anchor placement | Implemented |
| Two-point spatial distance | Implemented |
| World-to-screen projection | Implemented |
| Floating Compose measurement badge | Implemented |
| Heuristic uncertainty estimation | Implemented |
| AABB spatial dimensions | Domain implementation |
| Depth API | Temporarily disabled |
| Multipoint measurements | Planned |
| Polygon area measurement | Planned |
| JSON and CSV export | Planned |

---

## Spatial measurement pipeline

Aetheris performs a measurement through the following pipeline:

1. ARCore tracks the camera pose using visual-inertial odometry.
2. The application acquires and filters the latest point cloud.
3. A screen coordinate is converted from normalized viewport space to pixels.
4. `Frame.hitTest()` searches for a valid physical intersection.
5. The result is validated against tracked planes, oriented feature points or depth points.
6. A native ARCore `Anchor` is created for the selected position.
7. Anchor poses are observed continuously as ARCore refines its world map.
8. The Euclidean distance between the two anchored points is calculated.
9. OpenGL renders the anchors and the spatial line.
10. The line midpoint is projected into screen space.
11. Jetpack Compose displays the measurement badge at the projected position.

---

## Core engineering features

### Native ARCore anchor tracking

Measurement endpoints are associated with native ARCore `Anchor` objects instead of storing only a static coordinate.

As ARCore refines its pose graph, Aetheris reads the updated anchor poses and propagates them through the spatial state stream. This reduces visible displacement during longer tracking sessions.

Anchors are detached deterministically when replaced, cleared or stopped.

### Spatial raycasting

Hit tests are validated against supported ARCore trackables:

- Tracked planes whose hit poses are inside their polygons.
- Tracked feature points with `ESTIMATED_SURFACE_NORMAL`.
- Tracked `DepthPoint` results when available.

Invalid coordinates, paused tracking and unavailable frames are rejected safely.

### Point-cloud filtering

Each ARCore point contains four values:

```text
x, y, z, confidence
```

`ArCoreFrameProcessor` discards points with:

- Confidence below the configured threshold.
- Non-finite coordinates.
- Non-finite confidence values.

The acquired `PointCloud` is always closed through Kotlin's `use` mechanism.

### World-to-screen projection

World coordinates are transformed through the complete graphics pipeline:

```text
World space
    ↓ View matrix
Camera space
    ↓ Projection matrix
Clip space
    ↓ Perspective division
Normalized Device Coordinates
    ↓ Viewport mapping
Screen pixels
```

The projection validates matrix contents, homogeneous clip coordinates, frustum boundaries and viewport dimensions before exposing a visible screen position.

### OpenGL ES 3.0 rendering

The rendering layer uses:

- `GL_TEXTURE_EXTERNAL_OES` for the camera image.
- GLSL ES 3.0 vertex and fragment shaders.
- `GL_LINES` for measurement vectors.
- `GL_POINTS` for anchor markers.
- VAOs and VBOs for GPU-side geometry.
- Direct `FloatBuffer` instances.
- Preallocated matrices and vertex arrays.
- Explicit OpenGL resource destruction.

The renderers minimize allocations inside the frame loop and restore required OpenGL bindings after drawing.

### Uncertainty-aware distance model

Distance is calculated using the Euclidean norm:

\[
d = \sqrt{(x_2-x_1)^2 + (y_2-y_1)^2 + (z_2-z_1)^2}
\]

Aetheris also produces a heuristic uncertainty estimate:

\[
u = u_{\text{base}}
\cdot
\frac{1 + d / d_{\text{ref}}}
{c_{\text{effective}}}
\]

This model communicates that uncertainty tends to increase with distance and reduced tracking confidence.

It is an engineering estimate and does not represent certified calibration of the device.

---

## Architecture

Aetheris follows Clean Architecture principles and Unidirectional Data Flow.

```text
org.aetheris.app
├── data
│   ├── arcore
│   │   ├── ArCoreSessionManager
│   │   ├── ArCoreFrameProcessor
│   │   └── ArCoreHitTestProcessor
│   ├── opengl
│   │   ├── BackgroundRenderer
│   │   └── SpatialLineRenderer
│   └── repository
│       └── SpatialSensorRepositoryImpl
├── domain
│   ├── math
│   │   └── SpatialLineMath
│   ├── model
│   │   ├── AnchorSlot
│   │   ├── BoundingBox3D
│   │   ├── DistanceMeasurement
│   │   ├── MassEstimate
│   │   ├── Point3D
│   │   ├── ScreenPoint2D
│   │   ├── SpatialFrameData
│   │   └── TrackingStatus
│   ├── repository
│   │   └── SpatialSensorRepository
│   └── usecase
│       ├── CalculateDistanceUseCase
│       ├── EstimateSpatialDimensionsUseCase
│       └── ProjectWorldToScreenUseCase
├── presentation
│   ├── components
│   ├── measurement
│   └── permissions
└── di
    └── AppModule
```

### Domain layer

The mathematical domain is written in pure Kotlin and has no dependency on the Android SDK or ARCore.

This allows calculations and state rules to be tested directly on the JVM.

### Data layer

The data layer contains all integration with:

- ARCore sessions and frames.
- Point clouds.
- Hit testing.
- Native anchors.
- OpenGL resources.
- Camera textures.

Framework-specific objects do not leak into the domain layer.

### Presentation layer

The interface is implemented with Jetpack Compose and observes immutable state through `StateFlow`.

User actions are sent to `MeasurementViewModel`, which coordinates the domain and repository layers.

---

## Technology stack

- Kotlin
- Android SDK
- Jetpack Compose
- Material 3
- ARCore
- OpenGL ES 3.0
- GLSL ES 3.0
- Kotlin Coroutines
- StateFlow
- Koin
- JUnit 4
- MockK
- Google Truth
- kotlinx-coroutines-test
- Gradle Kotlin DSL
- GitHub Actions

---

## Depth API status

The current configuration intentionally uses:

```kotlin
depthMode = Config.DepthMode.DISABLED
```

The development device reports support for automatic depth, but its native pipeline produced an internal `ComputeDisparity` failure.

Disabling the Depth API preserves the stable operation of:

- Plane detection.
- Feature-point hit tests.
- Native anchors.
- Point-cloud processing.
- Distance measurement.
- OpenGL rendering.

Depth support remains represented in the architecture and can be re-enabled after validation across compatible devices.

---

## Testing

The project currently has a green baseline of:

```text
32 tests executed
0 failures
BUILD SUCCESSFUL
```

The test suite covers:

- Distance calculations.
- Mathematical models.
- Spatial projection.
- Point-cloud filtering.
- Hit-test validation.
- Plane polygon checks.
- Feature-point orientation.
- Anchor creation and replacement.
- Anchor cleanup.
- Repository state updates.
- Measurement ViewModel behavior.
- Coroutine-based UI state transitions.

---

## Continuous integration

GitHub Actions runs the following checks for every push and pull request targeting `main`:

1. JVM unit tests.
2. Android Lint.
3. Debug APK compilation.
4. Test-report upload when a failure occurs.
5. Debug APK upload as a workflow artifact.

Workflow file:

```text
.github/workflows/android.yml
```

---

## Requirements

To build Aetheris locally, you need:

- JDK 17.
- Android Studio with Android SDK support.
- An Android SDK compatible with the project configuration.
- An ARCore-supported physical Android device.
- USB debugging enabled for direct installation.

A physical device is recommended because ARCore tracking, anchors and camera behavior cannot be completely validated through ordinary JVM tests.

---

## Build and verification

Clone the repository:

```bash
git clone https://github.com/maurizioprizzi/aetheris.git
cd aetheris
```

Grant execution permission to the Gradle Wrapper when necessary:

```bash
chmod +x gradlew
```

Run unit tests:

```bash
./gradlew testDebugUnitTest --no-configuration-cache
```

Run Android Lint:

```bash
./gradlew lintDebug --no-configuration-cache
```

Build the debug APK:

```bash
./gradlew assembleDebug --no-configuration-cache
```

Run the complete local verification pipeline:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug \
    --no-configuration-cache
```

Install the debug build on a connected device:

```bash
./gradlew installDebug
```

The generated APK is available at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## Architecture Decision Records

Technical and architectural decisions are documented under [`docs/adr`](docs/adr).

- `ADR-001`: Adoption of Koin over Hilt/Dagger
- `ADR-002`: Apache 2.0 and Open Core strategy
- `ADR-003`: Pure Kotlin mathematical domain
- `ADR-004`: First-class measurement uncertainty
- `ADR-005`: Reactive spatial repository
- `ADR-006`: Hit-testing and raycasting abstraction
- `ADR-007`: Point-cloud confidence filtering
- `ADR-008`: Unidirectional Data Flow with StateFlow
- `ADR-009`: Declarative camera permission handling
- `ADR-010`: ARCore lifecycle and EGL context isolation
- `ADR-011`: Spatial raycasting and convex polygon gating
- `ADR-012`: OES camera texture and OpenGL geometry pipeline
- `ADR-013`: Native anchor tracking and pose graph correction
- `ADR-014`: Defensive ARCore/OpenGL resource management and Depth fallback

---

## Development log

The engineering history, debugging process, validation results and architectural evolution are recorded in [`DEVLOG.md`](DEVLOG.md).

---

## Roadmap

### Next

- Multipoint measurements.
- Three-dimensional polylines.
- Coplanar polygon area calculation.
- Translucent polygon rendering.
- JSON and CSV measurement export.

### Future research

- Device-specific calibration profiles.
- Confidence models based on empirical measurements.
- Depth API compatibility profiles.
- Plane-oriented dimensions instead of world-axis-only AABB.
- Measurement session persistence.
- Reproducible calibration datasets.
- On-device validation reports.

---

## Known limitations

- Accuracy depends on ARCore tracking quality and device calibration.
- Reflective, transparent or textureless surfaces can reduce stability.
- Fast camera movement may temporarily interrupt tracking.
- Low-light environments can reduce point-cloud quality.
- Current AABB dimensions are aligned to world axes.
- Depth mode is disabled in the current runtime configuration.
- The uncertainty model is heuristic and has not been certified.
- ARCore frames and GPU behavior require validation on physical devices.

---

## Contributing

Contributions, technical discussions and reproducible device reports are welcome.

When reporting a device-specific issue, include:

- Device manufacturer and model.
- Android version.
- ARCore version.
- Relevant Logcat output.
- Steps required to reproduce the issue.
- Whether Depth support was enabled or disabled.

Before opening a pull request, run:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug \
    --no-configuration-cache
```

---

## License

Aetheris is distributed under the [Apache License 2.0](LICENSE).

Copyright © 2026 Maurizio Prizzi