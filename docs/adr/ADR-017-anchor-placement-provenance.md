# ADR-017: Anchor Placement Provenance and Approximation Semantics

- **Status:** Accepted and implemented
- **Decision date:** 2026-09-02
- **Project:** Aetheris
- **Scope:** Spatial placement domain, ARCore data pipeline, repository state, and presentation state

## Context

Aetheris can create measurement anchors from more than one kind of spatial evidence supplied by ARCore.

Conventional hit testing may select:

1. A tracked plane whose hit pose lies inside its polygon.
2. A tracked feature point with an estimated surface normal.
3. A tracked depth point when depth information is available.

When conventional geometry is unavailable, an explicit user placement request may use an `InstantPlacementPoint`. This fallback provides an initial position based on an approximate distance and may refine its pose as ARCore gathers more environmental evidence.

Before this decision, the placement pipeline exposed only a `Point3D` or native `Anchor`. Once the result left `ArCoreHitTestProcessor`, the application could no longer determine which spatial method had produced it.

This loss of information created several problems:

- Conventional and approximate positions appeared equivalent to downstream consumers.
- The presentation layer could not warn the user about provisional placement.
- Future exports could not describe the evidence behind a measurement.
- Tests could verify that an anchor existed, but not why it had been accepted.
- Confidence and acceptance policies could not vary by placement method.

The application therefore required an explicit, framework-independent representation of spatial provenance.

## Decision

Aetheris represents the origin of every classified placement with the pure Kotlin enum `AnchorPlacementSource`.

The supported values are:

| Source | Category | Meaning |
|---|---|---|
| `PLANE` | Conventional | The hit belongs to the polygon of a tracked ARCore plane. |
| `FEATURE_POINT` | Conventional | The hit belongs to a tracked point with an estimated surface normal. |
| `DEPTH_POINT` | Conventional | The hit is supported by ARCore depth information. |
| `INSTANT_PLACEMENT` | Approximate | The initial hit uses an estimated distance and may refine over time. |

The domain model `AnchorPlacement` associates:

- a world-space `Point3D` position;
- an `AnchorPlacementSource` value.

Neither model depends on Android or ARCore classes.

## Classification boundary

Native ARCore trackables are classified inside `ArCoreHitTestProcessor`, at the boundary where their concrete types are still available.

The mapping is:

```text
Plane                 -> PLANE
Point                 -> FEATURE_POINT
DepthPoint            -> DEPTH_POINT
InstantPlacementPoint -> INSTANT_PLACEMENT
```

This classification occurs only after the existing validity checks have succeeded.

A tracked plane must still contain the hit pose inside its polygon. A feature point must still report `ESTIMATED_SURFACE_NORMAL`. All selected trackables must still satisfy their applicable tracking requirements.

Provenance does not weaken hit validation. It records which validated path produced the result.

## Hit-test priority

Conventional spatial evidence retains priority over Instant Placement.

The selection order remains:

1. Valid conventional hit returned by `Frame.hitTest`.
2. Valid Instant Placement hit only when conventional geometry is unavailable and the operation explicitly allows the fallback.

Continuous surface probing never uses Instant Placement. The approximate fallback is limited to explicit placement operations initiated by the user.

## API evolution and compatibility

The enriched APIs are:

- `performHitTestWithSource`, returning an `AnchorPlacement`.
- `createAnchorAtWithSource`, returning an `ArCoreAnchorPlacement` containing the native anchor and its source.

The previous APIs remain available:

- `performHitTest`, returning only `Point3D`.
- `createAnchorAt`, returning only `Anchor`.

The legacy methods delegate to the enriched implementations and discard the source only for callers that still use the former contract.

This approach allows incremental migration without duplicating the hit-test algorithm or breaking existing consumers.

## Repository ownership

`SpatialSensorRepositoryImpl` stores every active native anchor together with its original source.

The internal representation treats these values as a logical pair:

```text
ActiveAnchor
â”œâ”€â”€ native Anchor
â””â”€â”€ AnchorPlacementSource
```

The source belongs to the placement event. Pose graph updates may change the world-space position resolved from the native anchor, but they do not change the method that originally created it.

## Reactive propagation

Placement position and provenance follow the same reactive pipeline:

```text
ARCore trackable
    -> ArCoreHitTestProcessor
    -> SpatialSensorRepositoryImpl
    -> SpatialFrameData
    -> MeasurementViewModel
    -> MeasurementUiState
```

`SpatialFrameData` exposes separate source properties for the active start and end anchors while preserving its existing point properties.

`MeasurementViewModel` transfers each point and source within the same immutable state update.

`MeasurementUiState` can reconstruct complete `AnchorPlacement` values and derive whether the active measurement:

- has complete provenance;
- contains at least one approximate placement;
- contains only conventional placements;
- should display an approximate-placement warning.

## State invariants

The following invariants apply:

1. A source cannot exist without its corresponding point.
2. A point may temporarily exist without a source for backward compatibility during migration.
3. Start and end positions retain independent sources.
4. Position and source are published together by production code.
5. An empty measurement has neither positions nor sources.
6. Confirmation, discard, reset, replacement, rollback, and terminal cleanup remove obsolete provenance.

These rules prevent states such as an `INSTANT_PLACEMENT` source being associated with no spatial coordinate.

## Anchor lifecycle semantics

Provenance follows the lifecycle of its native anchor:

- **Tracking:** The current pose is resolved and the original source is retained.
- **Paused:** The last known point and its source are preserved while tracking may recover.
- **Stopped:** The point and source are removed and the native anchor is detached.
- **Replacement:** The previous anchor is detached before the replacement and its source are stored.
- **Rollback or failure:** Any newly created but uncommitted anchor is detached and no provenance is published.
- **Explicit clear:** Both active anchors, their positions, and their sources are removed.

## Approximation semantics

`INSTANT_PLACEMENT` means that the placement began from approximate spatial evidence.

It does not mean:

- that the point is invalid;
- that ARCore tracking is unavailable;
- that the anchor cannot refine its pose;
- that the measurement must automatically be rejected.

The current product policy permits the user to confirm a dimension containing an approximate point. The state exposes this condition so the Compose HUD can communicate it explicitly in a subsequent change.

Even if the native anchor pose improves over time, its original source remains `INSTANT_PLACEMENT`. A future quality model may record refinement state separately rather than rewriting historical provenance.

## Distance recalculation policy

Distance depends on the two world-space positions, not on their source labels.

`MeasurementViewModel` therefore recalculates `DistanceMeasurement` only when an anchor position changes or no previous measurement exists.

A provenance-only update is propagated to the UI state without triggering an unnecessary metric calculation.

## Consequences

### Positive

- Approximate and conventional placement are no longer semantically indistinguishable.
- The domain remains independent of ARCore and Android classes.
- The repository preserves provenance across pose updates and temporary tracking pauses.
- The presentation state can drive an honest approximation warning.
- Tests can verify both spatial results and their evidence source.
- Future persistence, export, calibration, and confidence policies have a stable source model.
- Existing callers can migrate incrementally through the preserved legacy APIs.

### Tradeoffs

- Spatial state contains additional nullable properties during the compatibility period.
- The source describes the original placement method, not the current refinement quality.
- Confirmed `SpatialDimensions` do not yet retain the provenance of the anchors used to produce each dimension.
- The current Compose HUD does not yet render the warning exposed by `MeasurementUiState`.
- Provenance improves transparency but does not independently quantify measurement accuracy.

## Alternatives considered

### Infer approximation from `isSurfaceDetected`

Rejected because surface probing describes the current reticle state, not the historical source of an already-created anchor.

### Expose ARCore `Trackable` types throughout the application

Rejected because it would couple domain and presentation models to the Android ARCore SDK and make JVM testing more difficult.

### Convert Instant Placement to a conventional source after pose refinement

Rejected for the current stage because refinement quality is not equivalent to knowing that a new conventional hit created the anchor. Historical provenance and current confidence are separate concepts.

### Block every approximate measurement

Rejected as the initial policy because Instant Placement is intentionally used to preserve interaction while conventional geometry is still converging. The application instead records the approximation so the interface and future policies can respond transparently.

## Validation

Unit tests cover:

- every `AnchorPlacementSource` classification;
- conventional and approximate derived properties;
- immutable association between `Point3D` and source;
- conventional-hit priority over Instant Placement;
- legacy API compatibility;
- propagation through `SpatialFrameData`;
- repository storage, pause handling, pose updates, replacement, detachment, and clearing;
- ViewModel propagation and provenance-only updates;
- presentation-state invariants and approximation indicators;
- clearing provenance after confirmation, discard, and reset;
- regression of distance, volume, density, and mass flows.

The complete project pipeline was executed after integration:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug \
  --no-configuration-cache
```

Result:

```text
BUILD SUCCESSFUL in 15s
53 actionable tasks: 12 executed, 41 up-to-date
```

## Follow-up work

1. Render the placement source and approximation warning in the Compose HUD.
2. Define whether confirmation should require explicit acknowledgement for approximate measurements.
3. Preserve source metadata with each confirmed width, height, and depth measurement.
4. Include provenance in future JSON and CSV exports.
5. Introduce a separate refinement or confidence model if physical validation supports it.
6. Compare repeatability and bias across placement sources using objects with known dimensions.