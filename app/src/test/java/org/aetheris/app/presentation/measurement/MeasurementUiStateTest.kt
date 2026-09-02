package org.aetheris.app.presentation.measurement

import com.google.common.truth.Truth.assertThat
import org.aetheris.app.domain.model.AnchorPlacementSource
import org.aetheris.app.domain.model.AnchorSlot
import org.aetheris.app.domain.model.DimensionAxis
import org.aetheris.app.domain.model.DistanceMeasurement
import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.ScreenPoint2D
import org.aetheris.app.domain.model.SpatialDimensions
import org.aetheris.app.domain.model.TrackingStatus
import org.aetheris.app.domain.model.VolumeMeasurement
import org.junit.Assert.assertThrows
import org.junit.Test

class MeasurementUiStateTest {

    @Test
    fun `initial state starts width measurement`() {
        val state =
            MeasurementUiState()

        assertThat(state.currentDimensionAxis)
            .isEqualTo(DimensionAxis.WIDTH)

        assertThat(state.measuredDimensionCount)
            .isEqualTo(0)

        assertThat(state.hasPendingDimension)
            .isTrue()

        assertThat(state.hasCompleteSpatialDimensions)
            .isFalse()

        assertThat(state.volumeMeasurement)
            .isNull()
    }

    @Test
    fun `initial state has no placement provenance`() {
        val state =
            MeasurementUiState()

        assertThat(state.selectedStartSource)
            .isNull()

        assertThat(state.selectedEndSource)
            .isNull()

        assertThat(state.selectedStartPlacement)
            .isNull()

        assertThat(state.selectedEndPlacement)
            .isNull()

        assertThat(state.hasCompletePlacementProvenance)
            .isFalse()

        assertThat(state.hasApproximatePlacement)
            .isFalse()

        assertThat(state.hasOnlyConventionalPlacements)
            .isFalse()

        assertThat(state.shouldShowApproximatePlacementWarning)
            .isFalse()
    }

    @Test
    fun `state requests start anchor when no points exist`() {
        val state =
            MeasurementUiState(
                trackingStatus =
                    TrackingStatus.TRACKING,
                isTargetingSurface = true
            )

        assertThat(state.nextAnchorSlot)
            .isEqualTo(AnchorSlot.START)

        assertThat(state.canPlaceAnchor)
            .isTrue()
    }

    @Test
    fun `state requests end anchor after start point exists`() {
        val state =
            MeasurementUiState(
                trackingStatus =
                    TrackingStatus.TRACKING,
                isTargetingSurface = true,
                selectedStartPoint =
                    point(
                        x = 0f,
                        y = 0f,
                        z = 0f
                    )
            )

        assertThat(state.nextAnchorSlot)
            .isEqualTo(AnchorSlot.END)

        assertThat(state.anchorCount)
            .isEqualTo(1)

        assertThat(state.canPlaceAnchor)
            .isTrue()
    }

    @Test
    fun `state builds start placement from point and source`() {
        val startPoint =
            point(
                x = 1f,
                y = 2f,
                z = 3f
            )

        val state =
            MeasurementUiState(
                selectedStartPoint = startPoint,
                selectedStartSource =
                    AnchorPlacementSource.PLANE
            )

        val placement =
            state.selectedStartPlacement

        assertThat(placement)
            .isNotNull()

        assertThat(placement?.position)
            .isEqualTo(startPoint)

        assertThat(placement?.source)
            .isEqualTo(
                AnchorPlacementSource.PLANE
            )

        assertThat(state.selectedEndPlacement)
            .isNull()

        assertThat(state.hasCompletePlacementProvenance)
            .isTrue()

        assertThat(state.hasOnlyConventionalPlacements)
            .isTrue()
    }

    @Test
    fun `state builds end placement from point and source`() {
        val endPoint =
            point(
                x = 4f,
                y = 5f,
                z = 6f
            )

        val state =
            MeasurementUiState(
                selectedEndPoint = endPoint,
                selectedEndSource =
                    AnchorPlacementSource.FEATURE_POINT
            )

        val placement =
            state.selectedEndPlacement

        assertThat(placement)
            .isNotNull()

        assertThat(placement?.position)
            .isEqualTo(endPoint)

        assertThat(placement?.source)
            .isEqualTo(
                AnchorPlacementSource.FEATURE_POINT
            )

        assertThat(state.selectedStartPlacement)
            .isNull()

        assertThat(state.hasCompletePlacementProvenance)
            .isTrue()

        assertThat(state.hasOnlyConventionalPlacements)
            .isTrue()
    }

    @Test
    fun `point without source preserves backward compatibility`() {
        val state =
            MeasurementUiState(
                selectedStartPoint =
                    point(
                        x = 1f,
                        y = 2f,
                        z = 3f
                    )
            )

        assertThat(state.hasStartPoint)
            .isTrue()

        assertThat(state.selectedStartPlacement)
            .isNull()

        assertThat(state.hasCompletePlacementProvenance)
            .isFalse()

        assertThat(state.hasApproximatePlacement)
            .isFalse()

        assertThat(state.hasOnlyConventionalPlacements)
            .isFalse()
    }

    @Test
    fun `instant placement marks current measurement as approximate`() {
        val state =
            MeasurementUiState(
                selectedStartPoint =
                    point(
                        x = 0f,
                        y = 0f,
                        z = 0f
                    ),
                selectedStartSource =
                    AnchorPlacementSource.PLANE,
                selectedEndPoint =
                    point(
                        x = 1f,
                        y = 0f,
                        z = 0f
                    ),
                selectedEndSource =
                    AnchorPlacementSource.INSTANT_PLACEMENT
            )

        assertThat(state.hasCompletePlacementProvenance)
            .isTrue()

        assertThat(state.hasApproximatePlacement)
            .isTrue()

        assertThat(state.hasOnlyConventionalPlacements)
            .isFalse()

        assertThat(state.shouldShowApproximatePlacementWarning)
            .isTrue()
    }

    @Test
    fun `conventional sources do not mark measurement as approximate`() {
        val state =
            MeasurementUiState(
                selectedStartPoint =
                    point(
                        x = 0f,
                        y = 0f,
                        z = 0f
                    ),
                selectedStartSource =
                    AnchorPlacementSource.PLANE,
                selectedEndPoint =
                    point(
                        x = 1f,
                        y = 0f,
                        z = 0f
                    ),
                selectedEndSource =
                    AnchorPlacementSource.DEPTH_POINT
            )

        assertThat(state.hasCompletePlacementProvenance)
            .isTrue()

        assertThat(state.hasApproximatePlacement)
            .isFalse()

        assertThat(state.hasOnlyConventionalPlacements)
            .isTrue()

        assertThat(state.shouldShowApproximatePlacementWarning)
            .isFalse()
    }

    @Test
    fun `complete current measurement can be confirmed`() {
        val state =
            MeasurementUiState(
                trackingStatus =
                    TrackingStatus.TRACKING,
                isTargetingSurface = true,
                selectedStartPoint =
                    point(
                        x = 0f,
                        y = 0f,
                        z = 0f
                    ),
                selectedStartSource =
                    AnchorPlacementSource.PLANE,
                selectedEndPoint =
                    point(
                        x = 2f,
                        y = 0f,
                        z = 0f
                    ),
                selectedEndSource =
                    AnchorPlacementSource.FEATURE_POINT,
                currentMeasurement =
                    distance(
                        meters = 2f
                    )
            )

        assertThat(state.hasCompleteMeasurement)
            .isTrue()

        assertThat(state.canConfirmCurrentDimension)
            .isTrue()

        assertThat(state.nextAnchorSlot)
            .isNull()

        assertThat(state.canPlaceAnchor)
            .isFalse()
    }

    @Test
    fun `approximate current measurement remains confirmable`() {
        val state =
            MeasurementUiState(
                trackingStatus =
                    TrackingStatus.TRACKING,
                selectedStartPoint =
                    point(
                        x = 0f,
                        y = 0f,
                        z = 0f
                    ),
                selectedStartSource =
                    AnchorPlacementSource.INSTANT_PLACEMENT,
                selectedEndPoint =
                    point(
                        x = 2f,
                        y = 0f,
                        z = 0f
                    ),
                selectedEndSource =
                    AnchorPlacementSource.INSTANT_PLACEMENT,
                currentMeasurement =
                    distance(
                        meters = 2f
                    )
            )

        assertThat(state.hasCompleteMeasurement)
            .isTrue()

        assertThat(state.hasApproximatePlacement)
            .isTrue()

        assertThat(state.canConfirmCurrentDimension)
            .isTrue()
    }

    @Test
    fun `tracking allows placement without conventional surface`() {
        val state =
            MeasurementUiState(
                trackingStatus =
                    TrackingStatus.TRACKING,
                isTargetingSurface = false
            )

        assertThat(state.hasConfirmedPlacementSurface)
            .isFalse()

        assertThat(state.requiresApproximatePlacement)
            .isTrue()

        assertThat(state.canPlaceAnchor)
            .isTrue()
    }

    @Test
    fun `confirmed surface does not require approximate placement`() {
        val state =
            MeasurementUiState(
                trackingStatus =
                    TrackingStatus.TRACKING,
                isTargetingSurface = true
            )

        assertThat(state.hasConfirmedPlacementSurface)
            .isTrue()

        assertThat(state.requiresApproximatePlacement)
            .isFalse()

        assertThat(state.canPlaceAnchor)
            .isTrue()
    }

    @Test
    fun `placement is blocked while tracking is unavailable`() {
        val state =
            MeasurementUiState(
                trackingStatus =
                    TrackingStatus.UNAVAILABLE,
                isTargetingSurface = true
            )

        assertThat(state.hasConfirmedPlacementSurface)
            .isFalse()

        assertThat(state.requiresApproximatePlacement)
            .isFalse()

        assertThat(state.canPlaceAnchor)
            .isFalse()
    }

    @Test
    fun `height becomes current axis after width is measured`() {
        val state =
            MeasurementUiState(
                spatialDimensions =
                    SpatialDimensions(
                        width =
                            distance(
                                meters = 2f
                            )
                    )
            )

        assertThat(state.currentDimensionAxis)
            .isEqualTo(DimensionAxis.HEIGHT)

        assertThat(state.measuredDimensionCount)
            .isEqualTo(1)

        assertThat(state.hasPendingDimension)
            .isTrue()
    }

    @Test
    fun `depth becomes current axis after width and height are measured`() {
        val state =
            MeasurementUiState(
                spatialDimensions =
                    SpatialDimensions(
                        width =
                            distance(
                                meters = 2f
                            ),
                        height =
                            distance(
                                meters = 3f
                            )
                    )
            )

        assertThat(state.currentDimensionAxis)
            .isEqualTo(DimensionAxis.DEPTH)

        assertThat(state.measuredDimensionCount)
            .isEqualTo(2)
    }

    @Test
    fun `no axis remains after all dimensions are measured`() {
        val state =
            MeasurementUiState(
                spatialDimensions =
                    completeDimensions()
            )

        assertThat(state.currentDimensionAxis)
            .isNull()

        assertThat(state.hasPendingDimension)
            .isFalse()

        assertThat(state.hasCompleteSpatialDimensions)
            .isTrue()

        assertThat(state.isReadyToCalculateVolume)
            .isTrue()

        assertThat(state.canPlaceAnchor)
            .isFalse()
    }

    @Test
    fun `spatial measurement is complete when volume exists`() {
        val state =
            MeasurementUiState(
                spatialDimensions =
                    completeDimensions(),
                volumeMeasurement =
                    VolumeMeasurement(
                        cubicMeters = 24f,
                        uncertaintyCubicMeters = 1f,
                        timestampMillis =
                            FIXED_TIMESTAMP
                    )
            )

        assertThat(state.hasCompleteSpatialDimensions)
            .isTrue()

        assertThat(state.isReadyToCalculateVolume)
            .isFalse()

        assertThat(state.hasCompleteSpatialMeasurement)
            .isTrue()

        assertThat(state.canResetMeasurement)
            .isTrue()
    }

    @Test
    fun `confirmed dimension keeps reset available without active anchors`() {
        val state =
            MeasurementUiState(
                spatialDimensions =
                    SpatialDimensions(
                        width =
                            distance(
                                meters = 2f
                            )
                    )
            )

        assertThat(state.hasStartPoint)
            .isFalse()

        assertThat(state.hasEndPoint)
            .isFalse()

        assertThat(state.canResetMeasurement)
            .isTrue()
    }

    @Test
    fun `badge is shown only for visible current measurement`() {
        val visibleState =
            MeasurementUiState(
                currentMeasurement =
                    distance(
                        meters = 2f
                    ),
                badgePosition =
                    ScreenPoint2D(
                        x = 100f,
                        y = 200f,
                        isVisible = true
                    )
            )

        val hiddenState =
            visibleState.copy(
                badgePosition =
                    ScreenPoint2D(
                        x = 100f,
                        y = 200f,
                        isVisible = false
                    )
            )

        assertThat(visibleState.shouldShowBadge)
            .isTrue()

        assertThat(hiddenState.shouldShowBadge)
            .isFalse()
    }

    @Test
    fun `source without corresponding point is rejected`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            MeasurementUiState(
                selectedStartSource =
                    AnchorPlacementSource.PLANE
            )
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            MeasurementUiState(
                selectedEndSource =
                    AnchorPlacementSource.INSTANT_PLACEMENT
            )
        }
    }

    @Test
    fun `invalid detected point count is rejected`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            MeasurementUiState(
                detectedPointsCount = -1
            )
        }
    }

    @Test
    fun `invalid viewport dimensions are rejected`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            MeasurementUiState(
                viewportWidthPx = -1
            )
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            MeasurementUiState(
                viewportHeightPx = -1
            )
        }
    }

    private fun completeDimensions(): SpatialDimensions {
        return SpatialDimensions(
            width =
                distance(
                    meters = 2f
                ),
            height =
                distance(
                    meters = 3f
                ),
            depth =
                distance(
                    meters = 4f
                )
        )
    }

    private fun point(
        x: Float,
        y: Float,
        z: Float
    ): Point3D {
        return Point3D(
            x = x,
            y = y,
            z = z
        )
    }

    private fun distance(
        meters: Float,
        uncertaintyMeters: Float = 0.01f
    ): DistanceMeasurement {
        return DistanceMeasurement(
            meters = meters,
            uncertaintyMeters =
                uncertaintyMeters,
            timestampMillis =
                FIXED_TIMESTAMP
        )
    }

    private companion object {
        const val FIXED_TIMESTAMP =
            1_000L
    }
}