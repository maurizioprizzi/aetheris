package org.aetheris.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import org.aetheris.app.domain.model.Point3D
import org.junit.Before
import org.junit.Test

class EstimateSpatialDimensionsUseCaseTest {

    private lateinit var useCase: EstimateSpatialDimensionsUseCase

    @Before
    fun setUp() {
        useCase = EstimateSpatialDimensionsUseCase()
    }

    @Test
    fun `when given point cloud then calculate correct bounding box and volume`() {
        val cloud = listOf(
            Point3D(0f, 0f, 0f),
            Point3D(2f, 0f, 0f),
            Point3D(0f, 3f, 0f),
            Point3D(0f, 0f, 4f),
            Point3D(2f, 3f, 4f)
        )

        val bbox = useCase(cloud)

        assertThat(bbox.widthMeters).isWithin(0.001f).of(2.0f)
        assertThat(bbox.heightMeters).isWithin(0.001f).of(3.0f)
        assertThat(bbox.depthMeters).isWithin(0.001f).of(4.0f)
        assertThat(bbox.volumeCubicMeters).isWithin(0.001f).of(24.0f)
        assertThat(bbox.volumeLiters).isWithin(0.1f).of(24000.0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `when empty point cloud then throw exception`() {
        useCase(emptyList())
    }
}