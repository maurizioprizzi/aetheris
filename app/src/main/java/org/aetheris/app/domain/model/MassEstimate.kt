package org.aetheris.app.domain.model

import java.util.Locale
import kotlin.math.max

data class MassEstimate(
    val kilograms: Float,

    /**
     * Margem de incerteza: o resultado é expresso como
     * kilograms ± confidenceIntervalKg.
     */
    val confidenceIntervalKg: Float,

    val densityUsedKgPerM3: Float
) {
    init {
        require(kilograms.isFinite() && kilograms >= 0f) {
            "A massa deve ser um valor finito e não negativo."
        }

        require(
            confidenceIntervalKg.isFinite() &&
                    confidenceIntervalKg >= 0f
        ) {
            "A incerteza deve ser um valor finito e não negativo."
        }

        require(
            densityUsedKgPerM3.isFinite() &&
                    densityUsedKgPerM3 > 0f
        ) {
            "A densidade deve ser um valor finito e maior que zero."
        }
    }

    val grams: Float
        get() = kilograms * GRAMS_PER_KILOGRAM

    val uncertaintyGrams: Float
        get() = confidenceIntervalKg * GRAMS_PER_KILOGRAM

    val minimumKilograms: Float
        get() = max(0f, kilograms - confidenceIntervalKg)

    val maximumKilograms: Float
        get() = kilograms + confidenceIntervalKg

    /**
     * Incerteza relativa percentual: (incerteza / massa) * 100%.
     * Retorna 0% se a massa for zero.
     */
    val relativeUncertaintyPercentage: Float
        get() = if (kilograms > 0f) {
            (confidenceIntervalKg / kilograms) * 100f
        } else {
            0f
        }

    /**
     * Formata a massa com sua incerteza associada.
     * Alterna automaticamente entre gramas (< 1 kg) e quilogramas (>= 1 kg).
     */
    fun formatted(
        locale: Locale = Locale.getDefault()
    ): String {
        return when {
            kilograms < KILOGRAM_DISPLAY_THRESHOLD -> {
                String.format(
                    locale,
                    "%.1f g (±%.1f g)",
                    grams,
                    uncertaintyGrams
                )
            }

            else -> {
                String.format(
                    locale,
                    "%.2f kg (±%.2f kg)",
                    kilograms,
                    confidenceIntervalKg
                )
            }
        }
    }

    /**
     * Formata apenas o valor principal sem a incerteza (ex: "450 g" ou "2.35 kg").
     */
    fun formattedValueOnly(
        locale: Locale = Locale.getDefault()
    ): String {
        return when {
            kilograms < KILOGRAM_DISPLAY_THRESHOLD -> {
                String.format(locale, "%.1f g", grams)
            }

            else -> {
                String.format(locale, "%.2f kg", kilograms)
            }
        }
    }

    companion object {
        private const val GRAMS_PER_KILOGRAM = 1_000f
        private const val KILOGRAM_DISPLAY_THRESHOLD = 1f
    }
}