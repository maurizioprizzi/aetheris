package org.aetheris.app.presentation.measurement

import org.aetheris.app.domain.model.AnchorSlot
import org.aetheris.app.domain.model.DimensionAxis
import org.aetheris.app.domain.model.DistanceMeasurement
import org.aetheris.app.domain.model.MassEstimate
import org.aetheris.app.domain.model.MaterialDensity
import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.ScreenPoint2D
import org.aetheris.app.domain.model.SpatialDimensions
import org.aetheris.app.domain.model.TrackingStatus
import org.aetheris.app.domain.model.VolumeMeasurement

/**
 * Estado visual da tela de medição espacial.
 *
 * Uma dimensão é capturada por vez utilizando dois pontos:
 *
 * 1. Largura;
 * 2. Altura;
 * 3. Profundidade.
 *
 * Depois que os três eixos são concluídos, o aplicativo
 * calcula o volume aproximado da caixa delimitadora.
 *
 * Quando um material é selecionado, o volume e sua densidade
 * podem ser utilizados para produzir uma estimativa de massa.
 */
data class MeasurementUiState(
    val trackingStatus: TrackingStatus =
        TrackingStatus.UNAVAILABLE,

    val isDepthEnabled: Boolean = false,

    val detectedPointsCount: Int = 0,

    /**
     * Indica que a mira está sobre uma superfície real
     * reconhecida pelo hit test convencional do ARCore.
     *
     * Este valor não inclui o Instant Placement.
     */
    val isTargetingSurface: Boolean = false,

    val isAnchorPlacementInProgress: Boolean = false,

    /**
     * Primeiro ponto da dimensão que está sendo medida.
     */
    val selectedStartPoint: Point3D? = null,

    /**
     * Segundo ponto da dimensão que está sendo medida.
     */
    val selectedEndPoint: Point3D? = null,

    /**
     * Distância entre os dois pontos da dimensão atual.
     *
     * Depois da confirmação, essa medição será armazenada
     * em [spatialDimensions].
     */
    val currentMeasurement: DistanceMeasurement? = null,

    /**
     * Largura, altura e profundidade já confirmadas.
     */
    val spatialDimensions: SpatialDimensions =
        SpatialDimensions.EMPTY,

    /**
     * Volume calculado depois da confirmação dos três eixos.
     */
    val volumeMeasurement: VolumeMeasurement? = null,

    /**
     * Densidade do material selecionado para estimar a massa
     * do objeto medido.
     */
    val selectedMaterialDensity: MaterialDensity? = null,

    /**
     * Massa estimada a partir do volume e da densidade.
     *
     * O resultado representa uma aproximação física e não
     * uma pesagem direta ou uma medição de balança.
     */
    val massEstimate: MassEstimate? = null,

    /**
     * Posição projetada do indicador da dimensão atual.
     */
    val badgePosition: ScreenPoint2D? = null,

    val viewportWidthPx: Int = 0,

    val viewportHeightPx: Int = 0
) {
    init {
        require(detectedPointsCount >= 0) {
            "A quantidade de pontos não pode ser negativa."
        }

        require(viewportWidthPx >= 0) {
            "A largura da viewport não pode ser negativa."
        }

        require(viewportHeightPx >= 0) {
            "A altura da viewport não pode ser negativa."
        }
    }

    val isTracking: Boolean
        get() = trackingStatus == TrackingStatus.TRACKING

    val hasValidViewport: Boolean
        get() = viewportWidthPx > 0 &&
                viewportHeightPx > 0

    val hasStartPoint: Boolean
        get() = selectedStartPoint != null

    val hasEndPoint: Boolean
        get() = selectedEndPoint != null

    /**
     * Quantidade de âncoras da dimensão atualmente ativa.
     */
    val anchorCount: Int
        get() = (if (hasStartPoint) 1 else 0) +
                (if (hasEndPoint) 1 else 0)

    /**
     * Indica que os dois pontos da dimensão atual
     * foram posicionados e sua distância foi calculada.
     */
    val hasCompleteMeasurement: Boolean
        get() = hasStartPoint &&
                hasEndPoint &&
                currentMeasurement != null

    /**
     * Próximo slot de âncora necessário para concluir
     * a dimensão atualmente ativa.
     */
    val nextAnchorSlot: AnchorSlot?
        get() = when {
            !hasStartPoint -> AnchorSlot.START
            !hasEndPoint -> AnchorSlot.END
            else -> null
        }

    /**
     * Eixo que deve ser medido atualmente.
     *
     * A sequência padrão é:
     *
     * WIDTH -> HEIGHT -> DEPTH
     *
     * Retorna null quando os três eixos já foram medidos.
     */
    val currentDimensionAxis: DimensionAxis?
        get() = spatialDimensions.nextPendingAxis

    /**
     * Quantidade de eixos já confirmados.
     */
    val measuredDimensionCount: Int
        get() = spatialDimensions.measuredAxisCount

    /**
     * Indica que ainda existe uma dimensão pendente.
     */
    val hasPendingDimension: Boolean
        get() = currentDimensionAxis != null

    /**
     * Permite confirmar a distância atual como medição
     * do eixo que está ativo.
     */
    val canConfirmCurrentDimension: Boolean
        get() = hasCompleteMeasurement &&
                currentDimensionAxis != null

    /**
     * Indica que largura, altura e profundidade
     * já foram confirmadas.
     */
    val hasCompleteSpatialDimensions: Boolean
        get() = spatialDimensions.isComplete

    /**
     * Indica que as dimensões estão completas, mas o
     * volume ainda precisa ser calculado.
     */
    val isReadyToCalculateVolume: Boolean
        get() = hasCompleteSpatialDimensions &&
                volumeMeasurement == null

    /**
     * Indica que as dimensões e o volume foram concluídos.
     */
    val hasCompleteSpatialMeasurement: Boolean
        get() = hasCompleteSpatialDimensions &&
                volumeMeasurement != null

    /**
     * Indica que um material foi selecionado.
     */
    val hasSelectedMaterialDensity: Boolean
        get() = selectedMaterialDensity != null

    /**
     * Indica que existem volume e densidade disponíveis,
     * mas a estimativa de massa ainda não foi calculada.
     */
    val isReadyToCalculateMass: Boolean
        get() = volumeMeasurement != null &&
                selectedMaterialDensity != null &&
                massEstimate == null

    /**
     * Indica que a estimativa física completa do objeto
     * possui dimensões, volume, material e massa.
     */
    val hasCompleteObjectEstimate: Boolean
        get() = hasCompleteSpatialMeasurement &&
                selectedMaterialDensity != null &&
                massEstimate != null

    /**
     * Indica que o próximo ponto poderá usar uma superfície
     * confirmada pelo ARCore.
     */
    val hasConfirmedPlacementSurface: Boolean
        get() = isTracking &&
                isTargetingSurface

    /**
     * Indica que não existe superfície convencional sob a
     * mira e que o próximo clique poderá precisar utilizar
     * o Instant Placement como fallback aproximado.
     */
    val requiresApproximatePlacement: Boolean
        get() = isTracking &&
                !isTargetingSurface

    /**
     * A criação de âncoras depende do rastreamento da câmera,
     * mas não exige mais que um plano convencional esteja
     * previamente detectado.
     *
     * Quando não houver superfície real, o processador poderá
     * tentar o Instant Placement somente durante o clique.
     */
    val canPlaceAnchor: Boolean
        get() = isTracking &&
                !isAnchorPlacementInProgress &&
                currentDimensionAxis != null &&
                nextAnchorSlot != null

    /**
     * O reset também fica disponível depois que uma ou mais
     * dimensões foram confirmadas ou quando existem resultados
     * físicos associados à medição.
     */
    val canResetMeasurement: Boolean
        get() = hasStartPoint ||
                hasEndPoint ||
                currentMeasurement != null ||
                !spatialDimensions.isEmpty ||
                volumeMeasurement != null ||
                selectedMaterialDensity != null ||
                massEstimate != null

    val shouldShowBadge: Boolean
        get() = currentMeasurement != null &&
                badgePosition?.isVisible == true
}