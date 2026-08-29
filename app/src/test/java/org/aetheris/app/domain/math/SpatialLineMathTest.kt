package org.aetheris.app.domain.math

import org.aetheris.app.domain.model.Point3D
import kotlin.math.sqrt

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
            x = (start.x + end.x) * 0.5f,
            y = (start.y + end.y) * 0.5f,
            z = (start.z + end.z) * 0.5f
        )
    }

    fun magnitude(
        start: Point3D,
        end: Point3D
    ): Float {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val dz = end.z - start.z

        return sqrt(
            dx * dx +
                    dy * dy +
                    dz * dz
        )
    }

    fun normalizedDirection(
        start: Point3D,
        end: Point3D
    ): Point3D? {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val dz = end.z - start.z

        val magnitude = sqrt(
            dx * dx +
                    dy * dy +
                    dz * dz
        )

        if (magnitude <= MINIMUM_MAGNITUDE) {
            return null
        }

        return Point3D(
            x = dx / magnitude,
            y = dy / magnitude,
            z = dz / magnitude
        )
    }

    private const val MINIMUM_MAGNITUDE = 1e-6f
}