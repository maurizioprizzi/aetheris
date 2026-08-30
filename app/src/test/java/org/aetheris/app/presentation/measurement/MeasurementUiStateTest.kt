package org.aetheris.app.presentation.measurement

import com.google.common.truth.Truth.assertThat
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
        val state = MeasurementUiState()

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
    fun `state requests start anchor when no points exist`() {
        val state = MeasurementUiState(
            trackingStatus = TrackingStatus.TRACKING,
            isTargetingSurface = true
        )

        assertThat(state.nextAnchorSlot)
            .isEqualTo(AnchorSlot.START)

        assertThat(state.canPlaceAnchor)
            .isTrue()
    }

    @Test
    fun `state requests end anchor after start point exists`() {
        val state = MeasurementUiState(
            trackingStatus = TrackingStatus.TRACKING,
            isTargetingSurface = true,
            selectedStartPoint = Point3D(
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
    fun `complete current measurement can be confirmed`() {
        val state = MeasurementUiState(
            trackingStatus = TrackingStatus.TRACKING,
            isTargetingSurface = true,
            selectedStartPoint = Point3D(
                x = 0f,
                y = 0f,
                z = 0f
            ),
            selectedEndPoint = Point3D(
                x = 2f,
                y = 0f,
                z = 0f
            ),
            currentMeasurement = distance(
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
    fun `height becomes current axis after width is measured`() {
        val state = MeasurementUiState(
            spatialDimensions = SpatialDimensions(
                width = distance(
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
        val state = MeasurementUiState(
            spatialDimensions = SpatialDimensions(
                width = distance(
                    meters = 2f
                ),
                height = distance(
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
        val state = MeasurementUiState(
            spatialDimensions = completeDimensions()
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
        val state = MeasurementUiState(
            spatialDimensions = completeDimensions(),
            volumeMeasurement = VolumeMeasurement(
                cubicMeters = 24f,
                uncertaintyCubicMeters = 1f,
                timestampMillis = FIXED_TIMESTAMP
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
        val state = MeasurementUiState(
            spatialDimensions = SpatialDimensions(
                width = distance(
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
        val visibleState = MeasurementUiState(
            currentMeasurement = distance(
                meters = 2f
            ),
            badgePosition = ScreenPoint2D(
                x = 100f,
                y = 200f,
                isVisible = true
            )
        )

        val hiddenState = visibleState.copy(
            badgePosition = ScreenPoint2D(
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
            width = distance(
                meters = 2f
            ),
            height = distance(
                meters = 3f
            ),
            depth = distance(
                meters = 4f
            )
        )
    }

    private fun distance(
        meters: Float,
        uncertaintyMeters: Float = 0.01f
    ): DistanceMeasurement {
        return DistanceMeasurement(
            meters = meters,
            uncertaintyMeters = uncertaintyMeters,
            timestampMillis = FIXED_TIMESTAMP
        )
    }

    private companion object {
        const val FIXED_TIMESTAMP = 1_000L
    }
}