package org.aetheris.app.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.SpatialFrameData

/**
 * Contrato de abstração de hardware para telemetria de sensores espaciais e AR.
 */
interface SpatialSensorRepository {
    /**
     * Fluxo contínuo e reativo do estado espacial do dispositivo em tempo real.
     */
    val spatialFrameStream: StateFlow<SpatialFrameData>

    /**
     * Projeta um raio (raycasting) a partir de coordenadas normalizadas de tela (0.0 a 1.0)
     * e retorna a coordenada cartesiana 3D no espaço físico, se houver superfície detectada.
     */
    fun performHitTest(normalizedX: Float, normalizedY: Float): Point3D?

    /**
     * Notifica o processador sobre novos dados de frame e nuvem de pontos do ARCore.
     */
    fun updateFrame(
        cameraPosition: Point3D,
        points: List<Point3D>,
        status: org.aetheris.app.domain.model.TrackingStatus,
        hasDepth: Boolean
    )

    /**
     * Reinicia âncoras e buffers espaciais.
     */
    fun resetTracking()
}