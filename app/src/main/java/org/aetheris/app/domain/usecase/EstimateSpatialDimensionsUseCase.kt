package org.aetheris.app.domain.usecase

import org.aetheris.app.domain.model.BoundingBox3D
import org.aetheris.app.domain.model.Point3D

/**
 * Calcula uma caixa delimitadora alinhada aos eixos (AABB)
 * a partir de pontos no espaço tridimensional.
 */
class EstimateSpatialDimensionsUseCase {

    /**
     * Calcula a AABB que envolve todos os pontos válidos.
     *
     * @throws IllegalArgumentException quando a coleção não contém
     * nenhum ponto válido.
     */
    operator fun invoke(
        points: Iterable<Point3D>
    ): BoundingBox3D {
        return requireNotNull(
            BoundingBox3D.fromPointCloud(points)
        ) {
            "A nuvem de pontos deve conter pelo menos um ponto válido."
        }
    }

    /**
     * Calcula a AABB formada por dois pontos extremos.
     */
    operator fun invoke(
        firstPoint: Point3D,
        secondPoint: Point3D
    ): BoundingBox3D {
        return BoundingBox3D.fromPoints(
            firstPoint = firstPoint,
            secondPoint = secondPoint
        )
    }

    /**
     * Sobrecarga de conveniência para uma quantidade
     * variável de pontos.
     */
    operator fun invoke(
        vararg points: Point3D
    ): BoundingBox3D {
        return invoke(points.asIterable())
    }
}