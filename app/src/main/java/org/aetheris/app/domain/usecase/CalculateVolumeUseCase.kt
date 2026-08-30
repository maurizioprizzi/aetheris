package org.aetheris.app.domain.usecase

import org.aetheris.app.domain.model.DistanceMeasurement
import org.aetheris.app.domain.model.SpatialDimensions
import org.aetheris.app.domain.model.VolumeMeasurement
import kotlin.math.hypot

/**
 * Calcula o volume de uma caixa delimitadora espacial
 * a partir de largura, altura e profundidade.
 *
 * O volume é calculado por:
 *
 * V = largura × altura × profundidade
 *
 * A incerteza é propagada considerando as incertezas
 * independentes das três dimensões:
 *
 * uV = sqrt(
 *     (altura × profundidade × uLargura)² +
 *     (largura × profundidade × uAltura)² +
 *     (largura × altura × uProfundidade)²
 * )
 *
 * O resultado representa uma aproximação geométrica baseada
 * em uma caixa delimitadora, e não o volume exato do objeto.
 */
class CalculateVolumeUseCase(
    private val timestampProvider: () -> Long = {
        System.currentTimeMillis()
    }
) {

    /**
     * Calcula o volume a partir de um conjunto completo
     * de dimensões espaciais.
     *
     * @throws IllegalArgumentException quando alguma das
     * três dimensões ainda não foi medida.
     */
    operator fun invoke(
        dimensions: SpatialDimensions
    ): VolumeMeasurement {
        require(dimensions.isComplete) {
            "Largura, altura e profundidade são necessárias " +
                    "para calcular o volume."
        }

        return invoke(
            width = requireNotNull(dimensions.width),
            height = requireNotNull(dimensions.height),
            depth = requireNotNull(dimensions.depth)
        )
    }

    /**
     * Calcula o volume diretamente a partir das três
     * medições de distância.
     */
    operator fun invoke(
        width: DistanceMeasurement,
        height: DistanceMeasurement,
        depth: DistanceMeasurement
    ): VolumeMeasurement {
        val widthMeters = width.meters.toDouble()
        val heightMeters = height.meters.toDouble()
        val depthMeters = depth.meters.toDouble()

        val volumeCubicMeters =
            widthMeters *
                    heightMeters *
                    depthMeters

        /*
         * Cada contribuição representa quanto a incerteza
         * de um eixo influencia o volume final.
         */
        val widthUncertaintyContribution =
            heightMeters *
                    depthMeters *
                    width.uncertaintyMeters.toDouble()

        val heightUncertaintyContribution =
            widthMeters *
                    depthMeters *
                    height.uncertaintyMeters.toDouble()

        val depthUncertaintyContribution =
            widthMeters *
                    heightMeters *
                    depth.uncertaintyMeters.toDouble()

        /*
         * hypot é utilizado para melhorar a estabilidade
         * numérica do cálculo da raiz da soma dos quadrados.
         */
        val volumeUncertaintyCubicMeters =
            hypot(
                hypot(
                    widthUncertaintyContribution,
                    heightUncertaintyContribution
                ),
                depthUncertaintyContribution
            )

        return VolumeMeasurement(
            cubicMeters = volumeCubicMeters.toFiniteFloat(
                valueName = "volume"
            ),
            uncertaintyCubicMeters =
                volumeUncertaintyCubicMeters.toFiniteFloat(
                    valueName = "incerteza do volume"
                ),
            timestampMillis = timestampProvider()
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