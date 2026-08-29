package org.aetheris.app.data.arcore

import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.TrackingState
import org.aetheris.app.domain.model.Point3D

class ArCoreHitTestProcessor {

    fun hasValidSurfaceAt(frame: Frame, normalizedX: Float, normalizedY: Float): Boolean {
        return performHitTest(frame, normalizedX, normalizedY) != null
    }

    fun performHitTest(frame: Frame, normalizedX: Float, normalizedY: Float): Point3D? {
        val hitResults = try {
            frame.hitTest(normalizedX, normalizedY)
        } catch (e: Exception) {
            return null
        }

        for (hit in hitResults) {
            val trackable = hit.trackable
            if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose) && trackable.trackingState == TrackingState.TRACKING) {
                val pose = hit.hitPose
                return Point3D(pose.tx(), pose.ty(), pose.tz())
            }
            if (trackable is Point && trackable.trackingState == TrackingState.TRACKING) {
                val pose = hit.hitPose
                return Point3D(pose.tx(), pose.ty(), pose.tz())
            }
        }
        return null
    }

    fun createAnchorAt(frame: Frame, normalizedX: Float, normalizedY: Float): Anchor? {
        val hitResults = try {
            frame.hitTest(normalizedX, normalizedY)
        } catch (e: Exception) {
            return null
        }

        for (hit in hitResults) {
            val trackable = hit.trackable
            if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose) && trackable.trackingState == TrackingState.TRACKING) {
                return hit.createAnchor()
            }
            if (trackable is Point && trackable.trackingState == TrackingState.TRACKING) {
                return hit.createAnchor()
            }
        }
        return null
    }
}
