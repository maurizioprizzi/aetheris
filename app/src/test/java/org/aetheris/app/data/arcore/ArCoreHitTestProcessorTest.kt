package org.aetheris.app.data.arcore

import com.google.ar.core.Anchor
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
import io.mockk.verify
import org.aetheris.app.domain.model.Point3D
import org.junit.Before
import org.junit.Test

class ArCoreHitTestProcessorTest {

    private lateinit var processor: ArCoreHitTestProcessor
    private lateinit var frame: Frame
    private lateinit var camera: Camera

    @Before
    fun setUp() {
        processor = ArCoreHitTestProcessor()

        frame = mockk()
        camera = mockk()

        every {
            frame.camera
        } returns camera

        every {
            camera.trackingState
        } returns TrackingState.TRACKING
    }

    @Test
    fun `performHitTest returns point when tracked plane contains hit pose`() {
        val pose = createPose(
            x = 1.0f,
            y = 2.0f,
            z = -3.0f
        )

        val plane: Plane = mockk {
            every {
                trackingState
            } returns TrackingState.TRACKING

            every {
                isPoseInPolygon(pose)
            } returns true
        }

        val hitResult = createHitResult(
            trackable = plane,
            pose = pose
        )

        every {
            frame.hitTest(540f, 960f)
        } returns listOf(hitResult)

        val result = processor.performHitTest(
            frame = frame,
            xPx = 540f,
            yPx = 960f
        )

        assertThat(result).isEqualTo(
            Point3D(
                x = 1.0f,
                y = 2.0f,
                z = -3.0f
            )
        )

        verify(exactly = 1) {
            frame.hitTest(540f, 960f)
        }
    }

    @Test
    fun `performHitTest returns point when tracked feature point exists`() {
        val pose = createPose(
            x = 4.0f,
            y = 5.0f,
            z = -6.0f
        )

        val point: Point = mockk {
            every {
                trackingState
            } returns TrackingState.TRACKING

            every {
                orientationMode
            } returns Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
        }

        val hitResult = createHitResult(
            trackable = point,
            pose = pose
        )

        every {
            frame.hitTest(540f, 960f)
        } returns listOf(hitResult)

        val result = processor.performHitTest(
            frame = frame,
            xPx = 540f,
            yPx = 960f
        )

        assertThat(result).isEqualTo(
            Point3D(
                x = 4.0f,
                y = 5.0f,
                z = -6.0f
            )
        )
    }

    @Test
    fun `performHitTest ignores plane when pose is outside polygon`() {
        val pose = createPose()

        val plane: Plane = mockk {
            every {
                trackingState
            } returns TrackingState.TRACKING

            every {
                isPoseInPolygon(pose)
            } returns false
        }

        val hitResult = createHitResult(
            trackable = plane,
            pose = pose
        )

        every {
            frame.hitTest(540f, 960f)
        } returns listOf(hitResult)

        val result = processor.performHitTest(
            frame = frame,
            xPx = 540f,
            yPx = 960f
        )

        assertThat(result).isNull()
    }

    @Test
    fun `performHitTest ignores trackable when tracking is paused`() {
        val pose = createPose()

        val plane: Plane = mockk {
            every {
                trackingState
            } returns TrackingState.PAUSED
        }

        val hitResult = createHitResult(
            trackable = plane,
            pose = pose
        )

        every {
            frame.hitTest(540f, 960f)
        } returns listOf(hitResult)

        val result = processor.performHitTest(
            frame = frame,
            xPx = 540f,
            yPx = 960f
        )

        assertThat(result).isNull()
    }

    @Test
    fun `performHitTest returns null when hit test is empty`() {
        every {
            frame.hitTest(540f, 960f)
        } returns emptyList()

        val result = processor.performHitTest(
            frame = frame,
            xPx = 540f,
            yPx = 960f
        )

        assertThat(result).isNull()
    }

    @Test
    fun `performHitTest returns null when ARCore throws exception`() {
        every {
            frame.hitTest(540f, 960f)
        } throws IllegalStateException("ARCore indisponível")

        val result = processor.performHitTest(
            frame = frame,
            xPx = 540f,
            yPx = 960f
        )

        assertThat(result).isNull()
    }

    @Test
    fun `createAnchorAt returns anchor for valid tracked plane`() {
        val pose = createPose()
        val anchor: Anchor = mockk()

        val plane: Plane = mockk {
            every {
                trackingState
            } returns TrackingState.TRACKING

            every {
                isPoseInPolygon(pose)
            } returns true
        }

        val hitResult = createHitResult(
            trackable = plane,
            pose = pose
        )

        every {
            hitResult.createAnchor()
        } returns anchor

        every {
            frame.hitTest(540f, 960f)
        } returns listOf(hitResult)

        val result = processor.createAnchorAt(
            frame = frame,
            xPx = 540f,
            yPx = 960f
        )

        assertThat(result).isSameInstanceAs(anchor)

        verify(exactly = 1) {
            hitResult.createAnchor()
        }
    }

    @Test
    fun `createAnchorAt does not create anchor for invalid plane`() {
        val pose = createPose()

        val plane: Plane = mockk {
            every {
                trackingState
            } returns TrackingState.PAUSED
        }

        val hitResult = createHitResult(
            trackable = plane,
            pose = pose
        )

        every {
            frame.hitTest(540f, 960f)
        } returns listOf(hitResult)

        val result = processor.createAnchorAt(
            frame = frame,
            xPx = 540f,
            yPx = 960f
        )

        assertThat(result).isNull()

        verify(exactly = 0) {
            hitResult.createAnchor()
        }
    }

    @Test
    fun `hasValidSurfaceAt returns true when valid surface exists`() {
        val pose = createPose()

        val plane: Plane = mockk {
            every {
                trackingState
            } returns TrackingState.TRACKING

            every {
                isPoseInPolygon(pose)
            } returns true
        }

        val hitResult = createHitResult(
            trackable = plane,
            pose = pose
        )

        every {
            frame.hitTest(540f, 960f)
        } returns listOf(hitResult)

        val result = processor.hasValidSurfaceAt(
            frame = frame,
            xPx = 540f,
            yPx = 960f
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `hasValidSurfaceAt returns false when no valid surface exists`() {
        every {
            frame.hitTest(540f, 960f)
        } returns emptyList()

        val result = processor.hasValidSurfaceAt(
            frame = frame,
            xPx = 540f,
            yPx = 960f
        )

        assertThat(result).isFalse()
    }

    private fun createPose(
        x: Float = 0.0f,
        y: Float = 0.0f,
        z: Float = 0.0f
    ): Pose {
        return mockk {
            every { tx() } returns x
            every { ty() } returns y
            every { tz() } returns z
        }
    }

    private fun createHitResult(
        trackable: Trackable,
        pose: Pose
    ): HitResult {
        return mockk {
            every {
                this@mockk.trackable
            } returns trackable

            every {
                hitPose
            } returns pose
        }
    }
}