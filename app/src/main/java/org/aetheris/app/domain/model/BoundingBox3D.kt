package org.aetheris.app.domain.model

import kotlin.math.abs

data class BoundingBox3D(
    val minPoint: Point3D,
    val maxPoint: Point3D
) {
    val widthMeters: Float get() = abs(maxPoint.x - minPoint.x)
    val heightMeters: Float get() = abs(maxPoint.y - minPoint.y)
    val depthMeters: Float get() = abs(maxPoint.z - minPoint.z)

    /**
     * Volume em metros cúbicos (m³)
     */
    val volumeCubicMeters: Float get() = widthMeters * heightMeters * depthMeters

    /**
     * Volume em litros (1 m³ = 1000 L)
     */
    val volumeLiters: Float get() = volumeCubicMeters * 1000f

    fun center(): Point3D = Point3D(
        x = (minPoint.x + maxPoint.x) / 2f,
        y = (minPoint.y + maxPoint.y) / 2f,
        z = (minPoint.z + maxPoint.z) / 2f
    )
}