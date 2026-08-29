package org.aetheris.app.presentation.measurement

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
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

    private val testDispatcher = StandardTestDispatcher()
    private val spatialSensorRepository: SpatialSensorRepository = mockk(relaxed = true)
    private val calculateDistanceUseCase = CalculateDistanceUseCase()
    private val projectWorldToScreenUseCase = ProjectWorldToScreenUseCase()

    private val spatialDataFlow = MutableStateFlow(SpatialFrameData())
    private lateinit var viewModel: MeasurementViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { spatialSensorRepository.getSpatialDataStream() } returns spatialDataFlow

        viewModel = MeasurementViewModel(
            spatialSensorRepository = spatialSensorRepository,
            calculateDistanceUseCase = calculateDistanceUseCase,
            projectWorldToScreenUseCase = projectWorldToScreenUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when repository emits tracking data, uiState updates reactively`() = runTest {
        spatialDataFlow.value = SpatialFrameData(
            trackingStatus = TrackingStatus.TRACKING,
            isDepthEnabled = true,
            pointCount = 150,
            isSurfaceDetected = true
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.trackingStatus).isEqualTo(TrackingStatus.TRACKING)
        assertThat(state.isDepthActive).isTrue()
        assertThat(state.detectedPointsCount).isEqualTo(150)
        assertThat(state.isTargetingSurface).isTrue()
    }

    @Test
    fun `onAnchorPointTapped should request native anchor creation for start and end slots`() = runTest {
        val pointA = Point3D(0f, 0f, 0f)
        val pointB = Point3D(0f, 3f, 4f)

        coEvery { spatialSensorRepository.createAnchor(0.5f, 0.5f, AnchorSlot.START) } returns pointA
        viewModel.onAnchorPointTapped()
        testScheduler.advanceUntilIdle()

        spatialDataFlow.value = spatialDataFlow.value.copy(anchoredStartPoint = pointA)
        testScheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedStartPoint).isEqualTo(pointA)

        coEvery { spatialSensorRepository.createAnchor(0.5f, 0.5f, AnchorSlot.END) } returns pointB
        viewModel.onAnchorPointTapped()
        testScheduler.advanceUntilIdle()

        spatialDataFlow.value = spatialDataFlow.value.copy(anchoredEndPoint = pointB)
        testScheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedEndPoint).isEqualTo(pointB)
        assertThat(viewModel.uiState.value.currentMeasurement?.meters).isWithin(0.001f).of(5.0f)
    }

    @Test
    fun `onResetMeasurements should clear repository anchors and local state`() = runTest {
        viewModel.onResetMeasurements()
        testScheduler.advanceUntilIdle()

        verify { spatialSensorRepository.clearAnchors() }
        val state = viewModel.uiState.value
        assertThat(state.selectedStartPoint).isNull()
        assertThat(state.selectedEndPoint).isNull()
        assertThat(state.currentMeasurement).isNull()
        assertThat(state.badgePosition).isNull()
    }
}