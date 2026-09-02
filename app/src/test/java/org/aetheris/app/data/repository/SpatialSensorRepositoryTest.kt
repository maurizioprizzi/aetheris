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
import org.aetheris.app.data.arcore.ArCoreAnchorPlacement
import org.aetheris.app.data.arcore.ArCoreFrameProcessor
import org.aetheris.app.data.arcore.ArCoreHitTestProcessor
import org.aetheris.app.domain.model.AnchorPlacement
import org.aetheris.app.domain.model.AnchorPlacementSource
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
    fun `performHitTest is processed against current frame`() =
        runTest {
            val expectedPoint = Point3D(
                x = 1f,
                y = 2f,
                z = 3f
            )

            every {
                hitTestProcessor.performHitTest(
                    frame,
                    any(),
                    any()
                )
            } returns expectedPoint

            val hitResult = async(
                start = CoroutineStart.UNDISPATCHED
            ) {
                repository.performHitTest(
                    normalizedX = 0.5f,
                    normalizedY = 0.5f
                )
            }

            assertThat(hitResult.isCompleted)
                .isFalse()

            repository.onFrameUpdate(frame)

            assertThat(hitResult.await())
                .isEqualTo(expectedPoint)

            verify(exactly = 1) {
                hitTestProcessor.performHitTest(
                    frame,
                    any(),
                    any()
                )
            }
        }

    @Test
    fun `createAnchor stores plane source in start slot`() =
        runTest {
            val pose = createPose(
                x = 1f,
                y = 2f,
                z = 3f
            )

            val anchor = createAnchor(
                pose = pose
            )

            every {
                hitTestProcessor.createAnchorAtWithSource(
                    frame,
                    any(),
                    any()
                )
            } returns ArCoreAnchorPlacement(
                anchor = anchor,
                source = AnchorPlacementSource.PLANE
            )

            val anchorResult = enqueueAnchor(
                slot = AnchorSlot.START
            )

            assertThat(anchorResult.isCompleted)
                .isFalse()

            repository.onFrameUpdate(frame)

            val expectedPoint = Point3D(
                x = 1f,
                y = 2f,
                z = 3f
            )

            assertThat(anchorResult.await())
                .isEqualTo(expectedPoint)

            val data =
                repository.spatialDataStream.value

            assertThat(data.anchoredStartPoint)
                .isEqualTo(expectedPoint)

            assertThat(data.anchoredStartSource)
                .isEqualTo(AnchorPlacementSource.PLANE)

            assertThat(data.anchoredStartPlacement)
                .isEqualTo(
                    AnchorPlacement(
                        position = expectedPoint,
                        source = AnchorPlacementSource.PLANE
                    )
                )

            assertThat(data.anchoredEndPoint)
                .isNull()

            assertThat(data.anchoredEndSource)
                .isNull()

            assertThat(data.hasOnlyConventionalAnchors)
                .isTrue()

            verify(exactly = 1) {
                hitTestProcessor.createAnchorAtWithSource(
                    frame,
                    any(),
                    any()
                )
            }
        }

    @Test
    fun `createAnchor stores instant placement source in end slot`() =
        runTest {
            val pose = createPose(
                x = 0f,
                y = 0f,
                z = -1.5f
            )

            val anchor = createAnchor(
                pose = pose
            )

            every {
                hitTestProcessor.createAnchorAtWithSource(
                    frame,
                    any(),
                    any()
                )
            } returns ArCoreAnchorPlacement(
                anchor = anchor,
                source =
                    AnchorPlacementSource.INSTANT_PLACEMENT
            )

            val anchorResult = enqueueAnchor(
                slot = AnchorSlot.END
            )

            repository.onFrameUpdate(frame)

            assertThat(anchorResult.await())
                .isEqualTo(
                    Point3D(
                        x = 0f,
                        y = 0f,
                        z = -1.5f
                    )
                )

            val data =
                repository.spatialDataStream.value

            assertThat(data.anchoredEndSource)
                .isEqualTo(
                    AnchorPlacementSource.INSTANT_PLACEMENT
                )

            assertThat(data.hasApproximateAnchor)
                .isTrue()

            assertThat(data.hasOnlyConventionalAnchors)
                .isFalse()
        }

    @Test
    fun `anchor pose update preserves placement source`() =
        runTest {
            var currentPose = createPose(
                x = 1f,
                y = 2f,
                z = 3f
            )

            val anchor: Anchor = mockk(relaxed = true)

            every {
                anchor.trackingState
            } returns TrackingState.TRACKING

            every {
                anchor.pose
            } answers {
                currentPose
            }

            every {
                hitTestProcessor.createAnchorAtWithSource(
                    frame,
                    any(),
                    any()
                )
            } returns ArCoreAnchorPlacement(
                anchor = anchor,
                source =
                    AnchorPlacementSource.FEATURE_POINT
            )

            val anchorResult = enqueueAnchor(
                slot = AnchorSlot.START
            )

            repository.onFrameUpdate(frame)
            anchorResult.await()

            currentPose = createPose(
                x = 4f,
                y = 5f,
                z = 6f
            )

            repository.onFrameUpdate(frame)

            val data =
                repository.spatialDataStream.value

            assertThat(data.anchoredStartPoint)
                .isEqualTo(
                    Point3D(
                        x = 4f,
                        y = 5f,
                        z = 6f
                    )
                )

            assertThat(data.anchoredStartSource)
                .isEqualTo(
                    AnchorPlacementSource.FEATURE_POINT
                )
        }

    @Test
    fun `paused anchor preserves previous point and source`() =
        runTest {
            var trackingState =
                TrackingState.TRACKING

            val pose = createPose(
                x = 1f,
                y = 2f,
                z = 3f
            )

            val anchor: Anchor = mockk(relaxed = true)

            every {
                anchor.trackingState
            } answers {
                trackingState
            }

            every {
                anchor.pose
            } returns pose

            every {
                hitTestProcessor.createAnchorAtWithSource(
                    frame,
                    any(),
                    any()
                )
            } returns ArCoreAnchorPlacement(
                anchor = anchor,
                source = AnchorPlacementSource.PLANE
            )

            val anchorResult = enqueueAnchor(
                slot = AnchorSlot.START
            )

            repository.onFrameUpdate(frame)
            anchorResult.await()

            val pointBeforePause =
                repository.spatialDataStream
                    .value
                    .anchoredStartPoint

            trackingState = TrackingState.PAUSED

            repository.onFrameUpdate(frame)

            val pausedData =
                repository.spatialDataStream.value

            assertThat(pausedData.anchoredStartPoint)
                .isEqualTo(pointBeforePause)

            assertThat(pausedData.anchoredStartSource)
                .isEqualTo(AnchorPlacementSource.PLANE)

            assertThat(pausedData.hasCompletePlacementProvenance)
                .isTrue()
        }

    @Test
    fun `stopped anchor clears point and source and detaches anchor`() =
        runTest {
            var trackingState =
                TrackingState.TRACKING

            val anchor: Anchor = mockk(relaxed = true)

            every {
                anchor.trackingState
            } answers {
                trackingState
            }

            every {
                anchor.pose
            } returns createPose(
                x = 1f,
                y = 2f,
                z = 3f
            )

            every {
                hitTestProcessor.createAnchorAtWithSource(
                    frame,
                    any(),
                    any()
                )
            } returns ArCoreAnchorPlacement(
                anchor = anchor,
                source = AnchorPlacementSource.DEPTH_POINT
            )

            val anchorResult = enqueueAnchor(
                slot = AnchorSlot.START
            )

            repository.onFrameUpdate(frame)
            anchorResult.await()

            trackingState = TrackingState.STOPPED

            repository.onFrameUpdate(frame)

            val data =
                repository.spatialDataStream.value

            assertThat(data.anchoredStartPoint)
                .isNull()

            assertThat(data.anchoredStartSource)
                .isNull()

            assertThat(data.anchoredStartPlacement)
                .isNull()

            verify(exactly = 1) {
                anchor.detach()
            }
        }

    @Test
    fun `replacing anchor detaches previous and stores new source`() =
        runTest {
            val firstAnchor = createAnchor(
                pose = createPose(
                    x = 1f,
                    y = 0f,
                    z = 0f
                )
            )

            val secondAnchor = createAnchor(
                pose = createPose(
                    x = 2f,
                    y = 0f,
                    z = 0f
                )
            )

            every {
                hitTestProcessor.createAnchorAtWithSource(
                    frame,
                    any(),
                    any()
                )
            } returnsMany listOf(
                ArCoreAnchorPlacement(
                    anchor = firstAnchor,
                    source = AnchorPlacementSource.PLANE
                ),
                ArCoreAnchorPlacement(
                    anchor = secondAnchor,
                    source =
                        AnchorPlacementSource.INSTANT_PLACEMENT
                )
            )

            val firstResult = enqueueAnchor(
                slot = AnchorSlot.START
            )

            repository.onFrameUpdate(frame)
            firstResult.await()

            val secondResult = enqueueAnchor(
                slot = AnchorSlot.START
            )

            repository.onFrameUpdate(frame)
            secondResult.await()

            val data =
                repository.spatialDataStream.value

            assertThat(data.anchoredStartPoint)
                .isEqualTo(
                    Point3D(
                        x = 2f,
                        y = 0f,
                        z = 0f
                    )
                )

            assertThat(data.anchoredStartSource)
                .isEqualTo(
                    AnchorPlacementSource.INSTANT_PLACEMENT
                )

            verify(exactly = 1) {
                firstAnchor.detach()
            }

            verify(exactly = 0) {
                secondAnchor.detach()
            }
        }

    @Test
    fun `clearAnchors detaches active anchors and clears provenance`() =
        runTest {
            val anchor = createAnchor(
                pose = createPose(
                    x = 1f,
                    y = 2f,
                    z = 3f
                )
            )

            every {
                hitTestProcessor.createAnchorAtWithSource(
                    frame,
                    any(),
                    any()
                )
            } returns ArCoreAnchorPlacement(
                anchor = anchor,
                source =
                    AnchorPlacementSource.INSTANT_PLACEMENT
            )

            val anchorResult = enqueueAnchor(
                slot = AnchorSlot.START
            )

            repository.onFrameUpdate(frame)
            anchorResult.await()

            assertThat(
                repository.spatialDataStream
                    .value
                    .anchoredStartSource
            ).isEqualTo(
                AnchorPlacementSource.INSTANT_PLACEMENT
            )

            repository.clearAnchors()

            val clearedData =
                repository.spatialDataStream.value

            assertThat(clearedData.anchoredStartPoint)
                .isNull()

            assertThat(clearedData.anchoredEndPoint)
                .isNull()

            assertThat(clearedData.anchoredStartSource)
                .isNull()

            assertThat(clearedData.anchoredEndSource)
                .isNull()

            assertThat(clearedData.hasApproximateAnchor)
                .isFalse()

            verify(exactly = 1) {
                anchor.detach()
            }
        }

    private fun createAnchor(
        pose: Pose
    ): Anchor {
        return mockk(relaxed = true) {
            every {
                trackingState
            } returns TrackingState.TRACKING

            every {
                this@mockk.pose
            } returns pose
        }
    }

    private fun createPose(
        x: Float,
        y: Float,
        z: Float
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

    private fun kotlinx.coroutines.CoroutineScope.enqueueAnchor(
        slot: AnchorSlot
    ) = async(
        start = CoroutineStart.UNDISPATCHED
    ) {
        repository.createAnchor(
            normalizedX = 0.5f,
            normalizedY = 0.5f,
            slot = slot
        )
    }
}
