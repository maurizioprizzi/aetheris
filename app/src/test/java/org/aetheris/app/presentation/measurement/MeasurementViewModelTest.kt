package org.aetheris.app.presentation.measurement

import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.SpatialFrameData
import org.aetheris.app.domain.model.TrackingStatus
import org.aetheris.app.domain.repository.SpatialSensorRepository
import org.aetheris.app.domain.usecase.CalculateDistanceUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<SpatialSensorRepository>(relaxed = true)
    private val calculateDistanceUseCase = CalculateDistanceUseCase()
    private val spatialStream = MutableStateFlow(SpatialFrameData())

    private lateinit var viewModel: MeasurementViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.spatialFrameStream } returns spatialStream
        viewModel = MeasurementViewModel(repository, calculateDistanceUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have null points and zero measurements`() {
        val state = viewModel.uiState.value

        assertThat(state.selectedStartPoint).isNull()
        assertThat(state.selectedEndPoint).isNull()
        assertThat(state.currentMeasurement).isNull()
    }

    @Test
    fun `tapping anchor should set Point A when no points are selected`() {
        val mockHitPoint = Point3D(x = 0.0f, y = 0.0f, z = -1.5f)
        every { repository.performHitTest(0.5f, 0.5f) } returns mockHitPoint

        viewModel.onAnchorPointTapped()

        val state = viewModel.uiState.value
        assertThat(state.selectedStartPoint).isEqualTo(mockHitPoint)
        assertThat(state.selectedEndPoint).isNull()
        assertThat(state.currentMeasurement).isNull()
    }

    @Test
    fun `tapping anchor should set Point B and calculate deterministic distance when Point A exists`() {
        val pointA = Point3D(x = 0.0f, y = 0.0f, z = 0.0f)
        val pointB = Point3D(x = 3.0f, y = 4.0f, z = 0.0f)

        every { repository.performHitTest(0.5f, 0.5f) } returns pointA andThen pointB

        // Primeiro toque -> Ponto A
        viewModel.onAnchorPointTapped()
        // Segundo toque -> Ponto B
        viewModel.onAnchorPointTapped()

        val state = viewModel.uiState.value
        assertThat(state.selectedStartPoint).isEqualTo(pointA)
        assertThat(state.selectedEndPoint).isEqualTo(pointB)

        val measurement = state.currentMeasurement
        assertThat(measurement).isNotNull()
        assertThat(measurement!!.meters.toDouble()).isWithin(1e-4).of(5.0)
        assertThat(measurement.uncertaintyMeters.toDouble()).isGreaterThan(0.0)
    }

    @Test
    fun `tapping anchor after both points are set should reset and assign new Point A`() {
        val pointA = Point3D(x = 0.0f, y = 0.0f, z = 0.0f)
        val pointB = Point3D(x = 1.0f, y = 0.0f, z = 0.0f)
        val pointC = Point3D(x = 5.0f, y = 5.0f, z = 5.0f)

        every { repository.performHitTest(0.5f, 0.5f) } returnsMany listOf(pointA, pointB, pointC)

        viewModel.onAnchorPointTapped() // Fixa Ponto A
        viewModel.onAnchorPointTapped() // Fixa Ponto B
        viewModel.onAnchorPointTapped() // Reinicia ciclo com Ponto C

        val state = viewModel.uiState.value
        assertThat(state.selectedStartPoint).isEqualTo(pointC)
        assertThat(state.selectedEndPoint).isNull()
        assertThat(state.currentMeasurement).isNull()
    }

    @Test
    fun `onResetMeasurements should clear all anchor points and measurement result`() {
        val pointA = Point3D(x = 0.0f, y = 0.0f, z = 0.0f)
        val pointB = Point3D(x = 1.0f, y = 0.0f, z = 0.0f)
        every { repository.performHitTest(0.5f, 0.5f) } returns pointA andThen pointB

        viewModel.onAnchorPointTapped()
        viewModel.onAnchorPointTapped()
        viewModel.onResetMeasurements()

        val state = viewModel.uiState.value
        assertThat(state.selectedStartPoint).isNull()
        assertThat(state.selectedEndPoint).isNull()
        assertThat(state.currentMeasurement).isNull()
    }

    @Test
    fun `processFrame should update tracking status in UI state correctly`() {
        val frame = mockk<Frame>()
        val camera = mockk<Camera>()
        every { frame.camera } returns camera
        every { camera.trackingState } returns TrackingState.TRACKING

        viewModel.processFrame(frame)

        assertThat(viewModel.uiState.value.trackingStatus).isEqualTo(TrackingStatus.TRACKING)
    }
}