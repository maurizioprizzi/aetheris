# ADR-016: Frame-Affine Placement Queue and Instant Placement Fallback

- **Status:** Accepted and implemented
- **Decision date:** 2026-09-01
- **Project:** Aetheris
- **Scope:** ARCore spatial placement pipeline

## Context

Aetheris receives ARCore frames on the OpenGL rendering thread while user interactions originate from the Jetpack Compose presentation layer.

Hit tests and anchor creation depend on a valid, current ARCore `Frame`. Retaining a frame and using it later from another coroutine or thread can introduce invalid native state, race conditions, session lifecycle failures, or operations against stale camera geometry.

The initial implementation shared the latest frame with requests initiated by the `MeasurementViewModel`. Although camera tracking and point-cloud telemetry were active, placement operations frequently returned no usable result.

Physical-device diagnostics demonstrated another constraint: conventional hit testing may return no plane or oriented feature point while ARCore is still building sufficient environmental geometry. This made the interface appear noninteractive even when camera tracking was active.

The project therefore required:

1. Strict frame and thread affinity for ARCore placement operations.
2. Safe coordination between UI requests and the rendering thread.
3. A controlled fallback while conventional geometry is unavailable.
4. Protection against excessive hit testing during continuous rendering.
5. Explicit treatment of approximate placement as a provisional result.

## Decision

Aetheris processes spatial placement requests through a frame-affine queue owned by `SpatialSensorRepositoryImpl`.

The presentation layer submits a request without directly accessing or retaining an ARCore frame. The request is suspended while awaiting processing.

During the next rendering update, `onFrameUpdate(frame)` processes pending requests using the current frame on the OpenGL rendering thread.

The repository then completes the suspended request with its result.

## Request processing model

The implemented flow is:

1. The user requests a hit test or anchor placement.
2. The repository validates normalized screen coordinates.
3. A one-shot spatial request is added to a synchronized queue.
4. The calling coroutine waits for completion with bounded lifetime.
5. The OpenGL rendering thread receives the next current ARCore frame.
6. `onFrameUpdate(frame)` drains the pending requests.
7. Pixel coordinates are calculated from the current viewport.
8. The hit test or anchor creation is executed against that frame.
9. The result is returned to the suspended caller.
10. Cancelled or expired requests are discarded safely.

No ARCore `Frame` is retained as shared state for later use by the main thread.

## Hit-test priority

Aetheris always prioritizes conventional ARCore geometry:

1. A tracked `Plane` whose hit pose is inside its polygon.
2. A tracked `Point` with an estimated surface normal.
3. A tracked `DepthPoint`, when depth information is available.
4. An `InstantPlacementPoint` only when conventional geometry is unavailable and the operation was explicitly requested by the user.

This ordering preserves the best available spatial evidence while maintaining usable interaction during early environmental mapping.

## Instant Placement policy

Instant Placement is enabled with `Config.InstantPlacementMode.LOCAL_Y_UP`.

The fallback uses an initial approximate distance of 1.5 meters.

Instant Placement is allowed only for explicit hit-test and anchor-placement requests.

It is not used by continuous surface probing because an approximate result must not be presented as a confirmed physical surface.

The pose of an `InstantPlacementPoint` may change as ARCore obtains better environmental geometry. Measurements based on approximate placement must therefore be treated as provisional.

## Surface-probe throttling

Continuous surface detection is throttled to approximately five probes per second.

This reduces repeated native hit-test work while preserving responsive visual feedback in the Compose interface.

The throttled probe uses only conventional geometry and does not activate Instant Placement.

## Anchor ownership

Native ARCore anchors remain owned by the repository.

When an anchor slot is replaced or the measurement is reset, the previous anchor is detached before its reference is discarded.

This prevents native resource leaks and keeps the reactive spatial state consistent with active ARCore resources.

## Depth configuration

This decision does not enable ARCore Depth.

Depth remains disabled in the current device configuration because the native pipeline produced `ComputeDisparity` failures despite reporting support for automatic depth mode.

Plane detection, feature points, anchors, point clouds, conventional hit testing, and Instant Placement remain available without Depth.

## Alternatives considered

### Retain the latest frame in shared state

Rejected because an ARCore frame represents native session state associated with a specific update cycle. Reusing it asynchronously can operate on stale or invalid data.

### Execute placement directly from the ViewModel

Rejected because the ViewModel should not own ARCore objects or depend on rendering-thread timing.

### Move ARCore placement to `Dispatchers.Main`

Rejected because changing coroutine dispatchers does not guarantee that the operation uses the current frame on the rendering thread.

### Require a confirmed plane before enabling placement

Rejected because plane discovery can take significant time or fail on low-texture geometry, making the interface appear unavailable even while tracking is active.

### Use Instant Placement for continuous surface detection

Rejected because it would make approximate geometry appear equivalent to a conventionally confirmed surface and would increase native processing frequency.

### Re-enable automatic Depth mode

Rejected for the current configuration because physical-device testing exposed failures in the native disparity pipeline.

## Consequences

### Positive consequences

- ARCore operations use the current frame on the correct rendering thread.
- UI actions no longer depend on unsafe shared-frame state.
- Placement remains available before conventional plane discovery completes.
- Conventional geometry retains priority over approximate placement.
- Pending operations have explicit cancellation and timeout behavior.
- Surface probing produces less native processing overhead.
- Anchor lifecycle management remains centralized in the repository.
- The architecture preserves separation between presentation, domain, and ARCore infrastructure.

### Negative consequences

- The repository requires synchronized request coordination.
- A placement request may wait until a subsequent render frame.
- Instant Placement introduces provisional distance and pose estimates.
- Approximate anchors can move as ARCore improves tracking.
- The current interface does not yet clearly distinguish approximate placement from confirmed conventional geometry.
- Additional physical calibration is required before uncertainty values can be considered experimentally validated.

## Validation

The implementation was validated with:

- JVM unit tests for repository request processing and anchor lifecycle.
- Android lint analysis.
- Debug APK assembly.
- Physical-device testing with filtered `AetherisHitTest` diagnostics.
- Successful creation of anchors from tracked `InstantPlacementPoint` results.
- Subsequent successful conventional placement after ARCore produced a valid oriented feature point.
- End-to-end interaction through the measurement interface.

The validation confirms functional behavior and thread coordination. It does not establish certified metrological accuracy.

## Follow-up work

- Propagate placement provenance into the domain state.
- Distinguish approximate and conventionally confirmed anchors in the HUD.
- Observe whether provisional anchors converge to improved tracking geometry.
- Measure objects with known dimensions at multiple viewing distances.
- Quantify repeatability, systematic bias, and sensitivity to lighting and surface texture.
- Reassess Depth only after device/runtime compatibility is independently confirmed.

## Related implementation

- [`ArCoreSessionManager.kt`](../../app/src/main/java/org/aetheris/app/data/arcore/ArCoreSessionManager.kt)
- [`ArCoreHitTestProcessor.kt`](../../app/src/main/java/org/aetheris/app/data/arcore/ArCoreHitTestProcessor.kt)
- [`SpatialSensorRepositoryImpl.kt`](../../app/src/main/java/org/aetheris/app/data/repository/SpatialSensorRepositoryImpl.kt)
- [`SpatialSensorRepository.kt`](../../app/src/main/java/org/aetheris/app/domain/repository/SpatialSensorRepository.kt)
- [`MeasurementViewModel.kt`](../../app/src/main/java/org/aetheris/app/presentation/measurement/MeasurementViewModel.kt)
- [`SpatialSensorRepositoryTest.kt`](../../app/src/test/java/org/aetheris/app/data/repository/SpatialSensorRepositoryTest.kt)
