package org.aetheris.app.domain.model

import kotlin.math.hypot

/**
 * Ponto imutável e vetor euclidiano em um sistema
 * cartesiano tridimensional.
 *
 * No espaço mundial do ARCore, as coordenadas são
 * expressas em metros.
 *
 * Os eixos seguem a convenção OpenGL:
 * +X para a direita, +Y para cima e +Z para trás.
 */
data class Point3D(
    val x: Float,
    val y: Float,
    val z: Float
) {

    init {
        require(hasFiniteCoordinates()) {
            "As coordenadas do Point3D devem ser finitas."
        }
    }

    /**
     * Calcula a distância euclidiana até outro ponto
     * utilizando precisão Double durante o cálculo.
     */
    fun distanceTo(other: Point3D): Float {
        val deltaX =
            x.toDouble() - other.x.toDouble()

        val deltaY =
            y.toDouble() - other.y.toDouble()

        val deltaZ =
            z.toDouble() - other.z.toDouble()

        return hypot(
            hypot(deltaX, deltaY),
            deltaZ
        ).toFloat()
    }

    /**
     * Calcula o ponto médio entre este ponto e outro.
     */
    fun midpointTo(other: Point3D): Point3D {
        return Point3D(
            x = (
                    (x.toDouble() + other.x.toDouble()) *
                            MIDPOINT_FACTOR
                    ).toFloat(),
            y = (
                    (y.toDouble() + other.y.toDouble()) *
                            MIDPOINT_FACTOR
                    ).toFloat(),
            z = (
                    (z.toDouble() + other.z.toDouble()) *
                            MIDPOINT_FACTOR
                    ).toFloat()
        )
    }

    /**
     * Magnitude do vetor em relação à origem.
     */
    val magnitude: Float
        get() = distanceTo(ORIGIN)

    /**
     * Retorna um vetor unitário com magnitude igual a 1.
     *
     * Retorna [ORIGIN] quando este vetor possui
     * magnitude zero.
     */
    fun normalized(): Point3D {
        val magnitudeDouble = hypot(
            hypot(
                x.toDouble(),
                y.toDouble()
            ),
            z.toDouble()
        )

        if (magnitudeDouble <= 0.0) {
            return ORIGIN
        }

        return Point3D(
            x = (x.toDouble() / magnitudeDouble).toFloat(),
            y = (y.toDouble() / magnitudeDouble).toFloat(),
            z = (z.toDouble() / magnitudeDouble).toFloat()
        )
    }

    /**
     * Produto escalar entre dois vetores.
     */
    infix fun dot(other: Point3D): Float {
        return x * other.x +
                y * other.y +
                z * other.z
    }

    /**
     * Produto vetorial entre dois vetores.
     *
     * Pode ser utilizado no cálculo de normais
     * de superfícies e áreas de polígonos 3D.
     */
    infix fun cross(other: Point3D): Point3D {
        return Point3D(
            x = y * other.z - z * other.y,
            y = z * other.x - x * other.z,
            z = x * other.y - y * other.x
        )
    }

    /**
     * Indica se todas as coordenadas são finitas.
     */
    fun hasFiniteCoordinates(): Boolean {
        return x.isFinite() &&
                y.isFinite() &&
                z.isFinite()
    }

    operator fun plus(other: Point3D): Point3D {
        return Point3D(
            x = x + other.x,
            y = y + other.y,
            z = z + other.z
        )
    }

    operator fun minus(other: Point3D): Point3D {
        return Point3D(
            x = x - other.x,
            y = y - other.y,
            z = z - other.z
        )
    }

    operator fun times(scalar: Float): Point3D {
        require(scalar.isFinite()) {
            "O escalar deve ser finito."
        }

        return Point3D(
            x = x * scalar,
            y = y * scalar,
            z = z * scalar
        )
    }

    operator fun div(scalar: Float): Point3D {
        require(
            scalar.isFinite() &&
                    scalar != 0f
        ) {
            "O divisor deve ser finito e diferente de zero."
        }

        return Point3D(
            x = x / scalar,
            y = y / scalar,
            z = z / scalar
        )
    }

    companion object {

        val ORIGIN = Point3D(
            x = 0f,
            y = 0f,
            z = 0f
        )

        private const val MIDPOINT_FACTOR = 0.5
    }
}