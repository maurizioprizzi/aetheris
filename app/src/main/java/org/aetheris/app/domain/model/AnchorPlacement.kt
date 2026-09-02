package org.aetheris.app.domain.model

/**
 * Representa uma posição ancorada no espaço tridimensional
 * e registra a origem utilizada durante sua criação.
 *
 * A posição pode ser atualizada ao longo do rastreamento,
 * enquanto a origem identifica como a âncora foi criada.
 *
 * @property position Posição atual da âncora no espaço mundial,
 * expressa em metros.
 * @property source Origem espacial do resultado utilizado para
 * criar a âncora.
 */
data class AnchorPlacement(
    val position: Point3D,
    val source: AnchorPlacementSource
) {

    /**
     * Indica que a âncora foi iniciada usando uma
     * profundidade aproximada.
     */
    val isApproximate: Boolean
        get() = source.isApproximate

    /**
     * Indica que a âncora foi criada a partir de um
     * hit test convencional.
     *
     * Isso não representa certificação metrológica.
     */
    val isConventional: Boolean
        get() = source.isConventional

    /**
     * Indica que a origem da âncora depende da Depth API.
     */
    val usesDepth: Boolean
        get() = source.usesDepth

    /**
     * Retorna uma nova representação com a posição atualizada,
     * preservando a origem da criação.
     *
     * Esse comportamento é útil porque a pose de uma âncora
     * pode mudar conforme o mapa espacial do ARCore evolui.
     */
    fun withUpdatedPosition(
        newPosition: Point3D
    ): AnchorPlacement {
        if (newPosition == position) {
            return this
        }

        return copy(
            position = newPosition
        )
    }
}