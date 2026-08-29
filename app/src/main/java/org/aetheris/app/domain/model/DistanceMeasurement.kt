package org.aetheris.app.domain.model

import java.util.Locale

/**
 * Representa uma medição de distância com sua
 * respectiva estimativa de incerteza.
 *
 * @property meters Distância medida em metros.
 * @property uncertaintyMeters Incerteza estimada em metros.
 * @property timestampMillis Momento em que a medição foi criada.
 */
data class DistanceMeasurement(
    val meters: Float,
    val uncertaintyMeters: Float =
        DEFAULT_UNCERTAINTY_METERS,
    val timestampMillis: Long =
        System.currentTimeMillis()
) {

    init {
        require(
            meters.isFinite() &&
                    meters >= 0f
        ) {
            "A distância deve ser um valor finito e não negativo."
        }

        require(
            uncertaintyMeters.isFinite() &&
                    uncertaintyMeters >= 0f
        ) {
            "A incerteza deve ser um valor finito e não negativo."
        }

        require(timestampMillis >= 0L) {
            "O timestamp não pode ser negativo."
        }
    }

    val centimeters: Float
        get() = meters * CENTIMETERS_PER_METER

    val millimeters: Float
        get() = meters * MILLIMETERS_PER_METER

    val uncertaintyCentimeters: Float
        get() =
            uncertaintyMeters * CENTIMETERS_PER_METER

    val uncertaintyMillimeters: Float
        get() =
            uncertaintyMeters * MILLIMETERS_PER_METER

    /**
     * Incerteza relativa percentual:
     *
     * (incerteza / distância) × 100
     *
     * Retorna 0% quando a distância é zero.
     */
    val relativeUncertaintyPercentage: Float
        get() = if (meters > 0f) {
            (
                    uncertaintyMeters /
                            meters
                    ) * PERCENTAGE_MULTIPLIER
        } else {
            0f
        }

    /**
     * Formata a distância com sua incerteza.
     *
     * Exemplos:
     * - 45,2 cm (±2,0 cm)
     * - 1,25 m (±0,02 m)
     */
    fun formattedMetric(
        locale: Locale = Locale.getDefault()
    ): String {
        return if (
            meters < CENTIMETER_DISPLAY_THRESHOLD_METERS
        ) {
            String.format(
                locale,
                "%.1f cm (±%.1f cm)",
                centimeters,
                uncertaintyCentimeters
            )
        } else {
            String.format(
                locale,
                "%.2f m (±%.2f m)",
                meters,
                uncertaintyMeters
            )
        }
    }

    /**
     * Formata somente o valor principal,
     * sem apresentar a incerteza.
     */
    fun formattedValueOnly(
        locale: Locale = Locale.getDefault()
    ): String {
        return if (
            meters < CENTIMETER_DISPLAY_THRESHOLD_METERS
        ) {
            String.format(
                locale,
                "%.1f cm",
                centimeters
            )
        } else {
            String.format(
                locale,
                "%.2f m",
                meters
            )
        }
    }

    companion object {
        const val DEFAULT_UNCERTAINTY_METERS = 0.02f

        private const val CENTIMETERS_PER_METER = 100f
        private const val MILLIMETERS_PER_METER = 1_000f
        private const val PERCENTAGE_MULTIPLIER = 100f

        private const val CENTIMETER_DISPLAY_THRESHOLD_METERS =
            1f
    }
}