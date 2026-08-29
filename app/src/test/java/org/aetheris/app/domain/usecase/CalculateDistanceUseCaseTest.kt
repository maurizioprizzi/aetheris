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
    fun `invoke calculates exact metric distance on single axis`() {
        val start = Point3D(0f, 0f, 0f)
        val end = Point3D(3f, 0f, 0f)

        val result = useCase(start = start, end = end)

        assertThat(result.meters).isWithin(1e-4f).of(3.0f)
        assertThat(result.centimeters).isWithin(1e-2f).of(300.0f)
    }

    @Test
    fun `invoke calculates exact metric distance in 3D diagonal`() {
        val start = Point3D(0f, 0f, 0f)
        val end = Point3D(1f, 2f, 2f) // sqrt(1 + 4 + 4) = 3.0

        val result = useCase(start = start, end = end)

        assertThat(result.meters).isWithin(1e-4f).of(3.0f)
    }

    @Test
    fun `invoke computes metric uncertainty proportional to distance`() {
        val start = Point3D(0f, 0f, 0f)
        val end = Point3D(0f, 0f, 2f)

        val result = useCase(start = start, end = end)

        assertThat(result.uncertaintyMeters).isGreaterThan(0.0f)
        assertThat(result.uncertaintyCentimeters).isGreaterThan(0.0f)
    }

    @Test
    fun `invoke returns zero distance when points are identical`() {
        val point = Point3D(1.5f, -2.0f, 3.2f)

        val result = useCase(start = point, end = point)

        assertThat(result.meters).isWithin(1e-5f).of(0.0f)
    }
}