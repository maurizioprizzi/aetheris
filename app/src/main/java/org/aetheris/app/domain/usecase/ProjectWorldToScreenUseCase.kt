package org.aetheris.app.domain.usecase

import org.aetheris.app.domain.model.Point3D
import org.aetheris.app.domain.model.ScreenPoint2D

class ProjectWorldToScreenUseCase {

    /**
     * Projeta um ponto tridimensional do mundo para coordenadas 2D de tela.
     *
     * @param point Ponto 3D no espaço métrico físico.
     * @param viewMatrix Matriz View 4x4 (16 floats, column-major).
     * @param projectionMatrix Matriz Projection 4x4 (16 floats, column-major).
     * @param viewportWidth Largura da viewport em pixels.
     * @param viewportHeight Altura da viewport em pixels.
     * @return [ScreenPoint2D] contendo coordenadas de pixel e status de visibilidade no frustum.
     */
    operator fun invoke(
        point: Point3D,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        viewportWidth: Int,
        viewportHeight: Int
    ): ScreenPoint2D {
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            return ScreenPoint2D(0f, 0f, isVisible = false)
        }

        // 1. P_world -> P_eye (M_view * P_world)
        val eyeX = viewMatrix[0] * point.x + viewMatrix[4] * point.y + viewMatrix[8] * point.z + viewMatrix[12] * 1.0f
        val eyeY = viewMatrix[1] * point.x + viewMatrix[5] * point.y + viewMatrix[9] * point.z + viewMatrix[13] * 1.0f
        val eyeZ = viewMatrix[2] * point.x + viewMatrix[6] * point.y + viewMatrix[10] * point.z + viewMatrix[14] * 1.0f
        val eyeW = viewMatrix[3] * point.x + viewMatrix[7] * point.y + viewMatrix[11] * point.z + viewMatrix[15] * 1.0f

        // 2. P_eye -> P_clip (M_proj * P_eye)
        val clipX = projectionMatrix[0] * eyeX + projectionMatrix[4] * eyeY + projectionMatrix[8] * eyeZ + projectionMatrix[12] * eyeW
        val clipY = projectionMatrix[1] * eyeX + projectionMatrix[5] * eyeY + projectionMatrix[9] * eyeZ + projectionMatrix[13] * eyeW
        val clipZ = projectionMatrix[2] * eyeX + projectionMatrix[6] * eyeY + projectionMatrix[10] * eyeZ + projectionMatrix[14] * eyeW
        val clipW = projectionMatrix[3] * eyeX + projectionMatrix[7] * eyeY + projectionMatrix[11] * eyeZ + projectionMatrix[15] * eyeW

        // Se o ponto estiver atrás do plano focal da câmera (w <= 0), está fora de visão
        if (clipW <= 0.0001f) {
            return ScreenPoint2D(0f, 0f, isVisible = false)
        }

        // 3. Divisão de Perspectiva -> NDC (Normalized Device Coordinates: [-1, 1])
        val ndcX = clipX / clipW
        val ndcY = clipY / clipW
        val ndcZ = clipZ / clipW

        // Checagem de frustum métrico [-1, 1]
        val isInsideFrustum = ndcX in -1.0f..1.0f && ndcY in -1.0f..1.0f && ndcZ in -1.0f..1.0f

        // 4. NDC -> Coordenadas de Tela em Pixels
        val screenX = ((ndcX + 1.0f) * 0.5f) * viewportWidth
        val screenY = ((1.0f - ndcY) * 0.5f) * viewportHeight

        return ScreenPoint2D(
            x = screenX,
            y = screenY,
            isVisible = isInsideFrustum
        )
    }

    /**
     * Calcula o ponto médio entre dois pontos 3D e projeta para coordenadas de tela.
     */
    fun projectMidpoint(
        pointA: Point3D,
        pointB: Point3D,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        viewportWidth: Int,
        viewportHeight: Int
    ): ScreenPoint2D {
        val midPoint = Point3D(
            x = (pointA.x + pointB.x) * 0.5f,
            y = (pointA.y + pointB.y) * 0.5f,
            z = (pointA.z + pointB.z) * 0.5f
        )
        return invoke(midPoint, viewMatrix, projectionMatrix, viewportWidth, viewportHeight)
    }
}