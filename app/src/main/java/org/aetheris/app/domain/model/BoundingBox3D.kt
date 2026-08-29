package org.aetheris.app.domain.model

import kotlin.math.max
import kotlin.math.min

/**
 * Caixa delimitadora alinhada aos eixos (AABB)
 * do sistema de coordenadas AR.
 */
data class BoundingBox3D(
    val minPoint: Point3D,
    val maxPoint: Point3D
) {
    init {
        require(minPoint.hasFiniteCoordinates()) {
            "minPoint deve conter coordenadas finitas."
        }

        require(maxPoint.hasFiniteCoordinates()) {
            "maxPoint deve conter coordenadas finitas."
        }

        require(minPoint.x <= maxPoint.x) {
            "minPoint.x não pode ser maior que maxPoint.x."
        }

        require(minPoint.y <= maxPoint.y) {
            "minPoint.y não pode ser maior que maxPoint.y."
        }

        require(minPoint.z <= maxPoint.z) {
            "minPoint.z não pode ser maior que maxPoint.z."
        }
    }

    val widthMeters: Float
        get() = maxPoint.x - minPoint.x

    val heightMeters: Float
        get() = maxPoint.y - minPoint.y

    val depthMeters: Float
        get() = maxPoint.z - minPoint.z

    /**
     * Volume em metros cúbicos (m³).
     */
    val volumeCubicMeters: Float
        get() = widthMeters * heightMeters * depthMeters

    /**
     * Volume em litros: 1 m³ = 1.000 L.
     */
    val volumeLiters: Float
        get() = volumeCubicMeters * LITERS_PER_CUBIC_METER

    fun center(): Point3D {
        return Point3D(
            x = minPoint.x + widthMeters / 2f,
            y = minPoint.y + heightMeters / 2f,
            z = minPoint.z + depthMeters / 2f
        )
    }

    companion object {
        private const val LITERS_PER_CUBIC_METER = 1_000f

        /**
         * Cria uma caixa delimitadora a partir de dois
         * pontos extremos em qualquer ordem.
         */
        fun fromPoints(
            firstPoint: Point3D,
            secondPoint: Point3D
        ): BoundingBox3D {
            return BoundingBox3D(
                minPoint = Point3D(
                    x = min(firstPoint.x, secondPoint.x),
                    y = min(firstPoint.y, secondPoint.y),
                    z = min(firstPoint.z, secondPoint.z)
                ),
                maxPoint = Point3D(
                    x = max(firstPoint.x, secondPoint.x),
                    y = max(firstPoint.y, secondPoint.y),
                    z = max(firstPoint.z, secondPoint.z)
                )
            )
        }

        /**
         * Cria uma caixa delimitadora que envolve todos
         * os pontos válidos da coleção.
         *
         * Pontos com coordenadas não finitas são ignorados.
         * Retorna null quando não existe nenhum ponto válido.
         */
        fun fromPointCloud(
            points: Iterable<Point3D>
        ): BoundingBox3D? {
            var foundValidPoint = false

            var minX = 0f
            var minY = 0f
            var minZ = 0f

            var maxX = 0f
            var maxY = 0f
            var maxZ = 0f

            for (point in points) {
                if (!point.hasFiniteCoordinates()) {
                    continue
                }

                if (!foundValidPoint) {
                    minX = point.x
                    minY = point.y
                    minZ = point.z

                    maxX = point.x
                    maxY = point.y
                    maxZ = point.z

                    foundValidPoint = true
                    continue
                }

                minX = min(minX, point.x)
                minY = min(minY, point.y)
                minZ = min(minZ, point.z)

                maxX = max(maxX, point.x)
                maxY = max(maxY, point.y)
                maxZ = max(maxZ, point.z)
            }

            if (!foundValidPoint) {
                return null
            }

            return BoundingBox3D(
                minPoint = Point3D(
                    x = minX,
                    y = minY,
                    z = minZ
                ),
                maxPoint = Point3D(
                    x = maxX,
                    y = maxY,
                    z = maxZ
                )
            )
        }
    }
}