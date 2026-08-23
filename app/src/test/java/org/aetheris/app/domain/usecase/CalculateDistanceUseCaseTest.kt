package org.aetheris.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import org.aetheris.app.domain.model.Point3D
import org.junit.Before
import org.junit.Test

class CalculateDistanceUseCaseTest {

    private lateinit var useCase: CalculateDistanceUseCase

    @Before
    fun setUp() {
        useCase = CalculateDistanceUseCase()
    }

    @Test
    fun `when origin to point on Z axis then calculate exact direct distance`() {
        val target = Point3D(0f, 0f, 2.5f)
        val result = useCase(end = target)

        assertThat(result.meters).isWithin(0.001f).of(2.5f)
        assertThat(result.centimeters).isWithin(0.1f).of(250f)
    }

    @Test
    fun `when 3D vector is 3-4-0 then return distance 5`() {
        val target = Point3D(3f, 4f, 0f)
        val result = useCase(end = target)

        assertThat(result.meters).isWithin(0.001f).of(5.0f)
    }

    @Test
    fun `when lower confidence then uncertainty increases`() {
        val target = Point3D(1f, 1f, 1f)

        val highConfidenceResult = useCase(end = target, confidenceScore = 1.0f)
        val lowConfidenceResult = useCase(end = target, confidenceScore = 0.4f)

        assertThat(lowConfidenceResult.uncertaintyMeters)
            .isGreaterThan(highConfidenceResult.uncertaintyMeters)
    }
}