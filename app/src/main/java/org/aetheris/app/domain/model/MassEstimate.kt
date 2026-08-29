package org.aetheris.app.domain.model

import java.util.Locale
import kotlin.math.max

/**
 * Representa uma estimativa de massa calculada
 * a partir do volume e da densidade de um material.
 *
 * @property kilograms Massa estimada em quilogramas.
 * @property confidenceIntervalKg Margem de incerteza em quilogramas.
 * @property densityUsedKgPerM3 Densidade utilizada no cálculo, em kg/m³.
 */
data class MassEstimate(
    val kilograms: Float,
    val confidenceIntervalKg: Float,
    val densityUsedKgPerM3: Float
) {

    init {
        require(
            kilograms.isFinite() &&
                    kilograms >= 0f
        ) {
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
        get() = max(
            0f,
            kilograms - confidenceIntervalKg
        )

    val maximumKilograms: Float
        get() = kilograms + confidenceIntervalKg

    /**
     * Incerteza relativa percentual:
     *
     * (incerteza / massa) × 100
     *
     * Retorna 0% quando a massa é zero.
     */
    val relativeUncertaintyPercentage: Float
        get() = if (kilograms > 0f) {
            (
                    confidenceIntervalKg /
                            kilograms
                    ) * PERCENTAGE_MULTIPLIER
        } else {
            0f
        }

    /**
     * Formata a massa com sua incerteza associada.
     *
     * Utiliza gramas para valores menores que 1 kg
     * e quilogramas para valores iguais ou maiores que 1 kg.
     */
    fun formatted(
        locale: Locale = Locale.getDefault()
    ): String {
        return if (kilograms < KILOGRAM_DISPLAY_THRESHOLD) {
            String.format(
                locale,
                "%.1f g (±%.1f g)",
                grams,
                uncertaintyGrams
            )
        } else {
            String.format(
                locale,
                "%.2f kg (±%.2f kg)",
                kilograms,
                confidenceIntervalKg
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
        return if (kilograms < KILOGRAM_DISPLAY_THRESHOLD) {
            String.format(
                locale,
                "%.1f g",
                grams
            )
        } else {
            String.format(
                locale,
                "%.2f kg",
                kilograms
            )
        }
    }

    private companion object {
        const val GRAMS_PER_KILOGRAM = 1_000f
        const val KILOGRAM_DISPLAY_THRESHOLD = 1f
        const val PERCENTAGE_MULTIPLIER = 100f
    }
}