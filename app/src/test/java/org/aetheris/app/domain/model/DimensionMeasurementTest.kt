package org.aetheris.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DimensionMeasurementTest {

    @Test
    fun `measurement preserves distance value`() {
        val distance = distance(
            meters = 2.5f,
            uncertaintyMeters = 0.02f
        )

        val dimension = DimensionMeasurement(
            measurement = distance
        )

        assertThat(dimension.measurement)
            .isSameInstanceAs(distance)
    }

    @Test
    fun `measurement without sources has no provenance`() {
        val dimension = DimensionMeasurement(
            measurement = distance()
        )

        assertThat(dimension.knownSources)
            .isEmpty()

        assertThat(dimension.knownSourceCount)
            .isEqualTo(0)

        assertThat(dimension.hasAnyProvenance)
            .isFalse()

        assertThat(dimension.hasCompleteProvenance)
            .isFalse()

        assertThat(dimension.hasPartialProvenance)
            .isFalse()

        assertThat(dimension.usesApproximatePlacement)
            .isFalse()

        assertThat(dimension.isFullyConventional)
            .isFalse()
    }

    @Test
    fun `complete conventional sources produce fully conventional measurement`() {
        val dimension = DimensionMeasurement(
            measurement = distance(),
            startSource = AnchorPlacementSource.PLANE,
            endSource = AnchorPlacementSource.FEATURE_POINT
        )

        assertThat(dimension.knownSources)
            .containsExactly(
                AnchorPlacementSource.PLANE,
                AnchorPlacementSource.FEATURE_POINT
            )
            .inOrder()

        assertThat(dimension.knownSourceCount)
            .isEqualTo(2)

        assertThat(dimension.hasAnyProvenance)
            .isTrue()

        assertThat(dimension.hasCompleteProvenance)
            .isTrue()

        assertThat(dimension.hasPartialProvenance)
            .isFalse()

        assertThat(dimension.isFullyConventional)
            .isTrue()

        assertThat(dimension.usesApproximatePlacement)
            .isFalse()

        assertThat(dimension.mayRefineOverTime)
            .isFalse()
    }

    @Test
    fun `instant placement marks measurement as approximate and refinable`() {
        val dimension = DimensionMeasurement(
            measurement = distance(),
            startSource = AnchorPlacementSource.PLANE,
            endSource = AnchorPlacementSource.INSTANT_PLACEMENT
        )

        assertThat(dimension.hasCompleteProvenance)
            .isTrue()

        assertThat(dimension.usesApproximatePlacement)
            .isTrue()

        assertThat(dimension.mayRefineOverTime)
            .isTrue()

        assertThat(dimension.isFullyConventional)
            .isFalse()
    }

    @Test
    fun `depth source marks measurement as depth dependent`() {
        val dimension = DimensionMeasurement(
            measurement = distance(),
            startSource = AnchorPlacementSource.DEPTH_POINT,
            endSource = AnchorPlacementSource.PLANE
        )

        assertThat(dimension.usesDepth)
            .isTrue()

        assertThat(dimension.isFullyConventional)
            .isTrue()
    }

    @Test
    fun `single known source produces partial provenance`() {
        val dimension = DimensionMeasurement(
            measurement = distance(),
            startSource = AnchorPlacementSource.FEATURE_POINT
        )

        assertThat(dimension.knownSources)
            .containsExactly(
                AnchorPlacementSource.FEATURE_POINT
            )

        assertThat(dimension.knownSourceCount)
            .isEqualTo(1)

        assertThat(dimension.hasAnyProvenance)
            .isTrue()

        assertThat(dimension.hasPartialProvenance)
            .isTrue()

        assertThat(dimension.hasCompleteProvenance)
            .isFalse()

        assertThat(dimension.isFullyConventional)
            .isFalse()
    }

    @Test
    fun `slot accessor returns corresponding anchor source`() {
        val dimension = DimensionMeasurement(
            measurement = distance(),
            startSource = AnchorPlacementSource.PLANE,
            endSource = AnchorPlacementSource.INSTANT_PLACEMENT
        )

        assertThat(dimension[AnchorSlot.START])
            .isEqualTo(AnchorPlacementSource.PLANE)

        assertThat(dimension[AnchorSlot.END])
            .isEqualTo(
                AnchorPlacementSource.INSTANT_PLACEMENT
            )
    }

    private fun distance(
        meters: Float = 1.0f,
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