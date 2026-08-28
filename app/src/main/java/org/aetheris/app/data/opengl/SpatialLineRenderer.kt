package org.aetheris.app.data.opengl

import android.opengl.GLES30
import android.opengl.Matrix
import org.aetheris.app.domain.model.Point3D
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Renderizador de baixo nível em OpenGL ES 3.0 responsável por projetar
 * os vetores métricos 3D e âncoras diretamente no espaço espacial da câmera.
 */
class SpatialLineRenderer {

    private val vertexShaderCode = """#version 300 es
        layout(location = 0) in vec4 a_Position;
        uniform mat4 u_MvpMatrix;
        uniform float u_PointSize;
        void main() {
            gl_Position = u_MvpMatrix * a_Position;
            gl_PointSize = u_PointSize;
        }
    """.trimIndent()

    private val fragmentShaderCode = """#version 300 es
        precision mediump float;
        uniform vec4 u_Color;
        out vec4 fragColor;
        void main() {
            fragColor = u_Color;
        }
    """.trimIndent()

    private var programId: Int = 0
    private var mvpMatrixUniform: Int = -1
    private var colorUniform: Int = -1
    private var pointSizeUniform: Int = -1

    // Matrizes de transformação reutilizáveis para evitar alocação de GC na renderização
    private val modelMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Buffer direto para armazenar as coordenadas dos vértices (Ponto A e Ponto B)
    // 2 vértices x 3 coordenadas (X, Y, Z) = 6 floats
    private val vertexArray = FloatArray(6)
    private val lineVertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(6 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    // Paleta de cores cromáticas no espaço sRGB
    private val lineColor = floatArrayOf(0.345f, 0.651f, 1.0f, 1.0f)     // Azul Cian (#58A6FF)
    private val anchorColor = floatArrayOf(0.247f, 0.725f, 0.314f, 1.0f)  // Verde (#3FB950)

    fun createOnGlThread() {
        val vShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
        val fShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)

        programId = GLES30.glCreateProgram().also {
            GLES30.glAttachShader(it, vShader)
            GLES30.glAttachShader(it, fShader)
            GLES30.glLinkProgram(it)
        }

        mvpMatrixUniform = GLES30.glGetUniformLocation(programId, "u_MvpMatrix")
        colorUniform = GLES30.glGetUniformLocation(programId, "u_Color")
        pointSizeUniform = GLES30.glGetUniformLocation(programId, "u_PointSize")

        Matrix.setIdentityM(modelMatrix, 0)
    }

    /**
     * Renderiza o vetor espacial no pipeline 3D com base nas matrizes de projeção e visualização.
     */
    fun draw(
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        startPoint: Point3D?,
        endPoint: Point3D?
    ) {
        if (startPoint == null && endPoint == null) return

        // M_clip = Projection * (View * Model)
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

        GLES30.glUseProgram(programId)
        GLES30.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0)

        // Configuração de Blending e Depth para renderização consistente sobre o feed da câmera
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        GLES30.glEnableVertexAttribArray(0)

        // Cenário 1: Apenas Ponto A fixado (Renderiza âncora isolada)
        if (startPoint != null && endPoint == null) {
            vertexArray[0] = startPoint.x
            vertexArray[1] = startPoint.y
            vertexArray[2] = startPoint.z

            lineVertexBuffer.position(0)
            lineVertexBuffer.put(vertexArray, 0, 3)
            lineVertexBuffer.position(0)

            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, lineVertexBuffer)
            GLES30.glUniform4fv(colorUniform, 1, anchorColor, 0)
            GLES30.glUniform1f(pointSizeUniform, 24.0f)
            GLES30.glDrawArrays(GLES30.GL_POINTS, 0, 1)
        }

        // Cenário 2: Pontos A e B fixados (Renderiza vetor 3D + âncoras de extremidade)
        if (startPoint != null && endPoint != null) {
            vertexArray[0] = startPoint.x
            vertexArray[1] = startPoint.y
            vertexArray[2] = startPoint.z
            vertexArray[3] = endPoint.x
            vertexArray[4] = endPoint.y
            vertexArray[5] = endPoint.z

            lineVertexBuffer.position(0)
            lineVertexBuffer.put(vertexArray)
            lineVertexBuffer.position(0)

            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, lineVertexBuffer)

            // 1. Desenha a linha métrica conectando os dois pontos
            GLES30.glLineWidth(8.0f)
            GLES30.glUniform4fv(colorUniform, 1, lineColor, 0)
            GLES30.glDrawArrays(GLES30.GL_LINES, 0, 2)

            // 2. Desenha os nós de ancoragem nos extremos da linha
            GLES30.glUniform4fv(colorUniform, 1, anchorColor, 0)
            GLES30.glUniform1f(pointSizeUniform, 20.0f)
            GLES30.glDrawArrays(GLES30.GL_POINTS, 0, 2)
        }

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisable(GLES30.GL_BLEND)
    }

    private fun loadShader(type: Int, code: String): Int {
        return GLES30.glCreateShader(type).also { shader ->
            GLES30.glShaderSource(shader, code)
            GLES30.glCompileShader(shader)
        }
    }
}