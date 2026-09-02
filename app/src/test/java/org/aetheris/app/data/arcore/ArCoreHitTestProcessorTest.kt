package org.aetheris.app.data.arcore

import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.Trackable
import com.google.ar.core.TrackingState
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.aetheris.app.domain.model.AnchorPlacementSource
import org.aetheris.app.domain.model.Point3D
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class ArCoreHitTestProcessorTest {

    private lateinit var processor: ArCoreHitTestProcessor
    private lateinit var frame: Frame
    private lateinit var camera: Camera

    @Before
    fun setUp() {
        processor = ArCoreHitTestProcessor(
            diagnosticsEnabled = false
        )

        frame = mockk()
        camera = mockk()

        every {
            frame.camera
        } returns camera

        every {
            camera.trackingState
        } returns TrackingState.TRACKING

        every {
            frame.hitTest(any(), any())
        } returns emptyList()

        every {
            frame.hitTestInstantPlacement(
                any(),
                any(),
                any()
            )
        } returns emptyList()
    }

    @Test
    fun `performHitTest preserves legacy point return for valid plane`() {
        val pose = createPose(
            x = 1f,
            y = 2f,
            z = -3f
        )

        val plane = createPlane(
            pose = pose,
            isPoseInPolygon = true
        )

        val hit = createHitResult(
            trackable = plane,
            pose = pose
        )

        every {
            frame.hitTest(540f, 960f)
        } returns listOf(hit)

        val result = processor.performHitTest(
            frame = frame,
            xPx = 540f,
            yPx = 960f
        )

        assertThat(result).isEqualTo(
            Point3D(
                x = 1f,
                y = 2f,
                z = -3f
            )
        )
    }

    @Test
    fun `performHitTestWithSource identifies plane`() {
        val pose = createPose(
            x = 1f,
            y = 2f,
            z = -3f
        )

        val plane = createPlane(
            pose = pose,
            isPoseInPolygon = true
        )

        every {
            frame.hitTest(540f, 960f)
        } returns listOf(
            createHitResult(
                trackable = plane,
                pose = pose
            )
        )

        val result = requireNotNull(
            processor.performHitTestWithSource(
                frame = frame,
                xPx = 540f,
                yPx = 960f
            )
        )

        assertThat(result.position).isEqualTo(
            Point3D(
                x = 1f,
                y = 2f,
                z = -3f
            )
        )

        assertThat(result.source)
            .isEqualTo(AnchorPlacementSource.PLANE)

        assertThat(result.isConventional)
            .isTrue()
    }

    @Test
    fun `performHitTestWithSource identifies oriented feature point`() {
        val pose = createPose(
            x = 4f,
            y = 5f,
            z = -6f
        )

        val point: Point = mockk {
            every {
                trackingState
            } returns TrackingState.TRACKING

            every {
                orientationMode
            } returns Point.OrientationMode
                .ESTIMATED_SURFACE_NORMAL
        }

        every {
            frame.hitTest(540f, 960f)
        } returns listOf(
            createHitResult(
                trackable = point,
                pose = pose
            )
        )

        val result = requireNotNull(
            processor.performHitTestWithSource(
                frame = frame,
                xPx = 540f,
                yPx = 960f
            )
        )

        assertThat(result.position).isEqualTo(
            Point3D(
                x = 4f,
                y = 5f,
                z = -6f
            )
        )

        assertThat(result.source)
            .isEqualTo(
                AnchorPlacementSource.FEATURE_POINT
            )
    }

    @Test
    fun `performHitTestWithSource identifies depth point`() {
        val pose = createPose(
            x = 0.5f,
            y = 1f,
            z = -2f
        )

        val depthPoint: DepthPoint = mockk {
            every {
                trackingState
            } returns TrackingState.TRACKING
        }

        every {
            frame.hitTest(540f, 960f)
        } returns listOf(
            createHitResult(
                trackable = depthPoint,
                pose = pose
            )
        )

        val result = requireNotNull(
            processor.performHitTestWithSource(
                frame = frame,
                xPx = 540f,
                yPx = 960f
            )
        )

        assertThat(result.source)
            .isEqualTo(
                AnchorPlacementSource.DEPTH_POINT
            )

        assertThat(result.usesDepth)
            .isTrue()
    }

    @Test
    fun `performHitTestWithSource uses instant placement fallback`() {
        val pose = createPose(
            x = 0f,
            y = 0f,
            z = -1.5f
        )

        val instantPoint: InstantPlacementPoint = mockk {
            every {
                trackingState
            } returns TrackingState.TRACKING
        }

        every {
            frame.hitTest(540f, 960f)
        } returns emptyList()

        every {
            frame.hitTestInstantPlacement(
                540f,
                960f,
                1.5f
            )
        } returns listOf(
            createHitResult(
                trackable = instantPoint,
                pose = pose
            )
        )

        val result = requireNotNull(
            processor.performHitTestWithSource(
                frame = frame,
                xPx = 540f,
                yPx = 960f
            )
        )

        assertThat(result.position).isEqualTo(
            Point3D(
                x = 0f,
                y = 0f,
                z = -1.5f
            )
        )

        assertThat(result.source)
            .isEqualTo(
                AnchorPlacementSource.INSTANT_PLACEMENT
            )

        assertThat(result.isApproximate)
            .isTrue()
    }

    @Test
    fun `conventional hit has priority over instant placement`() {
        val pose = createPose()

        val plane = createPlane(
            pose = pose,
            isPoseInPolygon = true
        )

        every {
            frame.hitTest(540f, 960f)
        } returns listOf(
            createHitResult(
                trackable = plane,
                pose = pose
            )
        )

        val result = requireNotNull(
            processor.performHitTestWithSource(
                frame = frame,
                xPx = 540f,
                yPx = 960f
            )
        )

        assertThat(result.source)
            .isEqualTo(AnchorPlacementSource.PLANE)

        verify(exactly = 0) {
            frame.hitTestInstantPlacement(
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `invalid plane falls back to instant placement`() {
        val planePose = createPose()

        val plane = createPlane(
            pose = planePose,
            isPoseInPolygon = false
        )

        val instantPose = createPose(
            z = -1.5f
        )

        val instantPoint: InstantPlacementPoint = mockk {
            every {
                trackingState
            } returns TrackingState.TRACKING
        }

        every {
            frame.hitTest(540f, 960f)
        } returns listOf(
            createHitResult(
                trackable = plane,
                pose = planePose
            )
        )

        every {
            frame.hitTestInstantPlacement(
                540f,
                960f,
                1.5f
            )
        } returns listOf(
            createHitResult(
                trackable = instantPoint,
                pose = instantPose
            )
        )

        val result = requireNotNull(
            processor.performHitTestWithSource(
                frame = frame,
                xPx = 540f,
                yPx = 960f
            )
        )

        assertThat(result.source)
            .isEqualTo(
                AnchorPlacementSource.INSTANT_PLACEMENT
            )
    }

    @Test
    fun `feature point without surface normal is rejected`() {
        val pose = createPose()

        val point: Point = mockk {
            every {
                trackingState
            } returns TrackingState.TRACKING

            every {
                orientationMode
            } returns Point.OrientationMode.INITIALIZED_TO_IDENTITY
        }

        every {
            frame.hitTest(540f, 960f)
        } returns listOf(
            createHitResult(
                trackable = point,
                pose = pose
            )
        )

        val result = processor.performHitTestWithSource(
            frame = frame,
            xPx = 540f,
            yPx = 960f
        )

        assertThat(result).isNull()
    }

    @Test
    fun `paused trackable is rejected`() {
        val pose = createPose()

        val plane: Plane = mockk {
            every {
                trackingState
            } returns TrackingState.PAUSED
        }

        every {
            frame.hitTest(540f, 960f)
        } returns listOf(
            createHitResult(
                trackable = plane,
                pose = pose
            )
        )

        val result = processor.performHitTestWithSource(
            frame = frame,
            xPx = 540f,
            yPx = 960f
        )

        assertThat(result).isNull()
    }

    @Test
    fun `request is rejected when camera is not tracking`() {
        every {
            camera.trackingState
        } returns TrackingState.PAUSED

        val result = processor.performHitTestWithSource(
            frame = frame,
            xPx = 540f,
            yPx = 960f
        )

        assertThat(result).isNull()

        verify(exactly = 0) {
            frame.hitTest(any(), any())
        }

        verify(exactly = 0) {
            frame.hitTestInstantPlacement(
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `invalid screen coordinates are rejected before frame access`() {
        val result = processor.performHitTestWithSource(
            frame = frame,
            xPx = Float.NaN,
            yPx = -1f
        )

        assertThat(result).isNull()

        verify(exactly = 0) {
            frame.hitTest(any(), any())
        }
    }

    @Test
    fun `ARCore runtime failure returns null`() {
        every {
            frame.hitTest(540f, 960f)
        } throws IllegalStateException(
            "ARCore indisponível"
        )

        val result = processor.performHitTestWithSource(
            frame = frame,
            xPx = 540f,
            yPx = 960f
        )

        assertThat(result).isNull()
    }

    @Test
    fun `createAnchorAt preserves legacy anchor return`() {
        val pose = createPose()
        val anchor: Anchor = mockk()

        val plane = createPlane(
            pose = pose,
            isPoseInPolygon = true
        )

        val hit = createHitResult(
            trackable = plane,
            pose = pose
        )

        every {
            hit.createAnchor()
        } returns anchor

        every {
            frame.hitTest(540f, 960f)
        } returns listOf(hit)

        val result = processor.createAnchorAt(
            frame = frame,
            xPx = 540f,
            yPx = 960f
        )

        assertThat(result)
            .isSameInstanceAs(anchor)

        verify(exactly = 1) {
            hit.createAnchor()
        }
    }

    @Test
    fun `createAnchorAtWithSource returns anchor and plane source`() {
        val pose = createPose()
        val anchor: Anchor = mockk()

        val plane = createPlane(
            pose = pose,
            isPoseInPolygon = true
        )

        val hit = createHitResult(
            trackable = plane,
            pose = pose
        )

        every {
            hit.createAnchor()
        } returns anchor

        every {
            frame.hitTest(540f, 960f)
        } returns listOf(hit)

        val result = requireNotNull(
            processor.createAnchorAtWithSource(
                frame = frame,
                xPx = 540f,
                yPx = 960f
            )
        )

        assertThat(result.anchor)
            .isSameInstanceAs(anchor)

        assertThat(result.source)
            .isEqualTo(AnchorPlacementSource.PLANE)

        assertThat(result.isApproximate)
            .isFalse()
    }

    @Test
    fun `createAnchorAtWithSource identifies instant placement`() {
        val pose = createPose(
            z = -1.5f
        )

        val anchor: Anchor = mockk()

        val instantPoint: InstantPlacementPoint = mockk {
            every {
                trackingState
            } returns TrackingState.TRACKING
        }

        val hit = createHitResult(
            trackable = instantPoint,
            pose = pose
        )

        every {
            hit.createAnchor()
        } returns anchor

        every {
            frame.hitTestInstantPlacement(
                540f,
                960f,
                1.5f
            )
        } returns listOf(hit)

        val result = requireNotNull(
            processor.createAnchorAtWithSource(
                frame = frame,
                xPx = 540f,
                yPx = 960f
            )
        )

        assertThat(result.anchor)
            .isSameInstanceAs(anchor)

        assertThat(result.source)
            .isEqualTo(
                AnchorPlacementSource.INSTANT_PLACEMENT
            )

        assertThat(result.isApproximate)
            .isTrue()
    }

    @Test
    fun `createAnchorAtWithSource does not create anchor without valid hit`() {
        val result = processor.createAnchorAtWithSource(
            frame = frame,
            xPx = 540f,
            yPx = 960f
        )

        assertThat(result).isNull()
    }

    @Test
    fun `surface probe accepts conventional geometry`() {
        val pose = createPose()

        val plane = createPlane(
            pose = pose,
            isPoseInPolygon = true
        )

        every {
            frame.hitTest(540f, 960f)
        } returns listOf(
            createHitResult(
                trackable = plane,
                pose = pose
            )
        )

        val result = processor.hasValidSurfaceAt(
            frame = frame,
            xPx = 540f,
            yPx = 960f
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `surface probe never uses instant placement`() {
        val result = processor.hasValidSurfaceAt(
            frame = frame,
            xPx = 540f,
            yPx = 960f
        )

        assertThat(result).isFalse()

        verify(exactly = 0) {
            frame.hitTestInstantPlacement(
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `invalid approximate distance is rejected`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            ArCoreHitTestProcessor(
                approximateDistanceMeters = 0f,
                diagnosticsEnabled = false
            )
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            ArCoreHitTestProcessor(
                approximateDistanceMeters = Float.NaN,
                diagnosticsEnabled = false
            )
        }
    }

    private fun createPlane(
        pose: Pose,
        isPoseInPolygon: Boolean
    ): Plane {
        return mockk {
            every {
                trackingState
            } returns TrackingState.TRACKING

            every {
                isPoseInPolygon(pose)
            } returns isPoseInPolygon
        }
    }

    private fun createPose(
        x: Float = 0f,
        y: Float = 0f,
        z: Float = 0f
    ): Pose {
        return mockk {
            every {
                tx()
            } returns x

            every {
                ty()
            } returns y

            every {
                tz()
            } returns z
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
