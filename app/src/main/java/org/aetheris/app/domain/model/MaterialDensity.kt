package org.aetheris.app.domain.model

import java.util.Locale
import kotlin.math.max

/**
 * Representa a densidade estimada de um material.
 *
 * A densidade é expressa em quilogramas por metro cúbico
 * e pode carregar uma margem de incerteza associada.
 *
 * Esse modelo será utilizado futuramente para estimar
 * a massa de objetos por meio da relação:
 *
 * massa = volume × densidade
 *
 * A densidade real de um objeto pode variar conforme
 * composição, umidade, porosidade, fabricação e presença
 * de espaços vazios. Portanto, o resultado deve ser
 * tratado como uma estimativa, não como pesagem direta.
 *
 * @property materialName Nome apresentado ao usuário.
 * @property kilogramsPerCubicMeter Densidade nominal em kg/m³.
 * @property uncertaintyKilogramsPerCubicMeter Margem de
 * incerteza da densidade em kg/m³.
 */
data class MaterialDensity(
    val materialName: String,
    val kilogramsPerCubicMeter: Float,
    val uncertaintyKilogramsPerCubicMeter: Float
) {
    init {
        require(materialName.isNotBlank()) {
            "O nome do material não pode estar vazio."
        }

        require(
            kilogramsPerCubicMeter.isFinite() &&
                    kilogramsPerCubicMeter > 0f
        ) {
            "A densidade deve ser finita e maior que zero."
        }

        require(
            uncertaintyKilogramsPerCubicMeter.isFinite() &&
                    uncertaintyKilogramsPerCubicMeter >= 0f
        ) {
            "A incerteza da densidade deve ser finita " +
                    "e não negativa."
        }

        require(
            (
                    kilogramsPerCubicMeter.toDouble() +
                            uncertaintyKilogramsPerCubicMeter
                                .toDouble()
                    ) <= Float.MAX_VALUE.toDouble()
        ) {
            "O limite máximo da densidade deve ser finito."
        }
    }

    /**
     * Densidade convertida para gramas por centímetro cúbico.
     *
     * 1 g/cm³ = 1.000 kg/m³.
     */
    val gramsPerCubicCentimeter: Float
        get() = kilogramsPerCubicMeter /
                KILOGRAMS_PER_CUBIC_METER_IN_GRAM_PER_CUBIC_CENTIMETER

    /**
     * Incerteza convertida para gramas por centímetro cúbico.
     */
    val uncertaintyGramsPerCubicCentimeter: Float
        get() = uncertaintyKilogramsPerCubicMeter /
                KILOGRAMS_PER_CUBIC_METER_IN_GRAM_PER_CUBIC_CENTIMETER

    /**
     * Menor densidade possível considerando a incerteza.
     *
     * O resultado nunca será negativo.
     */
    val minimumKilogramsPerCubicMeter: Float
        get() = max(
            0f,
            kilogramsPerCubicMeter -
                    uncertaintyKilogramsPerCubicMeter
        )

    /**
     * Maior densidade possível considerando a incerteza.
     */
    val maximumKilogramsPerCubicMeter: Float
        get() = kilogramsPerCubicMeter +
                uncertaintyKilogramsPerCubicMeter

    /**
     * Incerteza relativa percentual da densidade.
     */
    val relativeUncertaintyPercentage: Float
        get() = (
                uncertaintyKilogramsPerCubicMeter /
                        kilogramsPerCubicMeter
                ) * PERCENTAGE_MULTIPLIER

    /**
     * Indica que a densidade foi informada sem
     * margem de incerteza.
     */
    val isExactReference: Boolean
        get() = uncertaintyKilogramsPerCubicMeter == 0f

    /**
     * Formata o nome, a densidade e sua incerteza.
     *
     * Exemplo:
     *
     * Madeira: 650.0 kg/m³ (±75.0 kg/m³)
     */
    fun formattedMetric(
        locale: Locale = Locale.getDefault()
    ): String {
        return String.format(
            locale,
            "%s: %.1f kg/m³ (±%.1f kg/m³)",
            materialName,
            kilogramsPerCubicMeter,
            uncertaintyKilogramsPerCubicMeter
        )
    }

    /**
     * Formata somente o valor nominal da densidade.
     */
    fun formattedValueOnly(
        locale: Locale = Locale.getDefault()
    ): String {
        return String.format(
            locale,
            "%.1f kg/m³",
            kilogramsPerCubicMeter
        )
    }

    private companion object {
        const val
                KILOGRAMS_PER_CUBIC_METER_IN_GRAM_PER_CUBIC_CENTIMETER =
            1_000f

        const val PERCENTAGE_MULTIPLIER = 100f
    }
}