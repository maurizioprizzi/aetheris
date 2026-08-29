package org.aetheris.app.domain.usecase

import org.aetheris.app.domain.model.DistanceMeasurement
import org.aetheris.app.domain.model.Point3D

/**
 * Calcula a distância euclidiana entre dois pontos
 * no espaço tridimensional do ARCore.
 *
 * Produz uma estimativa heurística de incerteza:
 *
 * u = uBase * (1 + d / dRef) / cEfetiva
 *
 * Essa estimativa não representa uma calibração
 * metrológica certificada do aparelho.
 */
class CalculateDistanceUseCase(
    private val baseUncertaintyMeters: Float =
        DEFAULT_BASE_UNCERTAINTY_METERS,
    private val referenceDistanceMeters: Float =
        DEFAULT_REFERENCE_DISTANCE_METERS,
    private val minimumConfidence: Float =
        DEFAULT_MINIMUM_CONFIDENCE
) {
    init {
        require(
            baseUncertaintyMeters.isFinite() &&
                    baseUncertaintyMeters >= 0f
        ) {
            "A incerteza base deve ser finita e não negativa."
        }

        require(
            referenceDistanceMeters.isFinite() &&
                    referenceDistanceMeters > 0f
        ) {
            "A distância de referência deve ser maior que zero."
        }

        require(
            minimumConfidence.isFinite() &&
                    minimumConfidence in VALID_CONFIDENCE_RANGE
        ) {
            "A confiança mínima deve estar entre 0 e 1."
        }
    }

    operator fun invoke(
        start: Point3D,
        end: Point3D,
        confidenceScore: Float = MAXIMUM_CONFIDENCE
    ): DistanceMeasurement {
        require(
            confidenceScore.isFinite() &&
                    confidenceScore > 0f &&
                    confidenceScore <= MAXIMUM_CONFIDENCE
        ) {
            "O nível de confiança deve estar entre " +
                    "0 (exclusivo) e 1 (inclusivo)."
        }

        val distanceMeters = start.distanceTo(end)

        require(distanceMeters.isFinite()) {
            "Não foi possível calcular uma distância finita."
        }

        val effectiveConfidence =
            confidenceScore.coerceAtLeast(minimumConfidence)

        val distanceFactor =
            1f + distanceMeters / referenceDistanceMeters

        val estimatedUncertaintyMeters =
            baseUncertaintyMeters *
                    distanceFactor /
                    effectiveConfidence

        require(estimatedUncertaintyMeters.isFinite()) {
            "Não foi possível calcular uma incerteza finita."
        }

        return DistanceMeasurement(
            meters = distanceMeters,
            uncertaintyMeters = estimatedUncertaintyMeters
        )
    }

    private companion object {
        const val DEFAULT_BASE_UNCERTAINTY_METERS = 0.015f
        const val DEFAULT_REFERENCE_DISTANCE_METERS = 2f
        const val DEFAULT_MINIMUM_CONFIDENCE = 0.1f
        const val MAXIMUM_CONFIDENCE = 1f

        val VALID_CONFIDENCE_RANGE = 0f..1f
    }
}