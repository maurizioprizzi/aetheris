package org.aetheris.app.domain.model

/**
 * Representa um ponto projetado na tela 2D do dispositivo em pixels físicos.
 *
 * @property x Coordenada horizontal em pixels (0 = borda esquerda, width = borda direita).
 * @property y Coordenada vertical em pixels (0 = topo, height = base).
 * @property isVisible Indica se o ponto está dentro do campo de visão da câmera (Frustum e w > 0).
 */
data class ScreenPoint2D(
    val x: Float,
    val y: Float,
    val isVisible: Boolean
)