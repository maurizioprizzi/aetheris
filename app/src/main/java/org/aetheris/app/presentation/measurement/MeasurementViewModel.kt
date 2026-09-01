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
     * Atualiza as dimensões da viewport quando a superfície
     * gráfica sofre redimensionamento.
     */
    fun onSurfaceDimensionsChanged(
        widthPx: Int,
        heightPx: Int
    ) {
        if (widthPx <= 0 || heightPx <= 0) {
            return
        }

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
     * Atualiza a posição 2D do badge métrico usando
     * as matrizes mais recentes da câmera.
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
     * Processa o frame ARCore na GL Thread.
     */
    fun processFrame(frame: Frame) {
        (
                spatialSensorRepository
                        as? SpatialSensorRepositoryImpl
                )?.onFrameUpdate(frame)
    }

    /**
     * Cria a próxima âncora necessária para a dimensão
     * atualmente ativa.
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
            }
        }
    }

    /**
     * Confirma a distância atual como medição do eixo ativo.
     *
     * Depois da confirmação:
     *
     * 1. A medição é armazenada em largura, altura ou profundidade;
     * 2. As âncoras atuais são liberadas;
     * 3. O próximo eixo passa a ser selecionado;
     * 4. Ao concluir a profundidade, o volume é calculado;
     * 5. Se um material já estiver selecionado, a massa é estimada.
     */
    fun onConfirmCurrentDimension() {
        val state = _uiState.value

        if (!state.canConfirmCurrentDimension) {
            return
        }

        val currentAxis =
            state.currentDimensionAxis ?: return

        val currentMeasurement =
            state.currentMeasurement ?: return

        val updatedDimensions =
            state.spatialDimensions.withMeasurement(
                axis = currentAxis,
                measurement = currentMeasurement
            )

        val updatedVolume =
            if (updatedDimensions.isComplete) {
                calculateVolumeUseCase(
                    dimensions = updatedDimensions
                )
            } else {
                null
            }

        val updatedMassEstimate =
            if (
                updatedVolume != null &&
                state.selectedMaterialDensity != null
            ) {
                calculateMassUseCase(
                    volume = updatedVolume,
                    materialDensity =
                        state.selectedMaterialDensity
                )
            } else {
                null
            }

        _uiState.update { current ->
            current.copy(
                selectedStartPoint = null,
                selectedEndPoint = null,
                currentMeasurement = null,
                spatialDimensions = updatedDimensions,
                volumeMeasurement = updatedVolume,
                massEstimate = updatedMassEstimate,
                badgePosition = null,
                isAnchorPlacementInProgress = false
            )
        }

        /*
         * As âncoras da dimensão concluída não serão
         * reutilizadas pelo próximo eixo.
         */
        spatialSensorRepository.clearAnchors()
    }

    /**
     * Seleciona a densidade do material utilizado na
     * estimativa de massa.
     *
     * Quando o volume já está disponível, a massa é
     * recalculada imediatamente.
     */
    fun onMaterialDensitySelected(
        materialDensity: MaterialDensity
    ) {
        _uiState.update { current ->
            val updatedMassEstimate =
                current.volumeMeasurement?.let { volume ->
                    calculateMassUseCase(
                        volume = volume,
                        materialDensity = materialDensity
                    )
                }

            current.copy(
                selectedMaterialDensity = materialDensity,
                massEstimate = updatedMassEstimate
            )
        }
    }

    /**
     * Remove o material selecionado e invalida a estimativa
     * de massa associada a ele.
     */
    fun onClearSelectedMaterial() {
        _uiState.update { current ->
            if (
                current.selectedMaterialDensity == null &&
                current.massEstimate == null
            ) {
                current
            } else {
                current.copy(
                    selectedMaterialDensity = null,
                    massEstimate = null
                )
            }
        }
    }

    /**
     * Limpa somente os pontos da dimensão que está
     * sendo capturada, preservando dimensões anteriores.
     */
    fun onClearCurrentDimension() {
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

    /**
     * Reinicia completamente a medição tridimensional
     * e todos os resultados físicos associados.
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
                spatialDimensions = SpatialDimensions.EMPTY,
                volumeMeasurement = null,
                selectedMaterialDensity = null,
                massEstimate = null,
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

                        val measurement =
                            when {
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