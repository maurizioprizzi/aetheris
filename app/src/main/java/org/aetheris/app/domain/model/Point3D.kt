package org.aetheris.app.domain.model

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Ponto imutável no espaço cartesiano tridimensional (x = horizontal, y = vertical, z = profundidade).
 */
data class Point3D(
    val x: Float,
    val y: Float,
    val z: Float
) {
    fun distanceTo(other: Point3D): Float {
        val dx = (this.x - other.x).toDouble()
        val dy = (this.y - other.y).toDouble()
        val dz = (this.z - other.z).toDouble()
        return sqrt(dx.pow(2.0) + dy.pow(2.0) + dz.pow(2.0)).toFloat()
    }

    operator fun plus(other: Point3D) = Point3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Point3D) = Point3D(x - other.x, y - other.y, z - other.z)

    companion object {
        val ORIGIN = Point3D(0f, 0f, 0f)
    }
}