package org.aetheris.app.domain.model

/**
 * Estratégia física utilizada para estimar a massa de um objeto.
 *
 * A estratégia descreve como o volume ocupado por um ou mais
 * materiais será inferido. Ela não representa, isoladamente,
 * uma garantia de precisão metrológica.
 *
 * A seleção de uma estratégia deve permanecer explícita e
 * rastreável. Reconhecimento visual futuro poderá sugerir uma
 * estratégia, mas não deverá confirmá-la silenciosamente.
 *
 * Artifact revision: day18-v1.
 */
enum class PhysicalEstimationStrategy {

    /**
     * Considera que o volume externo medido está integralmente
     * ocupado por um único material predominantemente homogêneo.
     *
     * Corresponde ao cálculo atualmente implementado:
     *
     * massa = volume × densidade
     */
    HOMOGENEOUS_SOLID,

    /**
     * Aplica ao volume externo uma fração de ocupação declarada.
     *
     * A fração e sua incerteza fazem parte das hipóteses do modelo
     * e não devem ser inferidas sem procedência explícita.
     */
    OCCUPANCY_ADJUSTED,

    /**
     * Estima o volume material a partir de painéis estruturais,
     * suas dimensões e espessuras.
     *
     * É adequada para algumas estantes, mesas e estruturas de MDF,
     * madeira, vidro ou outros materiais em placas.
     */
    PANEL_ASSEMBLY,

    /**
     * Estima o volume material pela diferença entre a geometria
     * externa e a geometria interna de uma casca ou recipiente.
     *
     * Eventuais conteúdos devem ser modelados separadamente.
     */
    SHELL_OR_CONTAINER,

    /**
     * Combina componentes com geometrias, materiais e incertezas
     * potencialmente diferentes.
     *
     * É adequada para livros e outros objetos compostos quando
     * existem dados suficientes sobre sua construção.
     */
    COMPONENT_COMPOSITION,

    /**
     * Utiliza dados experimentais ou referências documentadas de
     * uma família de objetos, com faixa de aplicação e incerteza.
     *
     * Não representa uma medição direta do objeto observado.
     */
    EMPIRICAL_REFERENCE;

    /**
     * Indica que a estratégia utiliza diretamente todo o volume
     * externo medido como volume material.
     */
    val usesFullExternalVolume: Boolean
        get() = this == HOMOGENEOUS_SOLID

    /**
     * Indica que o modelo precisa de parâmetros estruturais além
     * das dimensões externas e de uma densidade nominal.
     */
    val requiresAdditionalModelParameters: Boolean
        get() = this != HOMOGENEOUS_SOLID

    /**
     * Indica que a massa é derivada de uma representação geométrica
     * explícita, em vez de uma referência empírica direta.
     */
    val usesAnalyticalGeometry: Boolean
        get() = this != EMPIRICAL_REFERENCE

    /**
     * Indica que a estratégia depende de uma fração de ocupação.
     */
    val requiresOccupancyFraction: Boolean
        get() = this == OCCUPANCY_ADJUSTED

    /**
     * Indica que a estratégia depende da definição de painéis.
     */
    val requiresPanelGeometry: Boolean
        get() = this == PANEL_ASSEMBLY

    /**
     * Indica que a estratégia depende de geometria interna ou
     * espessura de parede para determinar o volume material.
     */
    val requiresInternalGeometry: Boolean
        get() = this == SHELL_OR_CONTAINER

    /**
     * Indica que componentes físicos precisam ser modelados
     * individualmente antes da soma das massas.
     */
    val requiresComponentDefinitions: Boolean
        get() = this == COMPONENT_COMPOSITION

    /**
     * Indica que o cálculo depende de dados experimentais ou de
     * uma referência externa cuja procedência deve ser preservada.
     */
    val requiresEmpiricalReference: Boolean
        get() = this == EMPIRICAL_REFERENCE

    /**
     * Indica que a estratégia é compatível com o cálculo atual
     * de volume multiplicado diretamente pela densidade.
     */
    val supportsCurrentMassCalculation: Boolean
        get() = this == HOMOGENEOUS_SOLID
}
