package org.aetheris.app.domain.usecase

import org.aetheris.app.domain.model.DistanceMeasurement
import org.aetheris.app.domain.model.Point3D

class CalculateDistanceUseCase {

    operator fun invoke(
        start: Point3D = Point3D.ORIGIN,
        end: Point3D,
        confidenceScore: Float = 1.0f
    ): DistanceMeasurement {
        val rawDistance = start.distanceTo(end)

        // Em sensores ópticos monocular/ToF, a incerteza cresce proporcionalmente à distância
        val estimatedUncertainty = (0.015f * (1f + (rawDistance / 2f))) / confidenceScore.coerceIn(0.1f, 1.0f)

        return DistanceMeasurement(
            meters = rawDistance,
            uncertaintyMeters = estimatedUncertainty
        )
    }
}