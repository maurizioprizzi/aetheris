package org.aetheris.app.data.repository

import com.google.common.truth.Truth.assertThat
import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.TrackingStatus
import org.junit.Before
import org.junit.Test

class SpatialSensorRepositoryTest {

    private lateinit var repository: SpatialSensorRepositoryImpl

    @Before
    fun setUp() {
        repository = SpatialSensorRepositoryImpl()
    }

    @Test
    fun `when updating frame data then state flow emits new values`() {
        val samplePoints = listOf(Point3D(0.5f, 0.5f, 1.2f))

        repository.updateFrame(
            cameraPosition = Point3D(0f, 0f, 0f),
            points = samplePoints,
            status = TrackingStatus.TRACKING,
            hasDepth = true
        )

        val currentState = repository.spatialFrameStream.value
        assertThat(currentState.trackingStatus).isEqualTo(TrackingStatus.TRACKING)
        assertThat(currentState.isDepthAvailable).isTrue()
        assertThat(currentState.pointCloud).hasSize(1)
        assertThat(currentState.pointCloud.first().z).isEqualTo(1.2f)
    }

    @Test
    fun `when reset tracking then state returns to initial defaults`() {
        repository.updateFrame(
            cameraPosition = Point3D(1f, 1f, 1f),
            points = listOf(Point3D(2f, 2f, 2f)),
            status = TrackingStatus.TRACKING,
            hasDepth = true
        )

        repository.resetTracking()

        val resetState = repository.spatialFrameStream.value
        assertThat(resetState.trackingStatus).isEqualTo(TrackingStatus.INITIALIZING)
        assertThat(resetState.pointCloud).isEmpty()
    }
}