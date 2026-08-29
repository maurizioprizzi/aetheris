package org.aetheris.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import org.aetheris.app.domain.model.Point3D
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class CalculateDistanceUseCaseTest {

    private lateinit var useCase: CalculateDistanceUseCase

    @Before
    fun setUp() {
        useCase = CalculateDistanceUseCase()
    }

    @Test
    fun `invoke calculates distance on single axis`() {
        val start = Point3D(
            x = 0f,
            y = 0f,
            z = 0f
        )

        val end = Point3D(
            x = 3f,
            y = 0f,
            z = 0f
        )

        val result = useCase(
            start = start,
            end = end
        )

        assertThat(result.meters)
            .isWithin(DISTANCE_TOLERANCE)
            .of(3f)

        assertThat(result.centimeters)
            .isWithin(CENTIMETER_TOLERANCE)
            .of(300f)

        assertThat(result.millimeters)
            .isWithin(MILLIMETER_TOLERANCE)
            .of(3_000f)
    }

    @Test
    fun `invoke calculates distance on 3D diagonal`() {
        val start = Point3D(
            x = 0f,
            y = 0f,
            z = 0f
        )

        val end = Point3D(
            x = 1f,
            y = 2f,
            z = 2f
        )

        val result = useCase(
            start = start,
            end = end
        )

        assertThat(result.meters)
            .isWithin(DISTANCE_TOLERANCE)
            .of(3f)
    }

    @Test
    fun `invoke calculates expected uncertainty at reference distance`() {
        val start = Point3D(
            x = 0f,
            y = 0f,
            z = 0f
        )

        val end = Point3D(
            x = 0f,
            y = 0f,
            z = 2f
        )

        /*
         * Configuração padrão:
         *
         * u = 0.015 * (1 + 2 / 2) / 1
         * u = 0.030 m
         */
        val result = useCase(
            start = start,
            end = end,
            confidenceScore = 1f
        )

        assertThat(result.uncertaintyMeters)
            .isWithin(UNCERTAINTY_TOLERANCE)
            .of(0.03f)

        assertThat(result.uncertaintyCentimeters)
            .isWithin(CENTIMETER_TOLERANCE)
            .of(3f)
    }

    @Test
    fun `invoke increases uncertainty when confidence decreases`() {
        val start = Point3D(
            x = 0f,
            y = 0f,
            z = 0f
        )

        val end = Point3D(
            x = 0f,
            y = 0f,
            z = 2f
        )

        val highConfidence = useCase(
            start = start,
            end = end,
            confidenceScore = 1f
        )

        val lowConfidence = useCase(
            start = start,
            end = end,
            confidenceScore = 0.5f
        )

        assertThat(lowConfidence.uncertaintyMeters)
            .isGreaterThan(highConfidence.uncertaintyMeters)

        assertThat(lowConfidence.uncertaintyMeters)
            .isWithin(UNCERTAINTY_TOLERANCE)
            .of(0.06f)
    }

    @Test
    fun `invoke applies minimum confidence`() {
        val configuredUseCase = CalculateDistanceUseCase(
            baseUncertaintyMeters = 0.015f,
            referenceDistanceMeters = 2f,
            minimumConfidence = 0.1f
        )

        val result = configuredUseCase(
            start = Point3D(0f, 0f, 0f),
            end = Point3D(0f, 0f, 2f),
            confidenceScore = 0.01f
        )

        /*
         * A confiança efetiva é limitada a 0.1:
         *
         * u = 0.015 * (1 + 2 / 2) / 0.1
         * u = 0.30 m
         */
        assertThat(result.uncertaintyMeters)
            .isWithin(UNCERTAINTY_TOLERANCE)
            .of(0.30f)
    }

    @Test
    fun `invoke returns zero distance for identical points`() {
        val point = Point3D(
            x = 1.5f,
            y = -2f,
            z = 3.2f
        )

        val result = useCase(
            start = point,
            end = point
        )

        assertThat(result.meters)
            .isWithin(DISTANCE_TOLERANCE)
            .of(0f)

        assertThat(result.uncertaintyMeters)
            .isWithin(UNCERTAINTY_TOLERANCE)
            .of(0.015f)
    }

    @Test
    fun `invoke rejects zero confidence`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase(
                start = Point3D(0f, 0f, 0f),
                end = Point3D(1f, 0f, 0f),
                confidenceScore = 0f
            )
        }
    }

    @Test
    fun `invoke rejects confidence above one`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase(
                start = Point3D(0f, 0f, 0f),
                end = Point3D(1f, 0f, 0f),
                confidenceScore = 1.01f
            )
        }
    }

    @Test
    fun `constructor rejects negative base uncertainty`() {
        assertThrows(IllegalArgumentException::class.java) {
            CalculateDistanceUseCase(
                baseUncertaintyMeters = -0.01f
            )
        }
    }

    private companion object {
        const val DISTANCE_TOLERANCE = 1e-4f
        const val UNCERTAINTY_TOLERANCE = 1e-5f
        const val CENTIMETER_TOLERANCE = 1e-2f
        const val MILLIMETER_TOLERANCE = 1e-1f
    }
}