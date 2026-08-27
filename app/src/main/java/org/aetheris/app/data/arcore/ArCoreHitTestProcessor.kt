package org.aetheris.app.data.arcore

import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.TrackingState
import org.aetheris.app.domain.model.Point3D

/**
 * Processador de baixo nível responsável por projetar raios ópticos (Raycasting)
 * a partir do espaço 2D da tela contra a malha tridimensional e planos rastreados.
 */
class ArCoreHitTestProcessor {

    /**
     * Executa o hit-test no frame ativo convertendo coordenadas normalizadas [0.0, 1.0]
     * em coordenadas de viewport e filtrando trackables válidos.
     *
     * Prioridade de colisão:
     * 1. Superfície de plano delimitada pelo polígono convexo (Plane.isPoseInPolygon).
     * 2. Ponto de profundidade ToF / Depth API com rastreamento ativo.
     */
    fun performRaycast(
        frame: Frame?,
        normalizedX: Float,
        normalizedY: Float,
        viewportWidth: Int,
        viewportHeight: Int
    ): Point3D? {
        if (frame == null || frame.camera.trackingState != TrackingState.TRACKING) {
            return null
        }

        if (viewportWidth <= 0 || viewportHeight <= 0) {
            return null
        }

        // Conversão de coordenadas normalizadas (HUD Compose) para pixels físicos do viewport
        val pixelX = (normalizedX * viewportWidth).coerceIn(0f, viewportWidth.toFloat())
        val pixelY = (normalizedY * viewportHeight).coerceIn(0f, viewportHeight.toFloat())

        val hitResults = frame.hitTest(pixelX, pixelY)

        for (hit in hitResults) {
            val trackable = hit.trackable

            // Prioridade 1: Planos detectados com validação de limites poligonais
            if (trackable is Plane &&
                trackable.trackingState == TrackingState.TRACKING &&
                trackable.isPoseInPolygon(hit.hitPose)
            ) {
                val pose = hit.hitPose
                return Point3D(
                    x = pose.tx(),
                    y = pose.ty(),
                    z = pose.tz()
                )
            }

            // Prioridade 2: Pontos de profundidade ToF (Depth API) rastreados
            if (trackable is Point && trackable.trackingState == TrackingState.TRACKING) {
                val pose = hit.hitPose
                return Point3D(
                    x = pose.tx(),
                    y = pose.ty(),
                    z = pose.tz()
                )
            }
        }

        return null
    }
}