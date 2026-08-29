package org.aetheris.app.presentation.measurement

import org.aetheris.app.domain.model.AnchorSlot
import org.aetheris.app.domain.model.DistanceMeasurement
import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.ScreenPoint2D
import org.aetheris.app.domain.model.TrackingStatus

/**
 * Estado visual imutável da tela de medição espacial.
 */
data class MeasurementUiState(
    val trackingStatus: TrackingStatus =
        TrackingStatus.UNAVAILABLE,

    val isDepthEnabled: Boolean = false,

    val detectedPointsCount: Int = 0,

    val isTargetingSurface: Boolean = false,

    val isAnchorPlacementInProgress: Boolean = false,

    val selectedStartPoint: Point3D? = null,

    val selectedEndPoint: Point3D? = null,

    val currentMeasurement: DistanceMeasurement? = null,

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

    /**
     * Indica que o ARCore está rastreando
     * normalmente o ambiente.
     */
    val isTracking: Boolean
        get() = trackingStatus.allowsAnchorPlacement

    /**
     * Indica que a superfície gráfica possui
     * dimensões válidas para projeção.
     */
    val hasValidViewport: Boolean
        get() = viewportWidthPx > 0 &&
                viewportHeightPx > 0

    val hasStartPoint: Boolean
        get() = selectedStartPoint != null

    val hasEndPoint: Boolean
        get() = selectedEndPoint != null

    val anchorCount: Int
        get() {
            var count = 0

            if (hasStartPoint) {
                count++
            }

            if (hasEndPoint) {
                count++
            }

            return count
        }

    /**
     * Indica que os dois pontos e a distância
     * calculada estão disponíveis.
     */
    val hasCompleteMeasurement: Boolean
        get() = hasStartPoint &&
                hasEndPoint &&
                currentMeasurement != null

    /**
     * Define qual âncora deve ser posicionada
     * na próxima interação.
     */
    val nextAnchorSlot: AnchorSlot?
        get() = when {
            !hasStartPoint -> AnchorSlot.START
            !hasEndPoint -> AnchorSlot.END
            else -> null
        }

    /**
     * Indica que todas as condições necessárias
     * para posicionar uma âncora foram atendidas.
     */
    val canPlaceAnchor: Boolean
        get() = isTracking &&
                isTargetingSurface &&
                !isAnchorPlacementInProgress &&
                nextAnchorSlot != null

    val canResetMeasurement: Boolean
        get() = hasStartPoint ||
                hasEndPoint ||
                currentMeasurement != null

    /**
     * O indicador de distância só deve aparecer
     * quando sua posição projetada estiver visível.
     */
    val shouldShowBadge: Boolean
        get() = currentMeasurement != null &&
                badgePosition?.isVisible == true
}