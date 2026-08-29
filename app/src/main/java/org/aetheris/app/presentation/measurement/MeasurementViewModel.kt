package org.aetheris.app.presentation.measurement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.Frame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.aetheris.app.domain.model.AnchorSlot
import org.aetheris.app.domain.repository.SpatialSensorRepository
import org.aetheris.app.domain.usecase.CalculateDistanceUseCase
import org.aetheris.app.domain.usecase.ProjectWorldToScreenUseCase

class MeasurementViewModel(
    private val spatialSensorRepository: SpatialSensorRepository,
    private val calculateDistanceUseCase: CalculateDistanceUseCase,
    private val projectWorldToScreenUseCase: ProjectWorldToScreenUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeasurementUiState())
    val uiState: StateFlow<MeasurementUiState> = _uiState.asStateFlow()

    init {
        observeSpatialData()
    }

    private fun observeSpatialData() {
        viewModelScope.launch {
            spatialSensorRepository.getSpatialDataStream().collect { frameData ->
                _uiState.update { current ->
                    val start = frameData.anchoredStartPoint ?: current.selectedStartPoint
                    val end = frameData.anchoredEndPoint ?: current.selectedEndPoint
                    val updatedMeasurement = if (start != null && end != null) {
                        calculateDistanceUseCase(start, end)
                    } else {
                        current.currentMeasurement
                    }

                    current.copy(
                        trackingStatus = frameData.trackingStatus,
                        isDepthActive = frameData.isDepthEnabled,
                        detectedPointsCount = frameData.pointCount,
                        isTargetingSurface = frameData.isSurfaceDetected,
                        selectedStartPoint = start,
                        selectedEndPoint = end,
                        currentMeasurement = updatedMeasurement
                    )
                }
            }
        }
    }

    fun onSurfaceDimensionsChanged(width: Int, height: Int) {
        _uiState.update { it.copy(viewportWidth = width, viewportHeight = height) }
    }

    fun processFrame(frame: Frame) {
        spatialSensorRepository.updateFrameData(frame)
    }

    fun onCameraMatricesUpdated(viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        val start = _uiState.value.selectedStartPoint
        val end = _uiState.value.selectedEndPoint

        if (start != null && end != null) {
            val projectedBadge = projectWorldToScreenUseCase.projectMidpoint(
                pointA = start,
                pointB = end,
                viewMatrix = viewMatrix,
                projectionMatrix = projectionMatrix,
                viewportWidth = _uiState.value.viewportWidth,
                viewportHeight = _uiState.value.viewportHeight
            )
            _uiState.update { it.copy(badgePosition = projectedBadge) }
        } else {
            if (_uiState.value.badgePosition != null) {
                _uiState.update { it.copy(badgePosition = null) }
            }
        }
    }

    fun onAnchorPointTapped() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState.selectedStartPoint == null) {
                spatialSensorRepository.createAnchor(0.5f, 0.5f, AnchorSlot.START)
            } else if (currentState.selectedEndPoint == null) {
                spatialSensorRepository.createAnchor(0.5f, 0.5f, AnchorSlot.END)
            }
        }
    }

    fun onResetMeasurements() {
        spatialSensorRepository.clearAnchors()
        _uiState.update {
            it.copy(
                selectedStartPoint = null,
                selectedEndPoint = null,
                currentMeasurement = null,
                badgePosition = null
            )
        }
    }
}