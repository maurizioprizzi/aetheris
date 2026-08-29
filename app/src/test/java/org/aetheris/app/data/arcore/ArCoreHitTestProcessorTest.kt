package org.aetheris.app.data.arcore

import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.aetheris.app.domain.model.Point3D
import org.junit.Before
import org.junit.Test

class ArCoreHitTestProcessorTest {

    private lateinit var processor: ArCoreHitTestProcessor
    private val frame: Frame = mockk()

    @Before
    fun setUp() {
        processor = ArCoreHitTestProcessor()
    }

    @Test
    fun `performHitTest should return Point3D when plane is tracking and pose is in polygon`() {
        val hitResult: HitResult = mockk()
        val plane: Plane = mockk()
        val pose: Pose = mockk()

        every { pose.tx() } returns 1.0f
        every { pose.ty() } returns 2.0f
        every { pose.tz() } returns -3.0f
        every { plane.trackingState } returns TrackingState.TRACKING
        every { plane.isPoseInPolygon(pose) } returns true
        every { hitResult.trackable } returns plane
        every { hitResult.hitPose } returns pose
        every { frame.hitTest(0.5f, 0.5f) } returns listOf(hitResult)

        val result = processor.performHitTest(frame, 0.5f, 0.5f)

        assertThat(result).isEqualTo(Point3D(1.0f, 2.0f, -3.0f))
    }

    @Test
    fun `createAnchorAt should return Anchor when trackable is valid`() {
        val hitResult: HitResult = mockk()
        val plane: Plane = mockk()
        val pose: Pose = mockk()
        val anchor: Anchor = mockk()

        every { plane.trackingState } returns TrackingState.TRACKING
        every { plane.isPoseInPolygon(pose) } returns true
        every { hitResult.trackable } returns plane
        every { hitResult.hitPose } returns pose
        every { hitResult.createAnchor() } returns anchor
        every { frame.hitTest(0.5f, 0.5f) } returns listOf(hitResult)

        val result = processor.createAnchorAt(frame, 0.5f, 0.5f)

        assertThat(result).isEqualTo(anchor)
    }

    @Test
    fun `hasValidSurfaceAt should return true when surface exists`() {
        val hitResult: HitResult = mockk()
        val plane: Plane = mockk()
        val pose: Pose = mockk()

        every { pose.tx() } returns 0f
        every { pose.ty() } returns 0f
        every { pose.tz() } returns 0f
        every { plane.trackingState } returns TrackingState.TRACKING
        every { plane.isPoseInPolygon(pose) } returns true
        every { hitResult.trackable } returns plane
        every { hitResult.hitPose } returns pose
        every { frame.hitTest(0.5f, 0.5f) } returns listOf(hitResult)

        assertThat(processor.hasValidSurfaceAt(frame, 0.5f, 0.5f)).isTrue()
    }
}
