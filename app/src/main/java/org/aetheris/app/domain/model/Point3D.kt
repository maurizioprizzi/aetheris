package org.aetheris.app.domain.model

import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Ponto imutável e vetor euclidiano em um sistema cartesiano tridimensional.
 *
 * No espaço mundial do ARCore, as coordenadas são expressas em metros.
 * Os eixos seguem a convenção OpenGL: +X à direita, +Y para cima, +Z para trás.
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
     * Calcula a distância euclidiana até outro ponto com precisão Double.
     */
    fun distanceTo(other: Point3D): Float {
        val dx = x.toDouble() - other.x.toDouble()
        val dy = y.toDouble() - other.y.toDouble()
        val dz = z.toDouble() - other.z.toDouble()

        return hypot(
            hypot(dx, dy),
            dz
        ).toFloat()
    }

    /**
     * Ponto médio entre este ponto e outro.
     */
    fun midpointTo(other: Point3D): Point3D {
        return Point3D(
            x = x + (other.x - x) / 2f,
            y = y + (other.y - y) / 2f,
            z = z + (other.z - z) / 2f
        )
    }

    /**
     * Magnitude (norma euclidiana) do vetor a partir da origem.
     */
    val magnitude: Float
        get() = distanceTo(ORIGIN)

    /**
     * Retorna o vetor unitário com magnitude igual a 1.0.
     * Retorna ORIGIN se o vetor for nulo.
     */
    fun normalized(): Point3D {
        val mag = magnitude
        return if (mag > 0f) this / mag else ORIGIN
    }

    /**
     * Produto escalar (Dot Product) entre dois vetores: u . v
     */
    infix fun dot(other: Point3D): Float {
        return x * other.x + y * other.y + z * other.z
    }

    /**
     * Produto vetorial (Cross Product) entre dois vetores: u x v
     * Fundamental para cálculo de normais de superfície e área de polígonos 3D.
     */
    infix fun cross(other: Point3D): Point3D {
        return Point3D(
            x = y * other.z - z * other.y,
            y = z * other.x - x * other.z,
            z = x * other.y - y * other.x
        )
    }

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
        require(scalar.isFinite() && scalar != 0f) {
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
    }
}