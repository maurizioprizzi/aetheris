package org.aetheris.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class SpatialFrameDataTest {

    @Test
    fun `default state has no anchors and complete empty provenance`() {
        val state = SpatialFrameData()

        assertThat(state.isTracking)
            .isFalse()

        assertThat(state.hasPointCloud)
            .isFalse()

        assertThat(state.anchorCount)
            .isEqualTo(0)

        assertThat(state.hasCompleteMeasurement)
            .isFalse()

        assertThat(state.anchoredStartPlacement)
            .isNull()

        assertThat(state.anchoredEndPlacement)
            .isNull()

        assertThat(state.hasCompletePlacementProvenance)
            .isTrue()

        assertThat(state.hasApproximateAnchor)
            .isFalse()

        assertThat(state.hasOnlyConventionalAnchors)
            .isFalse()
    }

    @Test
    fun `start placement combines point and source`() {
        val point = Point3D(
            x = 1f,
            y = 2f,
            z = 3f
        )

        val state = SpatialFrameData(
            anchoredStartPoint = point,
            anchoredStartSource =
                AnchorPlacementSource.PLANE
        )

        assertThat(state.anchoredStartPlacement)
            .isEqualTo(
                AnchorPlacement(
                    position = point,
                    source =
                        AnchorPlacementSource.PLANE
                )
            )

        assertThat(state.hasStartAnchor)
            .isTrue()

        assertThat(state.hasEndAnchor)
            .isFalse()

        assertThat(state.anchorCount)
            .isEqualTo(1)

        assertThat(state.hasCompletePlacementProvenance)
            .isTrue()

        assertThat(state.hasOnlyConventionalAnchors)
            .isTrue()
    }

    @Test
    fun `end placement combines point and source`() {
        val point = Point3D(
            x = 4f,
            y = 5f,
            z = 6f
        )

        val state = SpatialFrameData(
            anchoredEndPoint = point,
            anchoredEndSource =
                AnchorPlacementSource.FEATURE_POINT
        )

        assertThat(state.anchoredEndPlacement)
            .isEqualTo(
                AnchorPlacement(
                    position = point,
                    source =
                        AnchorPlacementSource.FEATURE_POINT
                )
            )

        assertThat(state.hasEndAnchor)
            .isTrue()

        assertThat(state.hasCompletePlacementProvenance)
            .isTrue()

        assertThat(state.hasOnlyConventionalAnchors)
            .isTrue()
    }

    @Test
    fun `point without source preserves compatibility but provenance is incomplete`() {
        val state = SpatialFrameData(
            anchoredStartPoint = Point3D(
                x = 1f,
                y = 2f,
                z = 3f
            )
        )

        assertThat(state.hasStartAnchor)
            .isTrue()

        assertThat(state.anchoredStartPlacement)
            .isNull()

        assertThat(state.hasCompletePlacementProvenance)
            .isFalse()

        assertThat(state.hasApproximateAnchor)
            .isFalse()

        assertThat(state.hasOnlyConventionalAnchors)
            .isFalse()
    }

    @Test
    fun `instant placement marks state as approximate`() {
        val state = SpatialFrameData(
            anchoredStartPoint = Point3D(
                x = 0f,
                y = 0f,
                z = -1.5f
            ),
            anchoredStartSource =
                AnchorPlacementSource.INSTANT_PLACEMENT
        )

        assertThat(state.hasApproximateAnchor)
            .isTrue()

        assertThat(state.hasOnlyConventionalAnchors)
            .isFalse()

        assertThat(
            state.anchoredStartPlacement?.isApproximate
        ).isTrue()
    }

    @Test
    fun `complete measurement can contain different placement sources`() {
        val state = SpatialFrameData(
            anchoredStartPoint = Point3D(
                x = 0f,
                y = 0f,
                z = 0f
            ),
            anchoredEndPoint = Point3D(
                x = 1f,
                y = 0f,
                z = 0f
            ),
            anchoredStartSource =
                AnchorPlacementSource.PLANE,
            anchoredEndSource =
                AnchorPlacementSource.INSTANT_PLACEMENT
        )

        assertThat(state.hasCompleteMeasurement)
            .isTrue()

        assertThat(state.anchorCount)
            .isEqualTo(2)

        assertThat(state.hasCompletePlacementProvenance)
            .isTrue()

        assertThat(state.hasApproximateAnchor)
            .isTrue()

        assertThat(state.hasOnlyConventionalAnchors)
            .isFalse()
    }

    @Test
    fun `all known conventional anchors are reported as conventional`() {
        val state = SpatialFrameData(
            anchoredStartPoint = Point3D(
                x = 0f,
                y = 0f,
                z = 0f
            ),
            anchoredEndPoint = Point3D(
                x = 1f,
                y = 1f,
                z = 1f
            ),
            anchoredStartSource =
                AnchorPlacementSource.PLANE,
            anchoredEndSource =
                AnchorPlacementSource.DEPTH_POINT
        )

        assertThat(state.hasCompleteMeasurement)
            .isTrue()

        assertThat(state.hasCompletePlacementProvenance)
            .isTrue()

        assertThat(state.hasApproximateAnchor)
            .isFalse()

        assertThat(state.hasOnlyConventionalAnchors)
            .isTrue()
    }

    @Test
    fun `placement source without corresponding point is rejected`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            SpatialFrameData(
                anchoredStartSource =
                    AnchorPlacementSource.PLANE
            )
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            SpatialFrameData(
                anchoredEndSource =
                    AnchorPlacementSource.FEATURE_POINT
            )
        }
    }

    @Test
    fun `negative point count is rejected`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            SpatialFrameData(
                pointCount = -1
            )
        }
    }

    @Test
    fun `anchor placement readiness still requires tracking and conventional surface`() {
        val readyState = SpatialFrameData(
            trackingStatus =
                TrackingStatus.TRACKING,
            isSurfaceDetected = true
        )

        val noSurfaceState = readyState.copy(
            isSurfaceDetected = false
        )

        val unavailableState = readyState.copy(
            trackingStatus =
                TrackingStatus.UNAVAILABLE
        )

        assertThat(readyState.isReadyForAnchorPlacement)
            .isTrue()

        assertThat(noSurfaceState.isReadyForAnchorPlacement)
            .isFalse()

        assertThat(unavailableState.isReadyForAnchorPlacement)
            .isFalse()
    }
}