package org.aetheris.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import org.aetheris.app.domain.model.DistanceMeasurement
import org.aetheris.app.domain.model.SpatialDimensions
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import kotlin.math.sqrt

class CalculateVolumeUseCaseTest {

    private lateinit var useCase: CalculateVolumeUseCase

    @Before
    fun setUp() {
        useCase = CalculateVolumeUseCase(
            timestampProvider = {
                FIXED_TIMESTAMP
            }
        )
    }

    @Test
    fun `invoke calculates volume from three dimensions`() {
        val result = useCase(
            width = measurement(
                meters = 2f
            ),
            height = measurement(
                meters = 3f
            ),
            depth = measurement(
                meters = 4f
            )
        )

        assertThat(result.cubicMeters)
            .isWithin(0.0001f)
            .of(24f)

        assertThat(result.liters)
            .isWithin(0.01f)
            .of(24_000f)
    }

    @Test
    fun `invoke propagates uncertainty from all dimensions`() {
        val result = useCase(
            width = measurement(
                meters = 2f,
                uncertaintyMeters = 0.1f
            ),
            height = measurement(
                meters = 3f,
                uncertaintyMeters = 0.2f
            ),
            depth = measurement(
                meters = 4f,
                uncertaintyMeters = 0.3f
            )
        )

        /*
         * Contribuição da largura:
         * 3 × 4 × 0,1 = 1,2
         *
         * Contribuição da altura:
         * 2 × 4 × 0,2 = 1,6
         *
         * Contribuição da profundidade:
         * 2 × 3 × 0,3 = 1,8
         *
         * Incerteza:
         * sqrt(1,2² + 1,6² + 1,8²)
         */
        val expectedUncertainty =
            sqrt(
                1.2f * 1.2f +
                        1.6f * 1.6f +
                        1.8f * 1.8f
            )

        assertThat(result.uncertaintyCubicMeters)
            .isWithin(0.0001f)
            .of(expectedUncertainty)
    }

    @Test
    fun `invoke returns zero uncertainty when dimensions have no uncertainty`() {
        val result = useCase(
            width = measurement(
                meters = 2f,
                uncertaintyMeters = 0f
            ),
            height = measurement(
                meters = 3f,
                uncertaintyMeters = 0f
            ),
            depth = measurement(
                meters = 4f,
                uncertaintyMeters = 0f
            )
        )

        assertThat(result.cubicMeters)
            .isEqualTo(24f)

        assertThat(result.uncertaintyCubicMeters)
            .isEqualTo(0f)
    }

    @Test
    fun `invoke calculates uncertainty when one dimension is zero`() {
        val result = useCase(
            width = measurement(
                meters = 0f,
                uncertaintyMeters = 0.1f
            ),
            height = measurement(
                meters = 2f,
                uncertaintyMeters = 0f
            ),
            depth = measurement(
                meters = 3f,
                uncertaintyMeters = 0f
            )
        )

        assertThat(result.cubicMeters)
            .isEqualTo(0f)

        /*
         * Mesmo com volume igual a zero, a incerteza
         * da largura ainda influencia o resultado:
         *
         * 2 × 3 × 0,1 = 0,6 m³
         */
        assertThat(result.uncertaintyCubicMeters)
            .isWithin(0.0001f)
            .of(0.6f)
    }

    @Test
    fun `invoke uses timestamp supplied by provider`() {
        val result = useCase(
            width = measurement(
                meters = 1f
            ),
            height = measurement(
                meters = 1f
            ),
            depth = measurement(
                meters = 1f
            )
        )

        assertThat(result.timestampMillis)
            .isEqualTo(FIXED_TIMESTAMP)
    }

    @Test
    fun `invoke calculates volume from complete spatial dimensions`() {
        val dimensions = SpatialDimensions(
            width = measurement(
                meters = 1.5f,
                uncertaintyMeters = 0.05f
            ),
            height = measurement(
                meters = 2f,
                uncertaintyMeters = 0.05f
            ),
            depth = measurement(
                meters = 0.5f,
                uncertaintyMeters = 0.02f
            )
        )

        val result = useCase(dimensions)

        assertThat(result.cubicMeters)
            .isWithin(0.0001f)
            .of(1.5f)

        assertThat(result.liters)
            .isWithin(0.01f)
            .of(1_500f)
    }

    @Test
    fun `invoke rejects spatial dimensions without width`() {
        val dimensions = SpatialDimensions(
            width = null,
            height = measurement(
                meters = 2f
            ),
            depth = measurement(
                meters = 3f
            )
        )

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            useCase(dimensions)
        }
    }

    @Test
    fun `invoke rejects spatial dimensions without height`() {
        val dimensions = SpatialDimensions(
            width = measurement(
                meters = 1f
            ),
            height = null,
            depth = measurement(
                meters = 3f
            )
        )

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            useCase(dimensions)
        }
    }

    @Test
    fun `invoke rejects spatial dimensions without depth`() {
        val dimensions = SpatialDimensions(
            width = measurement(
                meters = 1f
            ),
            height = measurement(
                meters = 2f
            ),
            depth = null
        )

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            useCase(dimensions)
        }
    }

    @Test
    fun `invoke preserves volume measurement range`() {
        val result = useCase(
            width = measurement(
                meters = 2f,
                uncertaintyMeters = 0.1f
            ),
            height = measurement(
                meters = 3f,
                uncertaintyMeters = 0.1f
            ),
            depth = measurement(
                meters = 4f,
                uncertaintyMeters = 0.1f
            )
        )

        assertThat(result.minimumCubicMeters)
            .isAtLeast(0f)

        assertThat(result.maximumCubicMeters)
            .isGreaterThan(result.cubicMeters)
    }

    private fun measurement(
        meters: Float,
        uncertaintyMeters: Float = 0f
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