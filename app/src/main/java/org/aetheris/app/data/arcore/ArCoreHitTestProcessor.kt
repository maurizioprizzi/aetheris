package org.aetheris.app.data.arcore

import com.google.ar.core.Anchor
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.DeadlineExceededException
import com.google.ar.core.exceptions.NotTrackingException
import com.google.ar.core.exceptions.ResourceExhaustedException
import com.google.ar.core.exceptions.SessionPausedException
import org.aetheris.app.domain.model.Point3D

/**
 * Processa hit tests realizados contra superfícies
 * reconhecidas pelo ARCore.
 */
class ArCoreHitTestProcessor {

    /**
     * Verifica se existe uma superfície física válida
     * na coordenada em pixels da tela.
     */
    fun hasValidSurfaceAt(
        frame: Frame,
        xPx: Float,
        yPx: Float
    ): Boolean {
        return findValidHit(
            frame = frame,
            xPx = xPx,
            yPx = yPx
        ) != null
    }

    /**
     * Projeta um raio a partir das coordenadas da tela
     * e retorna sua posição no espaço mundial.
     */
    fun performHitTest(
        frame: Frame,
        xPx: Float,
        yPx: Float
    ): Point3D? {
        val hit = findValidHit(
            frame = frame,
            xPx = xPx,
            yPx = yPx
        ) ?: return null

        return try {
            hit.hitPose.toPoint3D()
        } catch (_: RuntimeException) {
            null
        }
    }

    /**
     * Cria uma âncora nativa na primeira superfície
     * válida encontrada.
     *
     * A âncora deve ser posteriormente liberada
     * por meio de Anchor.detach().
     */
    fun createAnchorAt(
        frame: Frame,
        xPx: Float,
        yPx: Float
    ): Anchor? {
        val hit = findValidHit(
            frame = frame,
            xPx = xPx,
            yPx = yPx
        ) ?: return null

        return try {
            hit.createAnchor()
        } catch (_: NotTrackingException) {
            null
        } catch (_: SessionPausedException) {
            null
        } catch (_: DeadlineExceededException) {
            null
        } catch (_: ResourceExhaustedException) {
            null
        } catch (_: RuntimeException) {
            /*
             * Protege a aplicação contra falhas inesperadas
             * provenientes do pipeline nativo do ARCore.
             */
            null
        }
    }

    private fun findValidHit(
        frame: Frame,
        xPx: Float,
        yPx: Float
    ): HitResult? {
        if (!areValidScreenCoordinates(xPx, yPx)) {
            return null
        }

        return try {
            if (
                frame.camera.trackingState !=
                TrackingState.TRACKING
            ) {
                return null
            }

            frame
                .hitTest(xPx, yPx)
                .firstOrNull { hit ->
                    isValidHit(hit)
                }
        } catch (_: RuntimeException) {
            /*
             * Um frame pode se tornar inválido quando a sessão
             * é pausada ou quando a câmera fica indisponível.
             */
            null
        }
    }

    private fun isValidHit(
        hit: HitResult
    ): Boolean {
        val trackable = hit.trackable

        if (
            trackable.trackingState !=
            TrackingState.TRACKING
        ) {
            return false
        }

        return when (trackable) {
            is Plane -> {
                trackable.isPoseInPolygon(
                    hit.hitPose
                )
            }

            is Point -> {
                trackable.orientationMode ==
                        Point.OrientationMode
                            .ESTIMATED_SURFACE_NORMAL
            }

            is DepthPoint -> {
                true
            }

            else -> {
                false
            }
        }
    }

    private fun areValidScreenCoordinates(
        xPx: Float,
        yPx: Float
    ): Boolean {
        return xPx.isFinite() &&
                yPx.isFinite() &&
                xPx >= 0f &&
                yPx >= 0f
    }

    private fun Pose.toPoint3D(): Point3D {
        return Point3D(
            x = tx(),
            y = ty(),
            z = tz()
        )
    }
}