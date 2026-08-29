package org.aetheris.app.domain.model

data class SpatialFrameData(
    val trackingStatus: TrackingStatus = TrackingStatus.UNAVAILABLE,
    val isDepthEnabled: Boolean = false,
    val pointCount: Int = 0,
    val isSurfaceDetected: Boolean = false,
    val anchoredStartPoint: Point3D? = null,
    val anchoredEndPoint: Point3D? = null
)