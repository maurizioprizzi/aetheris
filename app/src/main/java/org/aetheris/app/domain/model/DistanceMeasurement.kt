package org.aetheris.app.domain.model

import java.util.Locale

data class DistanceMeasurement(
    val meters: Float,
    val uncertaintyMeters: Float = DEFAULT_UNCERTAINTY_METERS,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    init {
        require(meters.isFinite() && meters >= 0f) {
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
        get() = uncertaintyMeters * CENTIMETERS_PER_METER

    val uncertaintyMillimeters: Float
        get() = uncertaintyMeters * MILLIMETERS_PER_METER

    /**
     * Incerteza relativa percentual: (incerteza / valor medido) * 100%.
     * Retorna 0% se a distância for zero.
     */
    val relativeUncertaintyPercentage: Float
        get() = if (meters > 0f) {
            (uncertaintyMeters / meters) * 100f
        } else {
            0f
        }

    /**
     * Formata a distância com sua incerteza associada (ex: "1.25 m (±0.02 m)").
     */
    fun formattedMetric(
        locale: Locale = Locale.getDefault()
    ): String {
        return when {
            meters < CENTIMETER_DISPLAY_THRESHOLD_METERS -> {
                String.format(
                    locale,
                    "%.1f cm (±%.1f cm)",
                    centimeters,
                    uncertaintyCentimeters
                )
            }

            else -> {
                String.format(
                    locale,
                    "%.2f m (±%.2f m)",
                    meters,
                    uncertaintyMeters
                )
            }
        }
    }

    /**
     * Formata apenas o valor principal sem a incerteza (ex: "1.25 m" ou "45.2 cm").
     */
    fun formattedValueOnly(
        locale: Locale = Locale.getDefault()
    ): String {
        return when {
            meters < CENTIMETER_DISPLAY_THRESHOLD_METERS -> {
                String.format(locale, "%.1f cm", centimeters)
            }

            else -> {
                String.format(locale, "%.2f m", meters)
            }
        }
    }

    companion object {
        const val DEFAULT_UNCERTAINTY_METERS = 0.02f

        private const val CENTIMETERS_PER_METER = 100f
        private const val MILLIMETERS_PER_METER = 1_000f
        private const val CENTIMETER_DISPLAY_THRESHOLD_METERS = 1f
    }
}