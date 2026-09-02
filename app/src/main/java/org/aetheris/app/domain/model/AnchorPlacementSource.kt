package org.aetheris.app.domain.model

/**
 * Identifica a origem espacial utilizada para criar uma âncora.
 *
 * Essa informação permite que o domínio e a interface diferenciem
 * uma interseção baseada em geometria convencional do ARCore de uma
 * posição inicial aproximada produzida pelo Instant Placement.
 *
 * A origem não representa, isoladamente, uma garantia de precisão
 * metrológica. Mesmo resultados convencionais dependem da qualidade
 * do rastreamento, da calibração do dispositivo e das condições da cena.
 */
enum class AnchorPlacementSource {

    /**
     * Interseção com um plano rastreado cuja pose está dentro
     * do polígono reconhecido pelo ARCore.
     */
    PLANE,

    /**
     * Interseção com um ponto visual rastreado que possui
     * uma normal de superfície estimada.
     */
    FEATURE_POINT,

    /**
     * Interseção produzida a partir de dados de profundidade.
     *
     * Essa origem somente estará disponível quando o Depth Mode
     * estiver habilitado e operacional no dispositivo.
     */
    DEPTH_POINT,

    /**
     * Posição criada pelo Instant Placement.
     *
     * A profundidade começa com uma distância aproximada e a pose
     * pode ser refinada conforme o ARCore reconhece melhor a cena.
     */
    INSTANT_PLACEMENT;

    /**
     * Indica que a posição foi iniciada com profundidade aproximada.
     */
    val isApproximate: Boolean
        get() = this == INSTANT_PLACEMENT

    /**
     * Indica que a origem foi obtida pelo hit test convencional.
     *
     * Isso não significa que a medição possua precisão certificada.
     */
    val isConventional: Boolean
        get() = !isApproximate

    /**
     * Indica que a origem depende da Depth API.
     */
    val usesDepth: Boolean
        get() = this == DEPTH_POINT

    /**
     * Indica que a pose pode sofrer refinamento relevante
     * após sua criação inicial.
     */
    val mayRefineOverTime: Boolean
        get() = this == INSTANT_PLACEMENT
}