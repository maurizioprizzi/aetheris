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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.aetheris.app.data.arcore.ArCoreFrameProcessor
import org.aetheris.app.data.arcore.ArCoreHitTestProcessor
import org.aetheris.app.domain.model.AnchorSlot
import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.TrackingStatus
import org.junit.Before
import org.junit.Test

class SpatialSensorRepositoryTest {

    private lateinit var frameProcessor: ArCoreFrameProcessor
    private lateinit var hitTestProcessor: ArCoreHitTestProcessor
    private lateinit var repository: SpatialSensorRepositoryImpl

    private lateinit var frame: Frame
    private lateinit var camera: Camera

    @Before
    fun setUp() {
        frameProcessor = mockk(relaxed = true)
        hitTestProcessor = mockk(relaxed = true)

        frame = mockk(relaxed = true)
        camera = mockk(relaxed = true)

        every {
            frame.camera
        } returns camera

        every {
            camera.trackingState
        } returns TrackingState.TRACKING

        repository = SpatialSensorRepositoryImpl(
            frameProcessor = frameProcessor,
            hitTestProcessor = hitTestProcessor,
            isDepthEnabledProvider = { true }
        )

        repository.updateViewportSize(
            widthPx = 1080,
            heightPx = 1920
        )
    }

    @Test
    fun `onFrameUpdate emits updated spatial data from frame`() {
        every {
            frameProcessor.processPointCloud(frame)
        } returns listOf(
            Point3D(1f, 2f, 3f),
            Point3D(4f, 5f, 6f)
        )

        every {
            hitTestProcessor.hasValidSurfaceAt(
                frame,
                any(),
                any()
            )
        } returns true

        repository.onFrameUpdate(frame)

        val currentData =
            repository.spatialDataStream.value

        assertThat(currentData.trackingStatus)
            .isEqualTo(TrackingStatus.TRACKING)

        assertThat(currentData.pointCount)
            .isEqualTo(2)

        assertThat(currentData.isSurfaceDetected)
            .isTrue()

        assertThat(currentData.isDepthEnabled)
            .isTrue()
    }

    @Test
    fun `createAnchor creates anchor via hit test processor and attaches slot`() =
        runTest {
            val anchor: Anchor =
                mockk(relaxed = true)

            val pose = mockk<Pose> {
                every { tx() } returns 1f
                every { ty() } returns 2f
                every { tz() } returns 3f
            }

            every {
                anchor.trackingState
            } returns TrackingState.TRACKING

            every {
                anchor.pose
            } returns pose

            every {
                hitTestProcessor.createAnchorAt(
                    frame,
                    any(),
                    any()
                )
            } returns anchor

            val anchorResult = async(
                start = CoroutineStart.UNDISPATCHED
            ) {
                repository.createAnchor(
                    normalizedX = 0.5f,
                    normalizedY = 0.5f,
                    slot = AnchorSlot.START
                )
            }

            assertThat(anchorResult.isCompleted)
                .isFalse()

            repository.onFrameUpdate(frame)

            assertThat(anchorResult.await())
                .isEqualTo(Point3D(1f, 2f, 3f))

            val data =
                repository.spatialDataStream.value

            assertThat(data.anchoredStartPoint)
                .isEqualTo(Point3D(1f, 2f, 3f))

            assertThat(data.anchoredEndPoint)
                .isNull()

            verify(exactly = 1) {
                hitTestProcessor.createAnchorAt(
                    frame,
                    any(),
                    any()
                )
            }
        }

    @Test
    fun `clearAnchors detaches active anchors and resets stream state`() =
        runTest {
            val anchor: Anchor =
                mockk(relaxed = true)

            val pose = mockk<Pose> {
                every { tx() } returns 1f
                every { ty() } returns 2f
                every { tz() } returns 3f
            }

            every {
                anchor.trackingState
            } returns TrackingState.TRACKING

            every {
                anchor.pose
            } returns pose

            every {
                hitTestProcessor.createAnchorAt(
                    frame,
                    any(),
                    any()
                )
            } returns anchor

            val anchorResult = async(
                start = CoroutineStart.UNDISPATCHED
            ) {
                repository.createAnchor(
                    normalizedX = 0.5f,
                    normalizedY = 0.5f,
                    slot = AnchorSlot.START
                )
            }

            repository.onFrameUpdate(frame)

            assertThat(anchorResult.await())
                .isEqualTo(Point3D(1f, 2f, 3f))

            assertThat(
                repository
                    .spatialDataStream
                    .value
                    .anchoredStartPoint
            ).isNotNull()

            repository.clearAnchors()
            repository.onFrameUpdate(frame)

            assertThat(
                repository
                    .spatialDataStream
                    .value
                    .anchoredStartPoint
            ).isNull()

            assertThat(
                repository
                    .spatialDataStream
                    .value
                    .anchoredEndPoint
            ).isNull()

            verify(exactly = 1) {
                anchor.detach()
            }
        }
}
