package org.aetheris.app.presentation.measurement

import org.aetheris.app.domain.model.DistanceMeasurement
import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.TrackingStatus

data class MeasurementUiState(
    val trackingStatus: TrackingStatus = TrackingStatus.INITIALIZING,
    val isDepthActive: Boolean = false,
    val detectedPointsCount: Int = 0,
    val selectedStartPoint: Point3D? = null,
    val selectedEndPoint: Point3D? = null,
    val currentMeasurement: DistanceMeasurement? = null,
    val isTargetingSurface: Boolean = false
)