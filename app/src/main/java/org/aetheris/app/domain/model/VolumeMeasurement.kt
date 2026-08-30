package org.aetheris.app.domain.model

import java.util.Locale
import kotlin.math.max

/**
 * Representa uma medição de volume com sua incerteza estimada.
 *
 * @property cubicMeters Volume calculado em metros cúbicos.
 * @property uncertaintyCubicMeters Margem de incerteza em metros cúbicos.
 * @property timestampMillis Momento em que a medição foi produzida.
 */
data class VolumeMeasurement(
    val cubicMeters: Float,
    val uncertaintyCubicMeters: Float,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    init {
        require(
            cubicMeters.isFinite() &&
                    cubicMeters >= 0f
        ) {
            "O volume deve ser um valor finito e não negativo."
        }

        require(
            uncertaintyCubicMeters.isFinite() &&
                    uncertaintyCubicMeters >= 0f
        ) {
            "A incerteza do volume deve ser finita e não negativa."
        }

        require(timestampMillis >= 0L) {
            "O timestamp não pode ser negativo."
        }
    }

    /**
     * Volume convertido para litros.
     *
     * 1 m³ = 1.000 litros.
     */
    val liters: Float
        get() = cubicMeters * LITERS_PER_CUBIC_METER

    /**
     * Incerteza convertida para litros.
     */
    val uncertaintyLiters: Float
        get() = uncertaintyCubicMeters * LITERS_PER_CUBIC_METER

    /**
     * Menor volume possível considerando a incerteza.
     *
     * O resultado nunca será negativo.
     */
    val minimumCubicMeters: Float
        get() = max(
            0f,
            cubicMeters - uncertaintyCubicMeters
        )

    /**
     * Maior volume possível considerando a incerteza.
     */
    val maximumCubicMeters: Float
        get() = cubicMeters + uncertaintyCubicMeters

    val minimumLiters: Float
        get() = minimumCubicMeters * LITERS_PER_CUBIC_METER

    val maximumLiters: Float
        get() = maximumCubicMeters * LITERS_PER_CUBIC_METER

    /**
     * Incerteza relativa percentual.
     *
     * Retorna zero quando o volume medido é zero.
     */
    val relativeUncertaintyPercentage: Float
        get() = if (cubicMeters > 0f) {
            (
                    uncertaintyCubicMeters /
                            cubicMeters
                    ) * PERCENTAGE_MULTIPLIER
        } else {
            0f
        }

    /**
     * Formata o volume junto com sua incerteza.
     *
     * Valores menores que 1 m³ são apresentados em litros.
     */
    fun formattedMetric(
        locale: Locale = Locale.getDefault()
    ): String {
        return if (cubicMeters < CUBIC_METER_DISPLAY_THRESHOLD) {
            String.format(
                locale,
                "%.1f L (±%.1f L)",
                liters,
                uncertaintyLiters
            )
        } else {
            String.format(
                locale,
                "%.3f m³ (±%.3f m³)",
                cubicMeters,
                uncertaintyCubicMeters
            )
        }
    }

    /**
     * Formata somente o valor principal, sem a incerteza.
     */
    fun formattedValueOnly(
        locale: Locale = Locale.getDefault()
    ): String {
        return if (cubicMeters < CUBIC_METER_DISPLAY_THRESHOLD) {
            String.format(
                locale,
                "%.1f L",
                liters
            )
        } else {
            String.format(
                locale,
                "%.3f m³",
                cubicMeters
            )
        }
    }

    private companion object {
        const val LITERS_PER_CUBIC_METER = 1_000f
        const val PERCENTAGE_MULTIPLIER = 100f
        const val CUBIC_METER_DISPLAY_THRESHOLD = 1f
    }
}