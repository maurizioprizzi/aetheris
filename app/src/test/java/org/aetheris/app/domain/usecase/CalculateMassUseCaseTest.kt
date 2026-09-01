package org.aetheris.app.domain.usecase

import org.aetheris.app.domain.model.MassEstimate
import org.aetheris.app.domain.model.MaterialDensity
import org.aetheris.app.domain.model.VolumeMeasurement
import kotlin.math.hypot

/**
 * Estima a massa de um objeto a partir do seu volume
 * e da densidade nominal do material.
 *
 * A massa é calculada por:
 *
 * massa = volume × densidade
 *
 * onde:
 *
 * - o volume é expresso em metros cúbicos;
 * - a densidade é expressa em quilogramas por metro cúbico;
 * - o resultado é expresso em quilogramas.
 *
 * A incerteza da massa considera, de forma independente,
 * a incerteza do volume e a incerteza da densidade:
 *
 * uM = sqrt(
 *     (densidade × uVolume)² +
 *     (volume × uDensidade)²
 * )
 *
 * O resultado representa uma estimativa física. Ele não
 * substitui uma pesagem realizada por balança calibrada.
 */
class CalculateMassUseCaseTest {

    operator fun invoke(
        volume: VolumeMeasurement,
        materialDensity: MaterialDensity
    ): MassEstimate {
        val volumeCubicMeters =
            volume.cubicMeters.toDouble()

        val volumeUncertaintyCubicMeters =
            volume.uncertaintyCubicMeters.toDouble()

        val densityKilogramsPerCubicMeter =
            materialDensity
                .kilogramsPerCubicMeter
                .toDouble()

        val densityUncertaintyKilogramsPerCubicMeter =
            materialDensity
                .uncertaintyKilogramsPerCubicMeter
                .toDouble()

        /*
         * m = V × ρ
         */
        val estimatedKilograms =
            volumeCubicMeters *
                    densityKilogramsPerCubicMeter

        /*
         * Contribuição da incerteza do volume:
         *
         * ρ × uV
         */
        val volumeUncertaintyContribution =
            densityKilogramsPerCubicMeter *
                    volumeUncertaintyCubicMeters

        /*
         * Contribuição da incerteza da densidade:
         *
         * V × uρ
         */
        val densityUncertaintyContribution =
            volumeCubicMeters *
                    densityUncertaintyKilogramsPerCubicMeter

        /*
         * hypot melhora a estabilidade numérica do cálculo:
         *
         * sqrt(a² + b²)
         */
        val estimatedUncertaintyKilograms =
            hypot(
                volumeUncertaintyContribution,
                densityUncertaintyContribution
            )

        return MassEstimate(
            kilograms = estimatedKilograms.toFiniteFloat(
                valueName = "massa estimada"
            ),
            confidenceIntervalKg =
                estimatedUncertaintyKilograms.toFiniteFloat(
                    valueName = "incerteza da massa"
                ),
            densityUsedKgPerM3 =
                materialDensity.kilogramsPerCubicMeter
        )
    }

    private fun Double.toFiniteFloat(
        valueName: String
    ): Float {
        require(
            isFinite() &&
                    this >= 0.0 &&
                    this <= Float.MAX_VALUE.toDouble()
        ) {
            "Não foi possível calcular um valor finito " +
                    "para $valueName."
        }

        return toFloat()
    }
}