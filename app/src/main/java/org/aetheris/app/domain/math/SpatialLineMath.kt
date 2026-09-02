package org.aetheris.app.domain.math

import org.aetheris.app.domain.model.Point3D
import kotlin.math.hypot

object SpatialLineMath {

    fun toVertexArray(
        start: Point3D,
        end: Point3D
    ): FloatArray {
        return floatArrayOf(
            start.x,
            start.y,
            start.z,
            end.x,
            end.y,
            end.z
        )
    }

    fun midpoint(
        start: Point3D,
        end: Point3D
    ): Point3D {
        return Point3D(
            x = average(start.x, end.x),
            y = average(start.y, end.y),
            z = average(start.z, end.z)
        )
    }

    fun magnitude(
        start: Point3D,
        end: Point3D
    ): Float {
        return start.distanceTo(end)
    }

    fun normalizedDirection(
        start: Point3D,
        end: Point3D
    ): Point3D? {
        val dx = end.x.toDouble() - start.x.toDouble()
        val dy = end.y.toDouble() - start.y.toDouble()
        val dz = end.z.toDouble() - start.z.toDouble()

        val magnitude = hypot(
            hypot(dx, dy),
            dz
        )

        if (
            !magnitude.isFinite() ||
            magnitude <= MINIMUM_MAGNITUDE
        ) {
            return null
        }

        return Point3D(
            x = (dx / magnitude).toFloat(),
            y = (dy / magnitude).toFloat(),
            z = (dz / magnitude).toFloat()
        )
    }

    private fun average(
        first: Float,
        second: Float
    ): Float {
        return (
                (first.toDouble() + second.toDouble()) *
                        MIDPOINT_FACTOR
                ).toFloat()
    }

    private const val MINIMUM_MAGNITUDE = 1e-6
    private const val MIDPOINT_FACTOR = 0.5
}