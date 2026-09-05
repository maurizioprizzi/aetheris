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
import org.aetheris.app.domain.model.DimensionMeasurement
import org.aetheris.app.domain.model.MaterialDensity
import org.aetheris.app.domain.model.SpatialDimensions
import org.aetheris.app.domain.repository.SpatialSensorRepository
import org.aetheris.app.domain.usecase.CalculateDistanceUseCase
import org.aetheris.app.domain.usecase.CalculateMassUseCase
import org.aetheris.app.domain.usecase.CalculateVolumeUseCase
import org.aetheris.app.domain.usecase.ProjectWorldToScreenUseCase

class MeasurementViewModel(
    private val spatialSensorRepository: SpatialSensorRepository,
    private val calculateDistanceUseCase: CalculateDistanceUseCase,
    private val calculateVolumeUseCase: CalculateVolumeUseCase,
    private val calculateMassUseCase: CalculateMassUseCase,
    private val projectWorldToScreenUseCase: ProjectWorldToScreenUseCase
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(MeasurementUiState())

    val uiState: StateFlow<MeasurementUiState> =
        _uiState.asStateFlow()

    private var anchorPlacementJob: Job? = null

    init {
        observeSpatialData()
    }

    /**
     * Atualiza as dimensões da viewport da interface e do
     * repositório responsável pelos hit tests do ARCore.
     */
    fun onSurfaceDimensionsChanged(
        widthPx: Int,
        heightPx: Int
    ) {
        if (widthPx <= 0 || heightPx <= 0) {
            return
        }

        /*
         * O repositório precisa conhecer o tamanho real da superfície
         * para converter coordenadas normalizadas em pixels.
         *
         * Sem esta atualização, normalizedToPixels() retorna null,
         * isSurfaceDetected permanece false e a criação de âncoras
         * não pode ser processada corretamente.
         */
        (spatialSensorRepository as? SpatialSensorRepositoryImpl)
            ?.updateViewportSize(
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
     * Atualiza a posição do indicador da dimensão atual
     * utilizando as matrizes mais recentes da câmera.
     */
    fun onCameraMatricesUpdated(
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray
    ) {
        val state = _uiState.value

        val startPoint =
            state.selectedStartPoint

        val endPoint =
            state.selectedEndPoint

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
                    viewportWidth = state.viewportWidthPx,
                    viewportHeight = state.viewportHeightPx
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
     * Entrega o frame mais recente ao repositório ARCore.
     */
    fun processFrame(frame: Frame) {
        (spatialSensorRepository as? SpatialSensorRepositoryImpl)
            ?.onFrameUpdate(frame)
    }

    /**
     * Cria o próximo ponto da dimensão atualmente ativa.
     */
    fun onAnchorPointTapped() {
        val state = _uiState.value

        val anchorSlot =
            state.nextAnchorSlot ?: return

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

        anchorPlacementJob =
            viewModelScope.launch {
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
                }
            }
    }

    /**
     * Confirma a distância atual como o valor do eixo ativo,
     * preservando as origens espaciais dos dois pontos.
     *
     * Quando os três eixos são concluídos, o volume é
     * calculado automaticamente. Caso um material esteja
     * selecionado, a massa também é calculada.
     */
    fun onConfirmCurrentDimension() {
        val state = _uiState.value

        val currentAxis =
            state.currentDimensionAxis ?: return

        val currentMeasurement =
            state.currentMeasurement ?: return

        if (!state.canConfirmCurrentDimension) {
            return
        }

        val confirmedDimension =
            DimensionMeasurement(
                measurement = currentMeasurement,
                startSource = state.selectedStartSource,
                endSource = state.selectedEndSource
            )

        val updatedDimensions =
            state.spatialDimensions
                .withDimensionMeasurement(
                    axis = currentAxis,
                    dimensionMeasurement =
                        confirmedDimension
                )

        val calculatedVolume =
            if (updatedDimensions.isComplete) {
                calculateVolumeUseCase(
                    dimensions = updatedDimensions
                )
            } else {
                null
            }

        val calculatedMass =
            if (
                calculatedVolume != null &&
                state.selectedMaterialDensity != null
            ) {
                calculateMassUseCase(
                    volume = calculatedVolume,
                    materialDensity =
                        state.selectedMaterialDensity
                )
            } else {
                null
            }

        spatialSensorRepository.clearAnchors()

        _uiState.update { current ->
            current.copy(
                selectedStartPoint = null,
                selectedStartSource = null,
                selectedEndPoint = null,
                selectedEndSource = null,
                currentMeasurement = null,
                spatialDimensions = updatedDimensions,
                volumeMeasurement = calculatedVolume,
                massEstimate = calculatedMass,
                badgePosition = null,
                isAnchorPlacementInProgress = false
            )
        }
    }

    /**
     * Seleciona o material usado na estimativa de massa.
     */
    fun onMaterialDensitySelected(
        materialDensity: MaterialDensity
    ) {
        _uiState.update { current ->
            val calculatedMass =
                current.volumeMeasurement?.let { volume ->
                    calculateMassUseCase(
                        volume = volume,
                        materialDensity = materialDensity
                    )
                }

            current.copy(
                selectedMaterialDensity = materialDensity,
                massEstimate = calculatedMass
            )
        }
    }

    /**
     * Remove o material selecionado e a estimativa de massa.
     */
    fun onClearSelectedMaterial() {
        _uiState.update { current ->
            current.copy(
                selectedMaterialDensity = null,
                massEstimate = null
            )
        }
    }

    /**
     * Descarta somente os pontos da dimensão atualmente ativa.
     */
    fun onClearCurrentDimension() {
        anchorPlacementJob?.cancel()
        anchorPlacementJob = null

        spatialSensorRepository.clearAnchors()

        _uiState.update { current ->
            current.copy(
                selectedStartPoint = null,
                selectedStartSource = null,
                selectedEndPoint = null,
                selectedEndSource = null,
                currentMeasurement = null,
                badgePosition = null,
                isAnchorPlacementInProgress = false
            )
        }
    }

    /**
     * Reinicia completamente a medição espacial.
     */
    fun onResetMeasurements() {
        anchorPlacementJob?.cancel()
        anchorPlacementJob = null

        spatialSensorRepository.clearAnchors()

        _uiState.update { current ->
            current.copy(
                selectedStartPoint = null,
                selectedStartSource = null,
                selectedEndPoint = null,
                selectedEndSource = null,
                currentMeasurement = null,
                spatialDimensions = SpatialDimensions.EMPTY,
                volumeMeasurement = null,
                selectedMaterialDensity = null,
                massEstimate = null,
                badgePosition = null,
                isAnchorPlacementInProgress = false
            )
        }
    }

    /**
     * Observa o estado espacial emitido pelo repositório.
     *
     * As posições e suas respectivas origens são transferidas
     * juntas para manter a consistência do estado apresentado.
     */
    private fun observeSpatialData() {
        viewModelScope.launch {
            spatialSensorRepository
                .spatialDataStream
                .collect { spatialData ->
                    _uiState.update { current ->
                        val startPoint =
                            spatialData.anchoredStartPoint

                        val startSource =
                            spatialData.anchoredStartSource

                        val endPoint =
                            spatialData.anchoredEndPoint

                        val endSource =
                            spatialData.anchoredEndSource

                        val anchorPositionsChanged =
                            startPoint !=
                                    current.selectedStartPoint ||
                                    endPoint !=
                                    current.selectedEndPoint

                        val measurement =
                            when {
                                startPoint == null ||
                                        endPoint == null -> {
                                    null
                                }

                                anchorPositionsChanged ||
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
                            selectedStartSource =
                                startSource,
                            selectedEndPoint =
                                endPoint,
                            selectedEndSource =
                                endSource,
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