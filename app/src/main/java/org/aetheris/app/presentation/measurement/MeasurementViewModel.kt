package org.aetheris.app.presentation.measurement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.aetheris.app.data.repository.SpatialSensorRepositoryImpl
import org.aetheris.app.domain.model.TrackingStatus
import org.aetheris.app.domain.repository.SpatialSensorRepository
import org.aetheris.app.domain.usecase.CalculateDistanceUseCase

class MeasurementViewModel(
    private val sensorRepository: SpatialSensorRepository,
    private val calculateDistanceUseCase: CalculateDistanceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeasurementUiState())
    val uiState: StateFlow<MeasurementUiState> = _uiState.asStateFlow()

    init {
        observeSensorTelemetry()
    }

    private fun observeSensorTelemetry() {
        sensorRepository.spatialFrameStream
            .onEach { frameData ->
                _uiState.update { current ->
                    current.copy(
                        trackingStatus = frameData.trackingStatus,
                        isDepthActive = frameData.isDepthAvailable,
                        detectedPointsCount = frameData.pointCloud.size,
                        isTargetingSurface = frameData.pointCloud.isNotEmpty()
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Propaga a resolução do viewport EGL/GLSurfaceView para o cálculo de raycast.
     */
    fun onSurfaceDimensionsChanged(width: Int, height: Int) {
        (sensorRepository as? SpatialSensorRepositoryImpl)?.updateViewportDimensions(width, height)
    }

    fun onAnchorPointTapped(normalizedScreenX: Float = 0.5f, normalizedScreenY: Float = 0.5f) {
        val hitPoint = sensorRepository.performHitTest(normalizedScreenX, normalizedScreenY) ?: return

        _uiState.update { current ->
            when {
                current.selectedStartPoint == null -> {
                    current.copy(
                        selectedStartPoint = hitPoint,
                        selectedEndPoint = null,
                        currentMeasurement = null
                    )
                }
                current.selectedEndPoint == null -> {
                    val measurement = calculateDistanceUseCase(
                        start = current.selectedStartPoint,
                        end = hitPoint
                    )
                    current.copy(
                        selectedEndPoint = hitPoint,
                        currentMeasurement = measurement
                    )
                }
                else -> {
                    current.copy(
                        selectedStartPoint = hitPoint,
                        selectedEndPoint = null,
                        currentMeasurement = null
                    )
                }
            }
        }
    }

    fun onResetMeasurements() {
        _uiState.update { current ->
            current.copy(
                selectedStartPoint = null,
                selectedEndPoint = null,
                currentMeasurement = null
            )
        }
    }

    fun processFrame(frame: Frame) {
        val status = when (frame.camera.trackingState) {
            TrackingState.TRACKING -> TrackingStatus.TRACKING
            TrackingState.PAUSED -> TrackingStatus.PAUSED
            else -> TrackingStatus.INITIALIZING
        }

        _uiState.update { current ->
            current.copy(trackingStatus = status)
        }
    }
}