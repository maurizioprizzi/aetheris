package org.aetheris.app.domain.model

data class SpatialFrameData(
    val cameraPose: Point3D = Point3D.ORIGIN,
    val pointCloud: List<Point3D> = emptyList(),
    val trackingStatus: TrackingStatus = TrackingStatus.INITIALIZING,
    val isDepthAvailable: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
)