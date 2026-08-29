package org.aetheris.app.presentation.measurement

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.aetheris.app.domain.model.AnchorSlot
import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.SpatialFrameData
import org.aetheris.app.domain.model.TrackingStatus
import org.aetheris.app.domain.repository.SpatialSensorRepository
import org.aetheris.app.domain.usecase.CalculateDistanceUseCase
import org.aetheris.app.domain.usecase.ProjectWorldToScreenUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementViewModelTest {

    private val testDispatcher =
        StandardTestDispatcher()

    private class FakeSpatialSensorRepository :
        SpatialSensorRepository {

        private val _stream =
            MutableStateFlow(
                SpatialFrameData(
                    trackingStatus =
                        TrackingStatus.TRACKING,
                    isDepthEnabled = true,
                    pointCount = 150,
                    isSurfaceDetected = true,
                    anchoredStartPoint = null,
                    anchoredEndPoint = null
                )
            )

        override val spatialDataStream:
                StateFlow<SpatialFrameData> =
            _stream

        var clearAnchorsCallCount: Int = 0
            private set

        override suspend fun performHitTest(
            normalizedX: Float,
            normalizedY: Float
        ): Point3D? {
            return null
        }

        override suspend fun createAnchor(
            normalizedX: Float,
            normalizedY: Float,
            slot: AnchorSlot
        ): Point3D? {
            return null
        }

        override fun clearAnchors() {
            clearAnchorsCallCount++
        }
    }

    private lateinit var fakeRepository:
            FakeSpatialSensorRepository

    private val calculateDistanceUseCase =
        CalculateDistanceUseCase()

    private val projectWorldToScreenUseCase:
            ProjectWorldToScreenUseCase =
        mockk(relaxed = true)

    private lateinit var viewModel:
            MeasurementViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(
            testDispatcher
        )

        fakeRepository =
            FakeSpatialSensorRepository()

        viewModel = MeasurementViewModel(
            spatialSensorRepository =
                fakeRepository,
            calculateDistanceUseCase =
                calculateDistanceUseCase,
            projectWorldToScreenUseCase =
                projectWorldToScreenUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state reflects tracking and depth from repository stream`() {
        testDispatcher.scheduler
            .advanceUntilIdle()

        val state =
            viewModel.uiState.value

        assertThat(state.trackingStatus)
            .isEqualTo(
                TrackingStatus.TRACKING
            )

        assertThat(state.isDepthEnabled)
            .isTrue()

        assertThat(state.detectedPointsCount)
            .isEqualTo(150)

        assertThat(state.isTargetingSurface)
            .isTrue()
    }

    @Test
    fun `onSurfaceDimensionsChanged updates viewport dimensions in state`() {
        viewModel.onSurfaceDimensionsChanged(
            widthPx = 1080,
            heightPx = 1920
        )

        val state =
            viewModel.uiState.value

        assertThat(state.viewportWidthPx)
            .isEqualTo(1080)

        assertThat(state.viewportHeightPx)
            .isEqualTo(1920)

        assertThat(state.hasValidViewport)
            .isTrue()
    }

    @Test
    fun `onResetMeasurements clears points and resets anchors`() {
        viewModel.onResetMeasurements()

        val state =
            viewModel.uiState.value

        assertThat(state.selectedStartPoint)
            .isNull()

        assertThat(state.selectedEndPoint)
            .isNull()

        assertThat(state.currentMeasurement)
            .isNull()

        assertThat(state.badgePosition)
            .isNull()

        assertThat(
            fakeRepository.clearAnchorsCallCount
        ).isEqualTo(1)
    }
}