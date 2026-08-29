package org.aetheris.app.domain.usecase

import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.ScreenPoint2D

/**
 * Projeta pontos do espaço mundial 3D para a tela 2D
 * em coordenadas de pixels da viewport.
 *
 * A implementação é pura e não depende de android.opengl.Matrix,
 * mantendo o caso de uso independente do Android e testável na JVM.
 */
class ProjectWorldToScreenUseCase {

    operator fun invoke(
        point: Point3D,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        viewportWidth: Int,
        viewportHeight: Int
    ): ScreenPoint2D {
        requireValidMatrix(
            matrix = viewMatrix,
            name = "viewMatrix"
        )

        requireValidMatrix(
            matrix = projectionMatrix,
            name = "projectionMatrix"
        )

        if (viewportWidth <= 0 || viewportHeight <= 0) {
            return ScreenPoint2D.NOT_VISIBLE
        }

        /*
         * 1. Mundo -> espaço da câmera (Eye Space).
         *
         * As matrizes OpenGL utilizam organização column-major.
         */
        val eyeX =
            viewMatrix[0] * point.x +
                    viewMatrix[4] * point.y +
                    viewMatrix[8] * point.z +
                    viewMatrix[12]

        val eyeY =
            viewMatrix[1] * point.x +
                    viewMatrix[5] * point.y +
                    viewMatrix[9] * point.z +
                    viewMatrix[13]

        val eyeZ =
            viewMatrix[2] * point.x +
                    viewMatrix[6] * point.y +
                    viewMatrix[10] * point.z +
                    viewMatrix[14]

        val eyeW =
            viewMatrix[3] * point.x +
                    viewMatrix[7] * point.y +
                    viewMatrix[11] * point.z +
                    viewMatrix[15]

        /*
         * 2. Espaço da câmera -> espaço de recorte
         * homogêneo (Clip Space).
         */
        val clipX =
            projectionMatrix[0] * eyeX +
                    projectionMatrix[4] * eyeY +
                    projectionMatrix[8] * eyeZ +
                    projectionMatrix[12] * eyeW

        val clipY =
            projectionMatrix[1] * eyeX +
                    projectionMatrix[5] * eyeY +
                    projectionMatrix[9] * eyeZ +
                    projectionMatrix[13] * eyeW

        val clipZ =
            projectionMatrix[2] * eyeX +
                    projectionMatrix[6] * eyeY +
                    projectionMatrix[10] * eyeZ +
                    projectionMatrix[14] * eyeW

        val clipW =
            projectionMatrix[3] * eyeX +
                    projectionMatrix[7] * eyeY +
                    projectionMatrix[11] * eyeZ +
                    projectionMatrix[15] * eyeW

        if (
            !clipX.isFinite() ||
            !clipY.isFinite() ||
            !clipZ.isFinite() ||
            !clipW.isFinite()
        ) {
            return ScreenPoint2D.NOT_VISIBLE
        }

        /*
         * Um W não positivo indica que o ponto está atrás
         * do plano da câmera.
         *
         * Um valor positivo excessivamente pequeno causaria
         * instabilidade numérica durante a divisão.
         */
        if (clipW <= MINIMUM_CLIP_W) {
            return ScreenPoint2D.NOT_VISIBLE
        }

        /*
         * 3. Verificação dos limites do frustum em Clip Space:
         *
         * -W <= X <= W
         * -W <= Y <= W
         * -W <= Z <= W
         */
        val isInsideFrustum =
            clipX >= -clipW &&
                    clipX <= clipW &&
                    clipY >= -clipW &&
                    clipY <= clipW &&
                    clipZ >= -clipW &&
                    clipZ <= clipW

        /*
         * 4. Divisão de perspectiva para Normalized
         * Device Coordinates (NDC), no intervalo [-1, 1].
         */
        val ndcX = clipX / clipW
        val ndcY = clipY / clipW

        if (!ndcX.isFinite() || !ndcY.isFinite()) {
            return ScreenPoint2D.NOT_VISIBLE
        }

        /*
         * 5. Mapeamento da viewport:
         *
         * NDC [-1, 1] -> pixels [0, dimensão - 1].
         *
         * O eixo Y é invertido porque no OpenGL ele cresce
         * para cima, enquanto na tela Android cresce para baixo.
         */
        val maximumX =
            (viewportWidth - 1)
                .coerceAtLeast(0)
                .toFloat()

        val maximumY =
            (viewportHeight - 1)
                .coerceAtLeast(0)
                .toFloat()

        val screenX =
            (ndcX + 1f) *
                    NDC_TO_VIEWPORT_SCALE *
                    maximumX

        val screenY =
            (1f - ndcY) *
                    NDC_TO_VIEWPORT_SCALE *
                    maximumY

        /*
         * Protege o modelo contra overflow durante a conversão
         * de NDC para pixels.
         */
        if (!screenX.isFinite() || !screenY.isFinite()) {
            return ScreenPoint2D.NOT_VISIBLE
        }

        return ScreenPoint2D(
            x = screenX,
            y = screenY,
            isVisible = isInsideFrustum
        )
    }

    /**
     * Projeta o ponto médio entre duas coordenadas 3D.
     *
     * É útil para posicionar indicadores de distância
     * sobre a linha desenhada.
     */
    fun projectMidpoint(
        pointA: Point3D,
        pointB: Point3D,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        viewportWidth: Int,
        viewportHeight: Int
    ): ScreenPoint2D {
        return invoke(
            point = pointA.midpointTo(pointB),
            viewMatrix = viewMatrix,
            projectionMatrix = projectionMatrix,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight
        )
    }

    private fun requireValidMatrix(
        matrix: FloatArray,
        name: String
    ) {
        require(matrix.size >= MATRIX_ELEMENT_COUNT) {
            "$name deve conter pelo menos " +
                    "$MATRIX_ELEMENT_COUNT valores."
        }

        for (index in 0 until MATRIX_ELEMENT_COUNT) {
            require(matrix[index].isFinite()) {
                "$name deve conter apenas valores finitos. " +
                        "Elemento no índice $index é inválido."
            }
        }
    }

    private companion object {
        const val MATRIX_ELEMENT_COUNT = 16
        const val MINIMUM_CLIP_W = 0.0001f
        const val NDC_TO_VIEWPORT_SCALE = 0.5f
    }
}