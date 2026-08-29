package org.aetheris.app.data.arcore

import com.google.ar.core.Anchor
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.DeadlineExceededException
import com.google.ar.core.exceptions.NotTrackingException
import com.google.ar.core.exceptions.ResourceExhaustedException
import com.google.ar.core.exceptions.SessionPausedException
import org.aetheris.app.domain.model.Point3D

class ArCoreHitTestProcessor {

    /**
     * Verifica se existe superfície física válida na coordenada em pixels da tela.
     */
    fun hasValidSurfaceAt(
        frame: Frame,
        xPx: Float,
        yPx: Float
    ): Boolean {
        return findValidHit(frame, xPx, yPx) != null
    }

    /**
     * Projeta um raio óptico a partir de coordenadas em pixels e retorna
     * a posição 3D no sistema de coordenadas do mundo.
     */
    fun performHitTest(
        frame: Frame,
        xPx: Float,
        yPx: Float
    ): Point3D? {
        val pose = findValidHit(frame, xPx, yPx)?.hitPose ?: return null

        return Point3D(
            x = pose.tx(),
            y = pose.ty(),
            z = pose.tz()
        )
    }

    /**
     * Cria uma âncora SLAM nativa na primeira superfície válida encontrada.
     * Deve ser liberada via anchor.detach() quando não for mais necessária.
     */
    fun createAnchorAt(
        frame: Frame,
        xPx: Float,
        yPx: Float
    ): Anchor? {
        val hit = findValidHit(frame, xPx, yPx) ?: return null

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
        }
    }

    private fun findValidHit(
        frame: Frame,
        xPx: Float,
        yPx: Float
    ): HitResult? {
        if (!xPx.isFinite() || !yPx.isFinite() || xPx < 0f || yPx < 0f) {
            return null
        }

        if (frame.camera.trackingState != TrackingState.TRACKING) {
            return null
        }

        val hitResults = try {
            frame.hitTest(xPx, yPx)
        } catch (_: Exception) {
            return null
        }

        return hitResults.firstOrNull(::isValidHit)
    }

    private fun isValidHit(hit: HitResult): Boolean {
        val trackable = hit.trackable

        if (trackable.trackingState != TrackingState.TRACKING) {
            return false
        }

        return when (trackable) {
            is Plane -> {
                trackable.isPoseInPolygon(hit.hitPose)
            }

            is Point -> {
                trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
            }

            is DepthPoint -> {
                true
            }

            else -> {
                false
            }
        }
    }
}