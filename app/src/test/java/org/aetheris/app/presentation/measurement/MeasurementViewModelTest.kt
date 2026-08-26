package org.aetheris.app.presentation.measurement

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.SpatialFrameData
import org.aetheris.app.domain.model.TrackingStatus
import org.aetheris.app.domain.repository.SpatialSensorRepository
import org.aetheris.app.domain.usecase.CalculateDistanceUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeSpatialSensorRepository
    private lateinit var calculateDistanceUseCase: CalculateDistanceUseCase
    private lateinit var viewModel: MeasurementViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSpatialSensorRepository()
        calculateDistanceUseCase = CalculateDistanceUseCase()
        viewModel = MeasurementViewModel(fakeRepository, calculateDistanceUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have empty points and null measurement`() {
        val state = viewModel.uiState.value

        assertNull(state.selectedStartPoint)
        assertNull(state.selectedEndPoint)
        assertNull(state.currentMeasurement)
        assertEquals(TrackingStatus.INITIALIZING, state.trackingStatus)
    }

    @Test
    fun `when first anchor point is tapped, should store startPoint`() {
        val expectedPoint = Point3D(x = 1.0f, y = 0.5f, z = -2.0f)
        fakeRepository.hitTestResult = expectedPoint

        viewModel.onAnchorPointTapped()

        val state = viewModel.uiState.value
        assertEquals(expectedPoint, state.selectedStartPoint)
        assertNull(state.selectedEndPoint)
        assertNull(state.currentMeasurement)
    }

    @Test
    fun `when second anchor point is tapped, should calculate deterministic distance`() {
        val pointA = Point3D(x = 0.0f, y = 0.0f, z = 0.0f)
        val pointB = Point3D(x = 3.0f, y = 4.0f, z = 0.0f)

        fakeRepository.hitTestResult = pointA
        viewModel.onAnchorPointTapped()

        fakeRepository.hitTestResult = pointB
        viewModel.onAnchorPointTapped()

        val state = viewModel.uiState.value
        assertEquals(pointA, state.selectedStartPoint)
        assertEquals(pointB, state.selectedEndPoint)
        assertNotNull(state.currentMeasurement)
        assertEquals(5.0, state.currentMeasurement!!.meters.toDouble(), 0.001)
    }

    @Test
    fun `when reset is called, all measurement points should be cleared`() {
        fakeRepository.hitTestResult = Point3D(x = 0f, y = 0f, z = 0f)
        viewModel.onAnchorPointTapped()

        viewModel.onResetMeasurements()

        val state = viewModel.uiState.value
        assertNull(state.selectedStartPoint)
        assertNull(state.selectedEndPoint)
        assertNull(state.currentMeasurement)
    }

    @Test
    fun `when sensor telemetry emits new frame, UI state should update reactively`() = runTest {
        val telemetryFrame = SpatialFrameData(
            trackingStatus = TrackingStatus.TRACKING,
            isDepthAvailable = true,
            pointCloud = listOf(Point3D(x = 0f, y = 0f, z = -1f))
        )

        fakeRepository.emitFrame(telemetryFrame)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TrackingStatus.TRACKING, state.trackingStatus)
        assertEquals(true, state.isDepthActive)
        assertEquals(1, state.detectedPointsCount)
        assertEquals(true, state.isTargetingSurface)
    }
}

/**
 * Dublê de teste determinístico para o repositório de sensores espaciais.
 */
private class FakeSpatialSensorRepository : SpatialSensorRepository {
    var hitTestResult: Point3D? = null
    private val _stream = MutableStateFlow(
        SpatialFrameData(
            trackingStatus = TrackingStatus.INITIALIZING,
            isDepthAvailable = false,
            pointCloud = emptyList()
        )
    )
    override val spatialFrameStream: StateFlow<SpatialFrameData> = _stream.asStateFlow()

    fun emitFrame(frame: SpatialFrameData) {
        _stream.value = frame
    }

    override fun performHitTest(normalizedX: Float, normalizedY: Float): Point3D? {
        return hitTestResult
    }

    override fun updateFrame(
        cameraPosition: Point3D,
        points: List<Point3D>,
        status: TrackingStatus,
        hasDepth: Boolean
    ) {
        _stream.value = SpatialFrameData(
            trackingStatus = status,
            isDepthAvailable = hasDepth,
            pointCloud = points
        )
    }

    override fun resetTracking() {
        _stream.value = SpatialFrameData(
            trackingStatus = TrackingStatus.INITIALIZING,
            isDepthAvailable = false,
            pointCloud = emptyList()
        )
    }
}