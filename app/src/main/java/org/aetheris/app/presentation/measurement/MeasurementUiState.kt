package org.aetheris.app.presentation.measurement

import org.aetheris.app.domain.model.AnchorSlot
import org.aetheris.app.domain.model.DistanceMeasurement
import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.ScreenPoint2D
import org.aetheris.app.domain.model.TrackingStatus

/**
 * Estado visual da tela de medição espacial.
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

    val isTracking: Boolean
        get() = trackingStatus == TrackingStatus.TRACKING

    val hasValidViewport: Boolean
        get() = viewportWidthPx > 0 &&
                viewportHeightPx > 0

    val hasStartPoint: Boolean
        get() = selectedStartPoint != null

    val hasEndPoint: Boolean
        get() = selectedEndPoint != null

    val anchorCount: Int
        get() = (if (hasStartPoint) 1 else 0) + (if (hasEndPoint) 1 else 0)

    val hasCompleteMeasurement: Boolean
        get() = hasStartPoint &&
                hasEndPoint &&
                currentMeasurement != null

    val nextAnchorSlot: AnchorSlot?
        get() = when {
            !hasStartPoint -> AnchorSlot.START
            !hasEndPoint -> AnchorSlot.END
            else -> null
        }

    val canPlaceAnchor: Boolean
        get() = isTracking &&
                isTargetingSurface &&
                !isAnchorPlacementInProgress &&
                nextAnchorSlot != null

    val canResetMeasurement: Boolean
        get() = hasStartPoint ||
                hasEndPoint ||
                currentMeasurement != null

    val shouldShowBadge: Boolean
        get() = currentMeasurement != null &&
                badgePosition?.isVisible == true
}