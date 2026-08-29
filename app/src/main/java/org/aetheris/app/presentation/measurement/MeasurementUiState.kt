package org.aetheris.app.presentation.measurement

import org.aetheris.app.domain.model.DistanceMeasurement
import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.ScreenPoint2D
import org.aetheris.app.domain.model.TrackingStatus

data class MeasurementUiState(
    val trackingStatus: TrackingStatus = TrackingStatus.UNAVAILABLE,
    val isDepthActive: Boolean = false,
    val detectedPointsCount: Int = 0,
    val isTargetingSurface: Boolean = false,
    val selectedStartPoint: Point3D? = null,
    val selectedEndPoint: Point3D? = null,
    val currentMeasurement: DistanceMeasurement? = null,
    val badgePosition: ScreenPoint2D? = null,
    val viewportWidth: Int = 1080,
    val viewportHeight: Int = 2400
)