package org.aetheris.app.domain.model

/**
 * Representa um ponto do mundo 3D projetado na tela do dispositivo (espaço de visualização 2D).
 *
 * @property x Coordenada horizontal em pixels da View.
 * @property y Coordenada vertical em pixels da View.
 * @property isVisible Indica que o ponto está à frente da câmera e dentro dos limites da Viewport.
 *                     Não representa teste de oclusão por objetos físicos reais ou virtuais.
 */
data class ScreenPoint2D(
    val x: Float,
    val y: Float,
    val isVisible: Boolean
) {
    init {
        require(x.isFinite() && y.isFinite()) {
            "As coordenadas de tela devem ser finitas."
        }
    }

    val isOffscreen: Boolean
        get() = !isVisible

    companion object {
        val NOT_VISIBLE = ScreenPoint2D(
            x = 0f,
            y = 0f,
            isVisible = false
        )
    }
}