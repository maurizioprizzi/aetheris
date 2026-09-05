package org.aetheris.app.domain.model

/**
 * Representa uma dimensão espacial confirmada juntamente
 * com a procedência dos dois pontos que a produziram.
 *
 * A distância permanece modelada por [DistanceMeasurement],
 * preservando seu valor, incerteza e instante de captura.
 *
 * As fontes podem ser nulas para manter compatibilidade com
 * medições anteriores à introdução da procedência espacial.
 * Também é permitido preservar procedência parcial quando
 * apenas um dos pontos possui origem conhecida.
 *
 * @property measurement Distância medida entre os pontos.
 * @property startSource Origem espacial do ponto inicial.
 * @property endSource Origem espacial do ponto final.
 */
data class DimensionMeasurement(
    val measurement: DistanceMeasurement,
    val startSource: AnchorPlacementSource? = null,
    val endSource: AnchorPlacementSource? = null
) {

    /**
     * Fontes conhecidas, preservando a ordem inicial e final.
     *
     * Fontes ausentes não são incluídas na coleção.
     */
    val knownSources: List<AnchorPlacementSource>
        get() = listOfNotNull(
            startSource,
            endSource
        )

    /**
     * Quantidade de pontos cuja procedência é conhecida.
     */
    val knownSourceCount: Int
        get() = knownSources.size

    /**
     * Indica que pelo menos um ponto possui procedência.
     */
    val hasAnyProvenance: Boolean
        get() = knownSourceCount > 0

    /**
     * Indica que os dois pontos possuem procedência conhecida.
     */
    val hasCompleteProvenance: Boolean
        get() = startSource != null &&
                endSource != null

    /**
     * Indica que somente um dos pontos possui procedência.
     */
    val hasPartialProvenance: Boolean
        get() = knownSourceCount == 1

    /**
     * Indica que pelo menos um ponto foi criado por
     * Instant Placement e começou com profundidade aproximada.
     */
    val usesApproximatePlacement: Boolean
        get() = knownSources.any { source ->
            source.isApproximate
        }

    /**
     * Indica que os dois pontos possuem procedência conhecida
     * e foram produzidos por hit tests convencionais.
     *
     * Uma dimensão sem procedência completa não é classificada
     * automaticamente como totalmente convencional.
     */
    val isFullyConventional: Boolean
        get() = hasCompleteProvenance &&
                knownSources.all { source ->
                    source.isConventional
                }

    /**
     * Indica que pelo menos um ponto utilizou dados
     * provenientes da Depth API.
     */
    val usesDepth: Boolean
        get() = knownSources.any { source ->
            source.usesDepth
        }

    /**
     * Indica que pelo menos uma das poses pode sofrer
     * refinamento relevante após sua criação inicial.
     */
    val mayRefineOverTime: Boolean
        get() = knownSources.any { source ->
            source.mayRefineOverTime
        }

    /**
     * Retorna a procedência correspondente ao slot informado.
     */
    operator fun get(
        slot: AnchorSlot
    ): AnchorPlacementSource? {
        return when (slot) {
            AnchorSlot.START -> startSource
            AnchorSlot.END -> endSource
        }
    }
}