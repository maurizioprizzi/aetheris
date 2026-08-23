package org.aetheris.app.domain.usecase

import org.aetheris.app.domain.model.BoundingBox3D
import org.aetheris.app.domain.model.Point3D

class EstimateSpatialDimensionsUseCase {

    /**
     * Calcula a caixa delimitadora 3D a partir de uma nuvem de pontos espaciais.
     */
    operator fun invoke(points: List<Point3D>): BoundingBox3D {
        require(points.isNotEmpty()) { "A nuvem de pontos não pode estar vazia." }

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.z < minZ) minZ = p.z

            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
            if (p.z > maxZ) maxZ = p.z
        }

        return BoundingBox3D(
            minPoint = Point3D(minX, minY, minZ),
            maxPoint = Point3D(maxX, maxY, maxZ)
        )
    }
}