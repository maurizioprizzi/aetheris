package org.aetheris.app.presentation.measurement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.Frame
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.aetheris.app.data.repository.SpatialSensorRepositoryImpl
import org.aetheris.app.domain.repository.SpatialSensorRepository
import org.aetheris.app.domain.usecase.CalculateDistanceUseCase
import org.aetheris.app.domain.usecase.ProjectWorldToScreenUseCase

class MeasurementViewModel(
    private val spatialSensorRepository: SpatialSensorRepository,
    private val calculateDistanceUseCase: CalculateDistanceUseCase,
    private val projectWorldToScreenUseCase:
    ProjectWorldToScreenUseCase
) : ViewModel() {

    private val concreteSpatialRepository =
        spatialSensorRepository as? SpatialSensorRepositoryImpl

    private val _uiState =
        MutableStateFlow(MeasurementUiState())

    val uiState: StateFlow<MeasurementUiState> =
        _uiState.asStateFlow()

    private var anchorPlacementJob: Job? = null

    init {
        observeSpatialData()
    }

    /**
     * Atualiza as dimensões da viewport quando
     * a superfície gráfica é redimensionada.
     */
    fun onSurfaceDimensionsChanged(
        widthPx: Int,
        heightPx: Int
    ) {
        if (widthPx <= 0 || heightPx <= 0) {
            return
        }

        /*
         * O repositório utiliza essas dimensões para converter
         * coordenadas normalizadas em pixels da tela.
         */
        concreteSpatialRepository?.updateViewportSize(
            widthPx = widthPx,
            heightPx = heightPx
        )

        _uiState.update { current ->
            if (
                current.viewportWidthPx == widthPx &&
                current.viewportHeightPx == heightPx
            ) {
                current
            } else {
                current.copy(
                    viewportWidthPx = widthPx,
                    viewportHeightPx = heightPx
                )
            }
        }
    }

    /**
     * Atualiza a posição 2D do indicador métrico
     * usando as matrizes mais recentes da câmera.
     *
     * É chamado na GL thread com cópias defensivas
     * das matrizes View e Projection.
     */
    fun onCameraMatricesUpdated(
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray
    ) {
        val state = _uiState.value
        val startPoint = state.selectedStartPoint
        val endPoint = state.selectedEndPoint

        val projectedBadge =
            if (
                startPoint != null &&
                endPoint != null &&
                state.hasValidViewport
            ) {
                projectWorldToScreenUseCase.projectMidpoint(
                    pointA = startPoint,
                    pointB = endPoint,
                    viewMatrix = viewMatrix,
                    projectionMatrix = projectionMatrix,
                    viewportWidth =
                        state.viewportWidthPx,
                    viewportHeight =
                        state.viewportHeightPx
                )
            } else {
                null
            }

        _uiState.update { current ->
            if (current.badgePosition == projectedBadge) {
                current
            } else {
                current.copy(
                    badgePosition = projectedBadge
                )
            }
        }
    }

    /**
     * Processa o frame atual do ARCore na GL thread.
     *
     * O frame é consumido sincronamente e não é
     * armazenado na ViewModel.
     */
    fun processFrame(frame: Frame) {
        concreteSpatialRepository?.onFrameUpdate(frame)
    }

    /**
     * Solicita a criação de uma âncora no centro da mira.
     */
    fun onAnchorPointTapped() {
        val state = _uiState.value

        val anchorSlot = state.nextAnchorSlot
            ?: return

        if (!state.canPlaceAnchor) {
            return
        }

        if (anchorPlacementJob?.isActive == true) {
            return
        }

        _uiState.update { current ->
            current.copy(
                isAnchorPlacementInProgress = true
            )
        }

        anchorPlacementJob = viewModelScope.launch {
            try {
                spatialSensorRepository.createAnchor(
                    normalizedX =
                        CENTER_NORMALIZED_COORDINATE,
                    normalizedY =
                        CENTER_NORMALIZED_COORDINATE,
                    slot = anchorSlot
                )
            } finally {
                _uiState.update { current ->
                    current.copy(
                        isAnchorPlacementInProgress = false
                    )
                }

                anchorPlacementJob = null
            }
        }
    }

    /**
     * Remove as âncoras e limpa a medição atual.
     */
    fun onResetMeasurements() {
        anchorPlacementJob?.cancel()
        anchorPlacementJob = null

        spatialSensorRepository.clearAnchors()

        _uiState.update { current ->
            current.copy(
                selectedStartPoint = null,
                selectedEndPoint = null,
                currentMeasurement = null,
                badgePosition = null,
                isAnchorPlacementInProgress = false
            )
        }
    }

    private fun observeSpatialData() {
        viewModelScope.launch {
            spatialSensorRepository
                .spatialDataStream
                .collect { spatialData ->
                    _uiState.update { current ->
                        val startPoint =
                            spatialData.anchoredStartPoint

                        val endPoint =
                            spatialData.anchoredEndPoint

                        val anchorsChanged =
                            startPoint !=
                                    current.selectedStartPoint ||
                                    endPoint !=
                                    current.selectedEndPoint

                        val measurement = when {
                            startPoint == null ||
                                    endPoint == null -> {
                                null
                            }

                            anchorsChanged ||
                                    current.currentMeasurement ==
                                    null -> {
                                calculateDistanceUseCase(
                                    start = startPoint,
                                    end = endPoint
                                )
                            }

                            else -> {
                                current.currentMeasurement
                            }
                        }

                        current.copy(
                            trackingStatus =
                                spatialData.trackingStatus,
                            isDepthEnabled =
                                spatialData.isDepthEnabled,
                            detectedPointsCount =
                                spatialData.pointCount,
                            isTargetingSurface =
                                spatialData.isSurfaceDetected,
                            selectedStartPoint =
                                startPoint,
                            selectedEndPoint =
                                endPoint,
                            currentMeasurement =
                                measurement,
                            badgePosition =
                                if (measurement == null) {
                                    null
                                } else {
                                    current.badgePosition
                                }
                        )
                    }
                }
        }
    }

    private companion object {
        const val CENTER_NORMALIZED_COORDINATE = 0.5f
    }
}