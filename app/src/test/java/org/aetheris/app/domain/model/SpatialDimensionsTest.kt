package org.aetheris.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpatialDimensionsTest {

    @Test
    fun `empty dimensions have no measured axes`() {
        val dimensions = SpatialDimensions.EMPTY

        assertThat(dimensions.isEmpty)
            .isTrue()

        assertThat(dimensions.isComplete)
            .isFalse()

        assertThat(dimensions.measuredAxisCount)
            .isEqualTo(0)

        assertThat(dimensions.pendingAxes)
            .containsExactly(
                DimensionAxis.WIDTH,
                DimensionAxis.HEIGHT,
                DimensionAxis.DEPTH
            )
            .inOrder()

        assertThat(dimensions.nextPendingAxis)
            .isEqualTo(DimensionAxis.WIDTH)
    }

    @Test
    fun `measured axes follow official dimension order`() {
        val dimensions = SpatialDimensions()
            .withMeasurement(
                axis = DimensionAxis.WIDTH,
                measurement = distance(2f)
            )
            .withMeasurement(
                axis = DimensionAxis.HEIGHT,
                measurement = distance(3f)
            )

        assertThat(dimensions.measuredAxisCount)
            .isEqualTo(2)

        assertThat(dimensions.pendingAxes)
            .containsExactly(DimensionAxis.DEPTH)

        assertThat(dimensions.nextPendingAxis)
            .isEqualTo(DimensionAxis.DEPTH)

        assertThat(dimensions.isComplete)
            .isFalse()
    }

    @Test
    fun `complete dimensions calculate cubic meters and liters`() {
        val dimensions = completeDimensions()

        assertThat(dimensions.isComplete)
            .isTrue()

        assertThat(dimensions.measuredAxisCount)
            .isEqualTo(3)

        assertThat(dimensions.pendingAxes)
            .isEmpty()

        assertThat(dimensions.nextPendingAxis)
            .isNull()

        assertThat(dimensions.volumeCubicMeters)
            .isEqualTo(24f)

        assertThat(dimensions.volumeLiters)
            .isEqualTo(24_000f)
    }

    @Test
    fun `incomplete dimensions do not calculate volume`() {
        val dimensions = SpatialDimensions(
            width = distance(2f),
            height = distance(3f)
        )

        assertThat(dimensions.volumeCubicMeters)
            .isNull()

        assertThat(dimensions.volumeLiters)
            .isNull()
    }

    @Test
    fun `axis accessor returns corresponding distance`() {
        val width = distance(2f)
        val height = distance(3f)
        val depth = distance(4f)

        val dimensions = SpatialDimensions(
            width = width,
            height = height,
            depth = depth
        )

        assertThat(dimensions[DimensionAxis.WIDTH])
            .isSameInstanceAs(width)

        assertThat(dimensions[DimensionAxis.HEIGHT])
            .isSameInstanceAs(height)

        assertThat(dimensions[DimensionAxis.DEPTH])
            .isSameInstanceAs(depth)
    }

    @Test
    fun `withMeasurement updates selected axis without mutating original`() {
        val original = SpatialDimensions.EMPTY
        val width = distance(2f)

        val updated = original.withMeasurement(
            axis = DimensionAxis.WIDTH,
            measurement = width
        )

        assertThat(original.width)
            .isNull()

        assertThat(updated.width)
            .isSameInstanceAs(width)

        assertThat(updated.height)
            .isNull()

        assertThat(updated.depth)
            .isNull()
    }

    @Test
    fun `withoutMeasurement removes selected axis`() {
        val original = completeDimensions()

        val updated = original.withoutMeasurement(
            DimensionAxis.HEIGHT
        )

        assertThat(original.height)
            .isNotNull()

        assertThat(updated.width)
            .isEqualTo(original.width)

        assertThat(updated.height)
            .isNull()

        assertThat(updated.depth)
            .isEqualTo(original.depth)

        assertThat(updated.nextPendingAxis)
            .isEqualTo(DimensionAxis.HEIGHT)
    }

    @Test
    fun `legacy distance is adapted to dimension without provenance`() {
        val width = distance(2f)

        val dimensions = SpatialDimensions(
            width = width
        )

        val dimension =
            dimensions.getDimensionMeasurement(
                DimensionAxis.WIDTH
            )

        assertThat(dimension?.measurement)
            .isSameInstanceAs(width)

        assertThat(dimension?.hasAnyProvenance)
            .isFalse()

        assertThat(
            dimensions.getDimensionMeasurement(
                DimensionAxis.HEIGHT
            )
        ).isNull()
    }

    @Test
    fun `withDimensionMeasurement preserves sources for selected axis`() {
        val width = DimensionMeasurement(
            measurement = distance(2f),
            startSource = AnchorPlacementSource.PLANE,
            endSource = AnchorPlacementSource.FEATURE_POINT
        )

        val dimensions = SpatialDimensions.EMPTY
            .withDimensionMeasurement(
                axis = DimensionAxis.WIDTH,
                dimensionMeasurement = width
            )

        assertThat(dimensions.width)
            .isSameInstanceAs(width.measurement)

        assertThat(
            dimensions.getDimensionMeasurement(
                DimensionAxis.WIDTH
            )
        ).isEqualTo(width)

        assertThat(dimensions.axesWithProvenance)
            .containsExactly(DimensionAxis.WIDTH)

        assertThat(dimensions.provenanceAxisCount)
            .isEqualTo(1)

        assertThat(dimensions.hasAnyProvenance)
            .isTrue()

        assertThat(dimensions.hasCompleteProvenance)
            .isTrue()
    }

    @Test
    fun `complete provenance requires both sources on every measured axis`() {
        val width = DimensionMeasurement(
            measurement = distance(2f),
            startSource = AnchorPlacementSource.PLANE,
            endSource = AnchorPlacementSource.FEATURE_POINT
        )

        val height = DimensionMeasurement(
            measurement = distance(3f),
            startSource = AnchorPlacementSource.PLANE
        )

        val dimensions = SpatialDimensions.EMPTY
            .withDimensionMeasurement(
                axis = DimensionAxis.WIDTH,
                dimensionMeasurement = width
            )
            .withDimensionMeasurement(
                axis = DimensionAxis.HEIGHT,
                dimensionMeasurement = height
            )

        assertThat(dimensions.provenanceAxisCount)
            .isEqualTo(2)

        assertThat(dimensions.hasCompleteProvenance)
            .isFalse()
    }

    @Test
    fun `empty dimensions do not report complete provenance`() {
        assertThat(
            SpatialDimensions.EMPTY.hasCompleteProvenance
        ).isFalse()
    }

    @Test
    fun `aggregate indicators expose approximate and refinable placement`() {
        val width = DimensionMeasurement(
            measurement = distance(2f),
            startSource = AnchorPlacementSource.PLANE,
            endSource =
                AnchorPlacementSource.INSTANT_PLACEMENT
        )

        val dimensions = SpatialDimensions.EMPTY
            .withDimensionMeasurement(
                axis = DimensionAxis.WIDTH,
                dimensionMeasurement = width
            )

        assertThat(dimensions.usesApproximatePlacement)
            .isTrue()

        assertThat(dimensions.mayRefineOverTime)
            .isTrue()

        assertThat(dimensions.usesDepth)
            .isFalse()
    }

    @Test
    fun `aggregate indicators expose depth usage`() {
        val depth = DimensionMeasurement(
            measurement = distance(4f),
            startSource = AnchorPlacementSource.DEPTH_POINT,
            endSource = AnchorPlacementSource.PLANE
        )

        val dimensions = SpatialDimensions.EMPTY
            .withDimensionMeasurement(
                axis = DimensionAxis.DEPTH,
                dimensionMeasurement = depth
            )

        assertThat(dimensions.usesDepth)
            .isTrue()

        assertThat(dimensions.usesApproximatePlacement)
            .isFalse()
    }

    @Test
    fun `withMeasurement removes previous provenance from axis`() {
        val provenancedWidth = DimensionMeasurement(
            measurement = distance(2f),
            startSource = AnchorPlacementSource.PLANE,
            endSource = AnchorPlacementSource.FEATURE_POINT
        )

        val replacement = distance(2.5f)

        val dimensions = SpatialDimensions.EMPTY
            .withDimensionMeasurement(
                axis = DimensionAxis.WIDTH,
                dimensionMeasurement = provenancedWidth
            )
            .withMeasurement(
                axis = DimensionAxis.WIDTH,
                measurement = replacement
            )

        val stored =
            dimensions.getDimensionMeasurement(
                DimensionAxis.WIDTH
            )

        assertThat(stored?.measurement)
            .isSameInstanceAs(replacement)

        assertThat(stored?.hasAnyProvenance)
            .isFalse()

        assertThat(dimensions.hasAnyProvenance)
            .isFalse()
    }

    @Test
    fun `withoutMeasurement removes distance and provenance`() {
        val provenancedWidth = DimensionMeasurement(
            measurement = distance(2f),
            startSource = AnchorPlacementSource.PLANE,
            endSource = AnchorPlacementSource.FEATURE_POINT
        )

        val dimensions = SpatialDimensions.EMPTY
            .withDimensionMeasurement(
                axis = DimensionAxis.WIDTH,
                dimensionMeasurement = provenancedWidth
            )
            .withoutMeasurement(DimensionAxis.WIDTH)

        assertThat(dimensions.width)
            .isNull()

        assertThat(
            dimensions.getDimensionMeasurement(
                DimensionAxis.WIDTH
            )
        ).isNull()

        assertThat(dimensions.hasAnyProvenance)
            .isFalse()
    }

    private fun completeDimensions(): SpatialDimensions {
        return SpatialDimensions(
            width = distance(2f),
            height = distance(3f),
            depth = distance(4f)
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