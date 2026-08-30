package org.aetheris.app.domain.model

/**
 * Identifica cada eixo dimensional medido durante
 * uma sessão de medição tridimensional.
 *
 * A sequência padrão é:
 *
 * [WIDTH] -> [HEIGHT] -> [DEPTH]
 */
enum class DimensionAxis {

    /**
     * Largura horizontal do alvo.
     */
    WIDTH,

    /**
     * Altura vertical do alvo.
     */
    HEIGHT,

    /**
     * Profundidade do alvo.
     */
    DEPTH;

    /**
     * Retorna o próximo eixo da sequência de medição.
     *
     * Retorna null quando a profundidade já foi medida
     * e não existe outro eixo pendente.
     */
    val nextAxis: DimensionAxis?
        get() = when (this) {
            WIDTH -> HEIGHT
            HEIGHT -> DEPTH
            DEPTH -> null
        }

    /**
     * Indica se este é o primeiro eixo da sequência.
     */
    val isFirst: Boolean
        get() = this == FIRST

    /**
     * Indica se este é o último eixo da sequência.
     */
    val isLast: Boolean
        get() = nextAxis == null

    companion object {

        /**
         * Primeiro eixo utilizado ao iniciar
         * uma nova sessão de medição.
         */
        val FIRST: DimensionAxis = WIDTH
    }
}