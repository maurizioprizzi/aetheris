package org.aetheris.app.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.SpatialFrameData
import org.aetheris.app.domain.model.TrackingStatus
import org.aetheris.app.domain.repository.SpatialSensorRepository

class SpatialSensorRepositoryImpl : SpatialSensorRepository {

    private val _spatialFrameStream = MutableStateFlow(SpatialFrameData())
    override val spatialFrameStream: StateFlow<SpatialFrameData> = _spatialFrameStream.asStateFlow()

    override fun updateFrame(
        cameraPosition: Point3D,
        points: List<Point3D>,
        status: TrackingStatus,
        hasDepth: Boolean
    ) {
        _spatialFrameStream.update {
            SpatialFrameData(
                cameraPose = cameraPosition,
                pointCloud = points,
                trackingStatus = status,
                isDepthAvailable = hasDepth,
                timestampMillis = System.currentTimeMillis()
            )
        }
    }

    override fun performHitTest(normalizedX: Float, normalizedY: Float): Point3D? {
        val currentPoints = _spatialFrameStream.value.pointCloud
        if (currentPoints.isEmpty()) return null

        // Algoritmo de projeção vetorial simples para cálculo de interseção mais próxima
        return currentPoints.minByOrNull { point ->
            val screenDistance = kotlin.math.sqrt(
                ((point.x - normalizedX) * (point.x - normalizedX) +
                        (point.y - normalizedY) * (point.y - normalizedY)).toDouble()
            )
            screenDistance
        }
    }

    override fun resetTracking() {
        _spatialFrameStream.value = SpatialFrameData()
    }
}