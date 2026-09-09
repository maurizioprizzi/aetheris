package org.aetheris.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpatialMeasurementQualityTest {

    @Test
    fun `empty dimensions produce incomplete quality`() {
        val quality =
            SpatialMeasurementQuality.assess(
                SpatialDimensions.EMPTY
            )

        assertThat(quality.classification)
            .isEqualTo(
                SpatialMeasurementQuality
                    .Classification.INCOMPLETE
            )

        assertThat(quality.measuredAxisCount)
            .isEqualTo(0)

        assertThat(quality.expectedAnchorCount)
            .isEqualTo(0)

        assertThat(quality.knownAnchorCount)
            .isEqualTo(0)

        assertThat(quality.unknownAnchorCount)
            .isEqualTo(0)

        assertThat(quality.hasCompleteDimensions)
            .isFalse()

        assertThat(quality.hasCompleteProvenance)
            .isFalse()

        assertThat(quality.isConclusive)
            .isFalse()

        assertThat(quality.requiresExplicitConfirmation)
            .isFalse()

        assertThat(quality.canProduceExperimentalEstimate)
            .isFalse()
    }

    @Test
    fun `complete legacy dimensions remain incomplete without provenance`() {
        val dimensions =
            SpatialDimensions(
                width = distance(1f),
                height = distance(2f),
                depth = distance(3f)
            )

        val quality =
            SpatialMeasurementQuality.assess(
                dimensions
            )

        assertThat(quality.classification)
            .isEqualTo(
                SpatialMeasurementQuality
                    .Classification.INCOMPLETE
            )

        assertThat(quality.measuredAxisCount)
            .isEqualTo(3)

        assertThat(quality.expectedAnchorCount)
            .isEqualTo(6)

        assertThat(quality.knownAnchorCount)
            .isEqualTo(0)

        assertThat(quality.unknownAnchorCount)
            .isEqualTo(6)

        assertThat(quality.hasCompleteDimensions)
            .isTrue()

        assertThat(quality.hasCompleteProvenance)
            .isFalse()

        assertThat(quality.requiresExplicitConfirmation)
            .isTrue()

        assertThat(quality.canProduceExperimentalEstimate)
            .isTrue()
    }

    @Test
    fun `partial dimensions remain incomplete even with complete provenance`() {
        val dimensions =
            SpatialDimensions.EMPTY
                .withDimensionMeasurement(
                    axis = DimensionAxis.WIDTH,
                    dimensionMeasurement =
                        dimension(
                            startSource =
                                AnchorPlacementSource.PLANE,
                            endSource =
                                AnchorPlacementSource
                                    .FEATURE_POINT
                        )
                )

        val quality =
            SpatialMeasurementQuality.assess(
                dimensions
            )

        assertThat(quality.classification)
            .isEqualTo(
                SpatialMeasurementQuality
                    .Classification.INCOMPLETE
            )

        assertThat(quality.measuredAxisCount)
            .isEqualTo(1)

        assertThat(quality.expectedAnchorCount)
            .isEqualTo(2)

        assertThat(quality.knownAnchorCount)
            .isEqualTo(2)

        assertThat(quality.conventionalAnchorCount)
            .isEqualTo(2)

        assertThat(quality.hasCompleteProvenance)
            .isTrue()

        assertThat(quality.hasCompleteDimensions)
            .isFalse()

        assertThat(quality.isConclusive)
            .isFalse()

        assertThat(quality.requiresExplicitConfirmation)
            .isFalse()
    }

    @Test
    fun `complete conventional dimensions produce conventional quality`() {
        val dimensions =
            completeDimensions(
                widthSources =
                    AnchorPlacementSource.PLANE to
                            AnchorPlacementSource.PLANE,
                heightSources =
                    AnchorPlacementSource.FEATURE_POINT to
                            AnchorPlacementSource.PLANE,
                depthSources =
                    AnchorPlacementSource.DEPTH_POINT to
                            AnchorPlacementSource.DEPTH_POINT
            )

        val quality =
            SpatialMeasurementQuality.assess(
                dimensions
            )

        assertThat(quality.classification)
            .isEqualTo(
                SpatialMeasurementQuality
                    .Classification.CONVENTIONAL
            )

        assertThat(quality.expectedAnchorCount)
            .isEqualTo(6)

        assertThat(quality.knownAnchorCount)
            .isEqualTo(6)

        assertThat(quality.conventionalAnchorCount)
            .isEqualTo(6)

        assertThat(quality.approximateAnchorCount)
            .isEqualTo(0)

        assertThat(quality.depthAnchorCount)
            .isEqualTo(2)

        assertThat(quality.refinableAnchorCount)
            .isEqualTo(0)

        assertThat(quality.unknownAnchorCount)
            .isEqualTo(0)

        assertThat(quality.isConclusive)
            .isTrue()

        assertThat(quality.usesDepth)
            .isTrue()

        assertThat(quality.containsApproximatePlacement)
            .isFalse()

        assertThat(quality.mayRefineOverTime)
            .isFalse()

        assertThat(quality.requiresExplicitConfirmation)
            .isFalse()

        assertThat(quality.canProduceExperimentalEstimate)
            .isTrue()
    }

    @Test
    fun `complete approximate dimensions produce approximate quality`() {
        val instant =
            AnchorPlacementSource.INSTANT_PLACEMENT

        val dimensions =
            completeDimensions(
                widthSources = instant to instant,
                heightSources = instant to instant,
                depthSources = instant to instant
            )

        val quality =
            SpatialMeasurementQuality.assess(
                dimensions
            )

        assertThat(quality.classification)
            .isEqualTo(
                SpatialMeasurementQuality
                    .Classification.APPROXIMATE
            )

        assertThat(quality.knownAnchorCount)
            .isEqualTo(6)

        assertThat(quality.conventionalAnchorCount)
            .isEqualTo(0)

        assertThat(quality.approximateAnchorCount)
            .isEqualTo(6)

        assertThat(quality.depthAnchorCount)
            .isEqualTo(0)

        assertThat(quality.refinableAnchorCount)
            .isEqualTo(6)

        assertThat(quality.containsApproximatePlacement)
            .isTrue()

        assertThat(quality.mayRefineOverTime)
            .isTrue()

        assertThat(quality.requiresExplicitConfirmation)
            .isTrue()
    }

    @Test
    fun `conventional and approximate sources produce mixed quality`() {
        val dimensions =
            completeDimensions(
                widthSources =
                    AnchorPlacementSource.PLANE to
                            AnchorPlacementSource
                                .INSTANT_PLACEMENT,
                heightSources =
                    AnchorPlacementSource.FEATURE_POINT to
                            AnchorPlacementSource.PLANE,
                depthSources =
                    AnchorPlacementSource.DEPTH_POINT to
                            AnchorPlacementSource
                                .INSTANT_PLACEMENT
            )

        val quality =
            SpatialMeasurementQuality.assess(
                dimensions
            )

        assertThat(quality.classification)
            .isEqualTo(
                SpatialMeasurementQuality
                    .Classification.MIXED
            )

        assertThat(quality.conventionalAnchorCount)
            .isEqualTo(4)

        assertThat(quality.approximateAnchorCount)
            .isEqualTo(2)

        assertThat(quality.depthAnchorCount)
            .isEqualTo(1)

        assertThat(quality.refinableAnchorCount)
            .isEqualTo(2)

        assertThat(quality.isConclusive)
            .isTrue()

        assertThat(quality.containsApproximatePlacement)
            .isTrue()

        assertThat(quality.usesDepth)
            .isTrue()

        assertThat(quality.requiresExplicitConfirmation)
            .isTrue()
    }

    @Test
    fun `missing source keeps complete dimensions quality incomplete`() {
        val dimensions =
            SpatialDimensions.EMPTY
                .withDimensionMeasurement(
                    axis = DimensionAxis.WIDTH,
                    dimensionMeasurement =
                        dimension(
                            startSource =
                                AnchorPlacementSource.PLANE,
                            endSource =
                                AnchorPlacementSource.PLANE,
                            meters = 1f
                        )
                )
                .withDimensionMeasurement(
                    axis = DimensionAxis.HEIGHT,
                    dimensionMeasurement =
                        dimension(
                            startSource =
                                AnchorPlacementSource.PLANE,
                            endSource =
                                AnchorPlacementSource.PLANE,
                            meters = 2f
                        )
                )
                .withDimensionMeasurement(
                    axis = DimensionAxis.DEPTH,
                    dimensionMeasurement =
                        dimension(
                            startSource =
                                AnchorPlacementSource
                                    .INSTANT_PLACEMENT,
                            endSource = null,
                            meters = 3f
                        )
                )

        val quality =
            SpatialMeasurementQuality.assess(
                dimensions
            )

        assertThat(quality.classification)
            .isEqualTo(
                SpatialMeasurementQuality
                    .Classification.INCOMPLETE
            )

        assertThat(quality.expectedAnchorCount)
            .isEqualTo(6)

        assertThat(quality.knownAnchorCount)
            .isEqualTo(5)

        assertThat(quality.conventionalAnchorCount)
            .isEqualTo(4)

        assertThat(quality.approximateAnchorCount)
            .isEqualTo(1)

        assertThat(quality.unknownAnchorCount)
            .isEqualTo(1)

        assertThat(quality.hasCompleteDimensions)
            .isTrue()

        assertThat(quality.hasCompleteProvenance)
            .isFalse()

        assertThat(quality.isConclusive)
            .isFalse()

        assertThat(quality.requiresExplicitConfirmation)
            .isTrue()

        assertThat(quality.canProduceExperimentalEstimate)
            .isTrue()
    }

    private fun completeDimensions(
        widthSources:
        Pair<AnchorPlacementSource, AnchorPlacementSource>,
        heightSources:
        Pair<AnchorPlacementSource, AnchorPlacementSource>,
        depthSources:
        Pair<AnchorPlacementSource, AnchorPlacementSource>
    ): SpatialDimensions {
        return SpatialDimensions.EMPTY
            .withDimensionMeasurement(
                axis = DimensionAxis.WIDTH,
                dimensionMeasurement =
                    dimension(
                        startSource =
                            widthSources.first,
                        endSource =
                            widthSources.second,
                        meters = 1f
                    )
            )
            .withDimensionMeasurement(
                axis = DimensionAxis.HEIGHT,
                dimensionMeasurement =
                    dimension(
                        startSource =
                            heightSources.first,
                        endSource =
                            heightSources.second,
                        meters = 2f
                    )
            )
            .withDimensionMeasurement(
                axis = DimensionAxis.DEPTH,
                dimensionMeasurement =
                    dimension(
                        startSource =
                            depthSources.first,
                        endSource =
                            depthSources.second,
                        meters = 3f
                    )
            )
    }

    private fun dimension(
        startSource: AnchorPlacementSource?,
        endSource: AnchorPlacementSource?,
        meters: Float = 1f
    ): DimensionMeasurement {
        return DimensionMeasurement(
            measurement = distance(meters),
            startSource = startSource,
            endSource = endSource
        )
    }

    private fun distance(
        meters: Float
    ): DistanceMeasurement {
        return DistanceMeasurement(
            meters = meters,
            uncertaintyMeters = 0.01f,
            timestampMillis = FIXED_TIMESTAMP
        )
    }

    private companion object {
        const val FIXED_TIMESTAMP = 1_000L
    }
}
