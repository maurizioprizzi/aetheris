package org.aetheris.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpatialDimensionsTest {

    @Test
    fun `empty dimensions start with width pending`() {
        val dimensions =
            SpatialDimensions.EMPTY

        assertThat(dimensions.isEmpty)
            .isTrue()

        assertThat(dimensions.isComplete)
            .isFalse()

        assertThat(dimensions.measuredAxisCount)
            .isEqualTo(0)

        assertThat(dimensions.nextPendingAxis)
            .isEqualTo(DimensionAxis.WIDTH)

        assertThat(dimensions.volumeCubicMeters)
            .isNull()

        assertThat(dimensions.volumeLiters)
            .isNull()
    }

    @Test
    fun `adding width advances next pending axis to height`() {
        val width =
            createMeasurement(
                meters = 2f
            )

        val dimensions =
            SpatialDimensions.EMPTY
                .withMeasurement(
                    axis = DimensionAxis.WIDTH,
                    measurement = width
                )

        assertThat(dimensions.width)
            .isSameInstanceAs(width)

        assertThat(dimensions.measuredAxisCount)
            .isEqualTo(1)

        assertThat(dimensions.nextPendingAxis)
            .isEqualTo(DimensionAxis.HEIGHT)

        assertThat(dimensions.isComplete)
            .isFalse()
    }

    @Test
    fun `pending axes preserve measurement order`() {
        val dimensions =
            SpatialDimensions.EMPTY
                .withMeasurement(
                    axis = DimensionAxis.WIDTH,
                    measurement =
                        createMeasurement(2f)
                )

        assertThat(dimensions.pendingAxes)
            .containsExactly(
                DimensionAxis.HEIGHT,
                DimensionAxis.DEPTH
            )
            .inOrder()
    }

    @Test
    fun `measurements can be accessed by dimension axis`() {
        val width =
            createMeasurement(2f)

        val height =
            createMeasurement(3f)

        val depth =
            createMeasurement(4f)

        val dimensions =
            SpatialDimensions(
                width = width,
                height = height,
                depth = depth
            )

        assertThat(
            dimensions[DimensionAxis.WIDTH]
        ).isSameInstanceAs(width)

        assertThat(
            dimensions[DimensionAxis.HEIGHT]
        ).isSameInstanceAs(height)

        assertThat(
            dimensions[DimensionAxis.DEPTH]
        ).isSameInstanceAs(depth)
    }

    @Test
    fun `complete dimensions calculate cubic meters and liters`() {
        val dimensions =
            SpatialDimensions(
                width =
                    createMeasurement(2f),
                height =
                    createMeasurement(3f),
                depth =
                    createMeasurement(4f)
            )

        assertThat(dimensions.isComplete)
            .isTrue()

        assertThat(dimensions.measuredAxisCount)
            .isEqualTo(3)

        assertThat(dimensions.nextPendingAxis)
            .isNull()

        assertThat(dimensions.volumeCubicMeters)
            .isWithin(1e-5f)
            .of(24f)

        assertThat(dimensions.volumeLiters)
            .isWithin(1e-2f)
            .of(24_000f)
    }

    @Test
    fun `zero dimension produces zero volume`() {
        val dimensions =
            SpatialDimensions(
                width =
                    createMeasurement(2f),
                height =
                    createMeasurement(0f),
                depth =
                    createMeasurement(4f)
            )

        assertThat(dimensions.isComplete)
            .isTrue()

        assertThat(dimensions.volumeCubicMeters)
            .isWithin(1e-5f)
            .of(0f)

        assertThat(dimensions.volumeLiters)
            .isWithin(1e-5f)
            .of(0f)
    }

    @Test
    fun `adding measurement returns new immutable instance`() {
        val initial =
            SpatialDimensions.EMPTY

        val updated =
            initial.withMeasurement(
                axis = DimensionAxis.WIDTH,
                measurement =
                    createMeasurement(1.5f)
            )

        assertThat(initial.width)
            .isNull()

        assertThat(initial.isEmpty)
            .isTrue()

        assertThat(updated.width)
            .isNotNull()

        assertThat(updated.isEmpty)
            .isFalse()
    }

    @Test
    fun `existing measurement can be replaced`() {
        val originalWidth =
            createMeasurement(1f)

        val correctedWidth =
            createMeasurement(1.2f)

        val dimensions =
            SpatialDimensions(
                width = originalWidth
            ).withMeasurement(
                axis = DimensionAxis.WIDTH,
                measurement = correctedWidth
            )

        assertThat(dimensions.width)
            .isSameInstanceAs(correctedWidth)
    }

    @Test
    fun `measurement can be removed from selected axis`() {
        val dimensions =
            SpatialDimensions(
                width =
                    createMeasurement(2f),
                height =
                    createMeasurement(3f),
                depth =
                    createMeasurement(4f)
            ).withoutMeasurement(
                axis = DimensionAxis.HEIGHT
            )

        assertThat(dimensions.width)
            .isNotNull()

        assertThat(dimensions.height)
            .isNull()

        assertThat(dimensions.depth)
            .isNotNull()

        assertThat(dimensions.isComplete)
            .isFalse()

        assertThat(dimensions.nextPendingAxis)
            .isEqualTo(DimensionAxis.HEIGHT)

        assertThat(dimensions.volumeCubicMeters)
            .isNull()
    }

    private fun createMeasurement(
        meters: Float
    ): DistanceMeasurement {
        return DistanceMeasurement(
            meters = meters,
            uncertaintyMeters = 0.02f,
            timestampMillis = FIXED_TIMESTAMP
        )
    }

    private companion object {
        const val FIXED_TIMESTAMP = 1_000L
    }
}