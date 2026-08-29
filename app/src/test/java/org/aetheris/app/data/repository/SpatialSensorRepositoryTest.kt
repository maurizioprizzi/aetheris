package org.aetheris.app.data.repository

import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.aetheris.app.data.arcore.ArCoreFrameProcessor
import org.aetheris.app.data.arcore.ArCoreHitTestProcessor
import org.aetheris.app.domain.model.AnchorSlot
import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.TrackingStatus
import org.junit.Before
import org.junit.Test

class SpatialSensorRepositoryTest {

    private val frameProcessor: ArCoreFrameProcessor = mockk(relaxed = true)
    private val hitTestProcessor: ArCoreHitTestProcessor = mockk(relaxed = true)
    private lateinit var repository: SpatialSensorRepositoryImpl

    private val frame: Frame = mockk()
    private val camera: Camera = mockk()

    @Before
    fun setUp() {
        repository = SpatialSensorRepositoryImpl(frameProcessor, hitTestProcessor)
        every { frame.camera } returns camera
        every { camera.trackingState } returns TrackingState.TRACKING
        every { frameProcessor.processPointCloud(any()) } returns emptyList()
        every { hitTestProcessor.hasValidSurfaceAt(any(), any(), any()) } returns true
    }

    @Test
    fun `updateFrameData should update stream with tracking status and points`() {
        every { frameProcessor.processPointCloud(frame) } returns listOf(Point3D(0f, 1f, -2f))
        every { hitTestProcessor.hasValidSurfaceAt(frame, 0.5f, 0.5f) } returns true

        repository.updateFrameData(frame)

        val state = repository.getSpatialDataStream().value
        assertThat(state.trackingStatus).isEqualTo(TrackingStatus.TRACKING)
        assertThat(state.isDepthEnabled).isTrue()
        assertThat(state.pointCount).isEqualTo(1)
        assertThat(state.isSurfaceDetected).isTrue()
    }

    @Test
    fun `performHitTest should delegate to hitTestProcessor`() = runTest {
        val expectedPoint = Point3D(1.0f, 2.0f, 3.0f)
        every { hitTestProcessor.performHitTest(frame, 0.5f, 0.5f) } returns expectedPoint

        repository.updateFrameData(frame)
        val result = repository.performHitTest(0.5f, 0.5f)

        assertThat(result).isEqualTo(expectedPoint)
    }

    @Test
    fun `createAnchor should attach anchor and update stream`() = runTest {
        val anchor: Anchor = mockk(relaxed = true)
        val pose: Pose = mockk()
        every { pose.tx() } returns 1.0f
        every { pose.ty() } returns 2.0f
        every { pose.tz() } returns -3.0f
        every { anchor.pose } returns pose
        every { anchor.trackingState } returns TrackingState.TRACKING
        every { hitTestProcessor.createAnchorAt(frame, 0.5f, 0.5f) } returns anchor

        repository.updateFrameData(frame)
        val result = repository.createAnchor(0.5f, 0.5f, AnchorSlot.START)

        assertThat(result).isEqualTo(Point3D(1.0f, 2.0f, -3.0f))
        assertThat(repository.getSpatialDataStream().value.anchoredStartPoint).isEqualTo(result)
    }

    @Test
    fun `clearAnchors should detach native anchors and reset stream points`() = runTest {
        val anchor: Anchor = mockk(relaxed = true)
        val pose: Pose = mockk()
        every { pose.tx() } returns 0f
        every { pose.ty() } returns 0f
        every { pose.tz() } returns 0f
        every { anchor.pose } returns pose
        every { anchor.trackingState } returns TrackingState.TRACKING
        every { hitTestProcessor.createAnchorAt(frame, 0.5f, 0.5f) } returns anchor

        repository.updateFrameData(frame)
        repository.createAnchor(0.5f, 0.5f, AnchorSlot.START)
        repository.clearAnchors()

        verify { anchor.detach() }
        assertThat(repository.getSpatialDataStream().value.anchoredStartPoint).isNull()
    }
}
