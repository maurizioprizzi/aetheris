package org.aetheris.app.presentation.measurement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.aetheris.app.domain.model.Point3D
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
}