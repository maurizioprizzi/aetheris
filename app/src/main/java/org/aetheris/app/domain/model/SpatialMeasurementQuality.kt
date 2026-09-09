package org.aetheris.app.domain.model

/**
 * Consolida a qualidade de uma medição espacial tridimensional.
 *
 * A avaliação utiliza as procedências preservadas em cada
 * [DimensionMeasurement] para distinguir medições inteiramente
 * convencionais, inteiramente aproximadas e aquelas que combinam
 * os dois tipos de posicionamento.
 *
 * Uma medição somente recebe uma classificação conclusiva quando:
 *
 * 1. largura, altura e profundidade foram medidas;
 * 2. os pontos inicial e final dos três eixos possuem procedência.
 *
 * Caso alguma dimensão ou procedência esteja ausente, a classificação
 * permanece [Classification.INCOMPLETE]. Isso impede que medições
 * legadas ou parcialmente documentadas sejam interpretadas como
 * completamente convencionais.
 *
 * Esta classe descreve rastreabilidade técnica, não precisão
 * metrológica certificada. Uma medição convencional ainda depende
 * da calibração do aparelho, da estabilidade do rastreamento e das
 * condições físicas da cena.
 */
class SpatialMeasurementQuality private constructor(

    /** Classificação consolidada da medição. */
    val classification: Classification,

    /** Quantidade de eixos que possuem uma distância medida. */
    val measuredAxisCount: Int,

    /**
     * Quantidade de procedências esperadas para os eixos medidos.
     *
     * Cada dimensão utiliza duas âncoras: uma inicial e outra final.
     */
    val expectedAnchorCount: Int,

    /** Quantidade de âncoras cuja procedência é conhecida. */
    val knownAnchorCount: Int,

    /** Quantidade de âncoras produzidas por hit tests convencionais. */
    val conventionalAnchorCount: Int,

    /** Quantidade de âncoras produzidas por Instant Placement. */
    val approximateAnchorCount: Int,

    /** Quantidade de âncoras provenientes da Depth API. */
    val depthAnchorCount: Int,

    /**
     * Quantidade de âncoras cujas poses podem sofrer refinamento
     * espacial relevante depois do posicionamento inicial.
     */
    val refinableAnchorCount: Int
) {

    /**
     * Classificações possíveis para uma medição espacial.
     */
    enum class Classification {

        /**
         * Faltam dimensões ou procedências para uma avaliação
         * conclusiva.
         */
        INCOMPLETE,

        /**
         * Todas as seis âncoras possuem procedência conhecida e
         * foram criadas por hit tests convencionais.
         */
        CONVENTIONAL,

        /**
         * A medição combina âncoras convencionais e aproximadas.
         */
        MIXED,

        /**
         * Todas as seis âncoras foram criadas por Instant Placement.
         */
        APPROXIMATE
    }

    /**
     * Quantidade de âncoras esperadas cuja procedência está ausente.
     */
    val unknownAnchorCount: Int
        get() = expectedAnchorCount -
                knownAnchorCount

    /**
     * Indica que largura, altura e profundidade foram medidas.
     */
    val hasCompleteDimensions: Boolean
        get() = measuredAxisCount ==
                DimensionAxis.entries.size

    /**
     * Indica que todas as âncoras esperadas possuem procedência.
     *
     * Um conjunto sem eixos medidos não é considerado rastreável.
     */
    val hasCompleteProvenance: Boolean
        get() = expectedAnchorCount > 0 &&
                unknownAnchorCount == 0

    /**
     * Indica que a avaliação possui dimensões e procedência completas.
     */
    val isConclusive: Boolean
        get() = hasCompleteDimensions &&
                hasCompleteProvenance

    /**
     * Indica que ao menos uma âncora utilizou Instant Placement.
     */
    val containsApproximatePlacement: Boolean
        get() = approximateAnchorCount > 0

    /**
     * Indica que ao menos uma âncora utilizou dados da Depth API.
     */
    val usesDepth: Boolean
        get() = depthAnchorCount > 0

    /**
     * Indica que ao menos uma pose pode sofrer refinamento espacial.
     */
    val mayRefineOverTime: Boolean
        get() = refinableAnchorCount > 0

    /**
     * Indica que a medição completa deve ser confirmada explicitamente
     * antes de ser aceita pelo usuário.
     *
     * A confirmação é exigida quando existe Instant Placement ou
     * quando a procedência completa não está disponível.
     */
    val requiresExplicitConfirmation: Boolean
        get() = hasCompleteDimensions &&
                classification !=
                Classification.CONVENTIONAL

    /**
     * Indica que as dimensões permitem calcular volume e massa como
     * estimativas experimentais.
     *
     * Este valor não representa aprovação metrológica. Ele apenas
     * informa que os três valores dimensionais necessários existem.
     */
    val canProduceExperimentalEstimate: Boolean
        get() = hasCompleteDimensions

    companion object {

        private const val ANCHORS_PER_AXIS = 2

        /**
         * Avalia as dimensões e suas procedências espaciais.
         *
         * A operação é pura e determinística: não altera
         * [SpatialDimensions] nem depende de Android ou ARCore.
         */
        fun assess(
            dimensions: SpatialDimensions
        ): SpatialMeasurementQuality {
            val measuredAxisCount =
                dimensions.measuredAxisCount

            val expectedAnchorCount =
                measuredAxisCount *
                        ANCHORS_PER_AXIS

            val knownSources =
                DimensionAxis.entries.flatMap { axis ->
                    dimensions
                        .getDimensionMeasurement(axis)
                        ?.knownSources
                        .orEmpty()
                }

            val conventionalAnchorCount =
                knownSources.count { source ->
                    source.isConventional
                }

            val approximateAnchorCount =
                knownSources.count { source ->
                    source.isApproximate
                }

            val depthAnchorCount =
                knownSources.count { source ->
                    source.usesDepth
                }

            val refinableAnchorCount =
                knownSources.count { source ->
                    source.mayRefineOverTime
                }

            val classification =
                classify(
                    hasCompleteDimensions =
                        dimensions.isComplete,
                    expectedAnchorCount =
                        expectedAnchorCount,
                    knownAnchorCount =
                        knownSources.size,
                    approximateAnchorCount =
                        approximateAnchorCount
                )

            return SpatialMeasurementQuality(
                classification = classification,
                measuredAxisCount =
                    measuredAxisCount,
                expectedAnchorCount =
                    expectedAnchorCount,
                knownAnchorCount =
                    knownSources.size,
                conventionalAnchorCount =
                    conventionalAnchorCount,
                approximateAnchorCount =
                    approximateAnchorCount,
                depthAnchorCount =
                    depthAnchorCount,
                refinableAnchorCount =
                    refinableAnchorCount
            )
        }

        /**
         * Produz uma classificação somente quando a medição possui
         * os três eixos e todas as procedências esperadas.
         */
        private fun classify(
            hasCompleteDimensions: Boolean,
            expectedAnchorCount: Int,
            knownAnchorCount: Int,
            approximateAnchorCount: Int
        ): Classification {
            if (
                !hasCompleteDimensions ||
                expectedAnchorCount == 0 ||
                knownAnchorCount !=
                expectedAnchorCount
            ) {
                return Classification.INCOMPLETE
            }

            return when (approximateAnchorCount) {
                0 -> Classification.CONVENTIONAL
                knownAnchorCount ->
                    Classification.APPROXIMATE

                else -> Classification.MIXED
            }
        }
    }
}