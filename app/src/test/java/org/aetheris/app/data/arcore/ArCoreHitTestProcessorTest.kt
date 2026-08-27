package org.aetheris.app.data.arcore

import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.Trackable
import com.google.ar.core.TrackingState
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.aetheris.app.domain.model.Point3D
import org.junit.Before
import org.junit.Test

class ArCoreHitTestProcessorTest {

    private lateinit var processor: ArCoreHitTestProcessor
    private val defaultWidth = 1080
    private val defaultHeight = 1920

    @Before
    fun setUp() {
        processor = ArCoreHitTestProcessor()
    }

    @Test
    fun `performRaycast should return null when frame is null`() {
        val result = processor.performRaycast(
            frame = null,
            normalizedX = 0.5f,
            normalizedY = 0.5f,
            viewportWidth = defaultWidth,
            viewportHeight = defaultHeight
        )

        assertThat(result).isNull()
    }

    @Test
    fun `performRaycast should return null when camera tracking state is not TRACKING`() {
        val frame = mockk<Frame>()
        val camera = mockk<Camera>()
        every { frame.camera } returns camera
        every { camera.trackingState } returns TrackingState.PAUSED

        val result = processor.performRaycast(
            frame = frame,
            normalizedX = 0.5f,
            normalizedY = 0.5f,
            viewportWidth = defaultWidth,
            viewportHeight = defaultHeight
        )

        assertThat(result).isNull()
    }

    @Test
    fun `performRaycast should return null when viewport dimensions are invalid`() {
        val frame = mockk<Frame>()
        val camera = mockk<Camera>()
        every { frame.camera } returns camera
        every { camera.trackingState } returns TrackingState.TRACKING

        val resultZeroWidth = processor.performRaycast(
            frame = frame,
            normalizedX = 0.5f,
            normalizedY = 0.5f,
            viewportWidth = 0,
            viewportHeight = 1920
        )

        val resultNegativeHeight = processor.performRaycast(
            frame = frame,
            normalizedX = 0.5f,
            normalizedY = 0.5f,
            viewportWidth = 1080,
            viewportHeight = -1
        )

        assertThat(resultZeroWidth).isNull()
        assertThat(resultNegativeHeight).isNull()
    }

    @Test
    fun `performRaycast should return Point3D when hit hits a valid Plane within polygon`() {
        val frame = mockk<Frame>()
        val camera = mockk<Camera>()
        val hitResult = mockk<HitResult>()
        val plane = mockk<Plane>()
        val pose = mockk<Pose>()

        every { frame.camera } returns camera
        every { camera.trackingState } returns TrackingState.TRACKING
        every { frame.hitTest(540f, 960f) } returns listOf(hitResult)

        every { hitResult.trackable } returns plane
        every { hitResult.hitPose } returns pose
        every { plane.trackingState } returns TrackingState.TRACKING
        every { plane.isPoseInPolygon(pose) } returns true

        every { pose.tx() } returns 0.25f
        every { pose.ty() } returns -0.15f
        every { pose.tz() } returns -1.80f

        val result = processor.performRaycast(
            frame = frame,
            normalizedX = 0.5f,
            normalizedY = 0.5f,
            viewportWidth = defaultWidth,
            viewportHeight = defaultHeight
        )

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(Point3D(x = 0.25f, y = -0.15f, z = -1.80f))
    }

    @Test
    fun `performRaycast should discard Plane when hit is outside polygon`() {
        val frame = mockk<Frame>()
        val camera = mockk<Camera>()
        val hitResult = mockk<HitResult>()
        val plane = mockk<Plane>()
        val pose = mockk<Pose>()

        every { frame.camera } returns camera
        every { camera.trackingState } returns TrackingState.TRACKING
        every { frame.hitTest(540f, 960f) } returns listOf(hitResult)

        every { hitResult.trackable } returns plane
        every { hitResult.hitPose } returns pose
        every { plane.trackingState } returns TrackingState.TRACKING
        every { plane.isPoseInPolygon(pose) } returns false

        val result = processor.performRaycast(
            frame = frame,
            normalizedX = 0.5f,
            normalizedY = 0.5f,
            viewportWidth = defaultWidth,
            viewportHeight = defaultHeight
        )

        assertThat(result).isNull()
    }

    @Test
    fun `performRaycast should fallback to Depth Point when hit is on active Point`() {
        val frame = mockk<Frame>()
        val camera = mockk<Camera>()
        val hitResult = mockk<HitResult>()
        val point = mockk<Point>()
        val pose = mockk<Pose>()

        every { frame.camera } returns camera
        every { camera.trackingState } returns TrackingState.TRACKING
        every { frame.hitTest(540f, 960f) } returns listOf(hitResult)

        every { hitResult.trackable } returns point
        every { hitResult.hitPose } returns pose
        every { point.trackingState } returns TrackingState.TRACKING

        every { pose.tx() } returns 1.10f
        every { pose.ty() } returns 0.05f
        every { pose.tz() } returns -2.40f

        val result = processor.performRaycast(
            frame = frame,
            normalizedX = 0.5f,
            normalizedY = 0.5f,
            viewportWidth = defaultWidth,
            viewportHeight = defaultHeight
        )

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(Point3D(x = 1.10f, y = 0.05f, z = -2.40f))
    }

    @Test
    fun `performRaycast should clamp out-of-bounds normalized coordinates to viewport range`() {
        val frame = mockk<Frame>()
        val camera = mockk<Camera>()

        every { frame.camera } returns camera
        every { camera.trackingState } returns TrackingState.TRACKING
        every { frame.hitTest(0f, 1920f) } returns emptyList()

        val result = processor.performRaycast(
            frame = frame,
            normalizedX = -0.5f,
            normalizedY = 1.8f,
            viewportWidth = defaultWidth,
            viewportHeight = defaultHeight
        )

        assertThat(result).isNull()
    }
}