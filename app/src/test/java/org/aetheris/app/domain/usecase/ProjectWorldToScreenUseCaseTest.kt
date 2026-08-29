package org.aetheris.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import org.aetheris.app.domain.model.Point3D
import org.junit.Before
import org.junit.Test

class ProjectWorldToScreenUseCaseTest {

    private lateinit var useCase: ProjectWorldToScreenUseCase

    private val identityMatrix = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    // Matriz de projeção perspectiva simples com fov = 90 deg, near = 0.1, far = 100
    private val simpleProjectionMatrix = floatArrayOf(
        1f, 0f,  0f,     0f,
        0f, 1f,  0f,     0f,
        0f, 0f, -1.002f, -1f,
        0f, 0f, -0.2002f, 0f
    )

    @Before
    fun setUp() {
        useCase = ProjectWorldToScreenUseCase()
    }

    @Test
    fun `when point is directly ahead in center, should project to viewport center`() {
        val pointAhead = Point3D(0f, 0f, -1f)
        val width = 1080
        val height = 2400

        val result = useCase(
            point = pointAhead,
            viewMatrix = identityMatrix,
            projectionMatrix = simpleProjectionMatrix,
            viewportWidth = width,
            viewportHeight = height
        )

        assertThat(result.isVisible).isTrue()
        assertThat(result.x).isWithin(1.0f).of(540f)
        assertThat(result.y).isWithin(1.0f).of(1200f)
    }

    @Test
    fun `when point is behind the camera, should return isVisible false`() {
        val pointBehind = Point3D(0f, 0f, 2f)

        val result = useCase(
            point = pointBehind,
            viewMatrix = identityMatrix,
            projectionMatrix = simpleProjectionMatrix,
            viewportWidth = 1080,
            viewportHeight = 2400
        )

        assertThat(result.isVisible).isFalse()
    }

    @Test
    fun `when viewport is invalid, should return non-visible origin`() {
        val point = Point3D(0f, 0f, -1f)

        val result = useCase(
            point = point,
            viewMatrix = identityMatrix,
            projectionMatrix = simpleProjectionMatrix,
            viewportWidth = 0,
            viewportHeight = 0
        )

        assertThat(result.isVisible).isFalse()
        assertThat(result.x).isEqualTo(0f)
        assertThat(result.y).isEqualTo(0f)
    }

    @Test
    fun `projectMidpoint should project exact geometric center of vector AB`() {
        val pointA = Point3D(-0.5f, 0f, -2f)
        val pointB = Point3D(0.5f, 0f, -2f)

        val result = useCase.projectMidpoint(
            pointA = pointA,
            pointB = pointB,
            viewMatrix = identityMatrix,
            projectionMatrix = simpleProjectionMatrix,
            viewportWidth = 1080,
            viewportHeight = 1920
        )

        assertThat(result.isVisible).isTrue()
        assertThat(result.x).isWithin(1.0f).of(540f)
        assertThat(result.y).isWithin(1.0f).of(960f)
    }
}