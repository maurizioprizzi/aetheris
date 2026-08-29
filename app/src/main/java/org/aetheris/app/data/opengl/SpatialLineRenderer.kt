package org.aetheris.app.data.opengl

import android.opengl.GLES30
import android.opengl.Matrix
import org.aetheris.app.domain.model.Point3D
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Renderiza pontos de ancoragem e uma linha entre duas posições
 * expressas no sistema de coordenadas do mundo ARCore.
 *
 * Todos os métodos devem ser chamados na thread OpenGL.
 */
class SpatialLineRenderer(
    private val requestedLineWidthPx: Float = 8f,
    private val requestedAnchorSizePx: Float = 20f,
    private val requestedSingleAnchorSizePx: Float = 24f
) {
    init {
        require(requestedLineWidthPx.isFinite() && requestedLineWidthPx > 0f)
        require(requestedAnchorSizePx.isFinite() && requestedAnchorSizePx > 0f)
        require(
            requestedSingleAnchorSizePx.isFinite() &&
                    requestedSingleAnchorSizePx > 0f
        )
    }

    private var programId = 0
    private var vertexArrayId = 0
    private var vertexBufferId = 0

    private var mvpMatrixUniform = -1
    private var colorUniform = -1
    private var pointSizeUniform = -1
    private var roundPointUniform = -1

    private var supportedLineWidth = 1f
    private var supportedAnchorSize = requestedAnchorSizePx
    private var supportedSingleAnchorSize = requestedSingleAnchorSizePx

    /*
     * Os Point3D recebidos já estão no espaço do mundo ARCore.
     * Portanto, MVP = Projection x View, sem uma Model matrix adicional.
     */
    private val mvpMatrix = FloatArray(MATRIX_SIZE)

    private val vertexArray = FloatArray(MAX_VERTEX_FLOAT_COUNT)

    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(MAX_VERTEX_FLOAT_COUNT * FLOAT_SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    fun createOnGlThread() {
        resetHandles()

        val vertexShader = compileShader(
            GLES30.GL_VERTEX_SHADER,
            VERTEX_SHADER_SOURCE
        )

        val fragmentShader = try {
            compileShader(
                GLES30.GL_FRAGMENT_SHADER,
                FRAGMENT_SHADER_SOURCE
            )
        } catch (exception: RuntimeException) {
            GLES30.glDeleteShader(vertexShader)
            throw exception
        }

        programId = linkProgram(
            vertexShader = vertexShader,
            fragmentShader = fragmentShader
        )

        resolveUniformLocations()
        createGeometryBuffers()
        querySupportedSizes()

        checkGlError("Criação do SpatialLineRenderer")
    }

    fun draw(
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        startPoint: Point3D?,
        endPoint: Point3D?
    ) {
        check(isCreated()) {
            "SpatialLineRenderer ainda não foi criado na thread OpenGL."
        }

        require(viewMatrix.size >= MATRIX_SIZE) {
            "viewMatrix deve conter pelo menos 16 valores."
        }

        require(projectionMatrix.size >= MATRIX_SIZE) {
            "projectionMatrix deve conter pelo menos 16 valores."
        }

        val firstPoint = startPoint ?: endPoint ?: return
        val hasLine = startPoint != null && endPoint != null

        val vertexCount = if (hasLine) {
            fillLineVertices(
                startPoint = requireNotNull(startPoint),
                endPoint = requireNotNull(endPoint)
            )
        } else {
            fillSinglePointVertex(firstPoint)
        }

        uploadVertices(vertexCount)

        Matrix.multiplyMM(
            mvpMatrix,
            0,
            projectionMatrix,
            0,
            viewMatrix,
            0
        )

        GLES30.glUseProgram(programId)

        GLES30.glUniformMatrix4fv(
            mvpMatrixUniform,
            1,
            false,
            mvpMatrix,
            0
        )

        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glDepthMask(true)

        GLES30.glBindVertexArray(vertexArrayId)

        if (hasLine) {
            drawLine()
            drawAnchors(
                vertexCount = 2,
                pointSize = supportedAnchorSize
            )
        } else {
            drawAnchors(
                vertexCount = 1,
                pointSize = supportedSingleAnchorSize
            )
        }

        GLES30.glBindVertexArray(0)
        GLES30.glUseProgram(0)

        checkGlError("Renderização da linha espacial")
    }

    fun destroyOnGlThread() {
        val tempHandle = IntArray(1)

        if (vertexBufferId != 0) {
            tempHandle[0] = vertexBufferId
            GLES30.glDeleteBuffers(1, tempHandle, 0)
        }

        if (vertexArrayId != 0) {
            tempHandle[0] = vertexArrayId
            GLES30.glDeleteVertexArrays(1, tempHandle, 0)
        }

        if (programId != 0) {
            GLES30.glDeleteProgram(programId)
        }

        resetHandles()
    }

    private fun drawLine() {
        GLES30.glLineWidth(supportedLineWidth)

        GLES30.glUniform4fv(
            colorUniform,
            1,
            LINE_COLOR,
            0
        )

        /*
         * Desativa o recorte circular, pois gl_PointCoord só possui
         * significado durante a renderização de GL_POINTS.
         */
        GLES30.glUniform1i(roundPointUniform, 0)

        GLES30.glDrawArrays(
            GLES30.GL_LINES,
            0,
            2
        )
    }

    private fun drawAnchors(
        vertexCount: Int,
        pointSize: Float
    ) {
        GLES30.glUniform4fv(
            colorUniform,
            1,
            ANCHOR_COLOR,
            0
        )

        GLES30.glUniform1f(
            pointSizeUniform,
            pointSize
        )

        GLES30.glUniform1i(roundPointUniform, 1)

        GLES30.glDrawArrays(
            GLES30.GL_POINTS,
            0,
            vertexCount
        )
    }

    private fun fillSinglePointVertex(
        point: Point3D
    ): Int {
        vertexArray[0] = point.x
        vertexArray[1] = point.y
        vertexArray[2] = point.z

        return 1
    }

    private fun fillLineVertices(
        startPoint: Point3D,
        endPoint: Point3D
    ): Int {
        vertexArray[0] = startPoint.x
        vertexArray[1] = startPoint.y
        vertexArray[2] = startPoint.z

        vertexArray[3] = endPoint.x
        vertexArray[4] = endPoint.y
        vertexArray[5] = endPoint.z

        return 2
    }

    private fun uploadVertices(vertexCount: Int) {
        val floatCount = vertexCount * COORDINATES_PER_VERTEX

        vertexBuffer.clear()
        vertexBuffer.put(vertexArray, 0, floatCount)
        vertexBuffer.flip()

        GLES30.glBindBuffer(
            GLES30.GL_ARRAY_BUFFER,
            vertexBufferId
        )

        GLES30.glBufferSubData(
            GLES30.GL_ARRAY_BUFFER,
            0,
            floatCount * FLOAT_SIZE_BYTES,
            vertexBuffer
        )

        GLES30.glBindBuffer(
            GLES30.GL_ARRAY_BUFFER,
            0
        )
    }

    private fun createGeometryBuffers() {
        val vertexArrays = IntArray(1)
        GLES30.glGenVertexArrays(1, vertexArrays, 0)
        vertexArrayId = vertexArrays[0]

        check(vertexArrayId != 0) {
            "Não foi possível criar o VAO da linha espacial."
        }

        val buffers = IntArray(1)
        GLES30.glGenBuffers(1, buffers, 0)
        vertexBufferId = buffers[0]

        check(vertexBufferId != 0) {
            "Não foi possível criar o VBO da linha espacial."
        }

        GLES30.glBindVertexArray(vertexArrayId)

        GLES30.glBindBuffer(
            GLES30.GL_ARRAY_BUFFER,
            vertexBufferId
        )

        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            MAX_VERTEX_FLOAT_COUNT * FLOAT_SIZE_BYTES,
            null,
            GLES30.GL_DYNAMIC_DRAW
        )

        GLES30.glEnableVertexAttribArray(
            POSITION_ATTRIBUTE_LOCATION
        )

        GLES30.glVertexAttribPointer(
            POSITION_ATTRIBUTE_LOCATION,
            COORDINATES_PER_VERTEX,
            GLES30.GL_FLOAT,
            false,
            0,
            0
        )

        GLES30.glBindBuffer(
            GLES30.GL_ARRAY_BUFFER,
            0
        )

        GLES30.glBindVertexArray(0)
    }

    private fun querySupportedSizes() {
        val lineWidthRange = FloatArray(2)

        GLES30.glGetFloatv(
            GLES30.GL_ALIASED_LINE_WIDTH_RANGE,
            lineWidthRange,
            0
        )

        supportedLineWidth = requestedLineWidthPx.coerceIn(
            lineWidthRange[0],
            lineWidthRange[1]
        )

        val pointSizeRange = FloatArray(2)

        GLES30.glGetFloatv(
            GLES30.GL_ALIASED_POINT_SIZE_RANGE,
            pointSizeRange,
            0
        )

        supportedAnchorSize = requestedAnchorSizePx.coerceIn(
            pointSizeRange[0],
            pointSizeRange[1]
        )

        supportedSingleAnchorSize =
            requestedSingleAnchorSizePx.coerceIn(
                pointSizeRange[0],
                pointSizeRange[1]
            )
    }

    private fun resolveUniformLocations() {
        mvpMatrixUniform = requireUniform("u_MvpMatrix")
        colorUniform = requireUniform("u_Color")
        pointSizeUniform = requireUniform("u_PointSize")
        roundPointUniform = requireUniform("u_RoundPoint")
    }

    private fun requireUniform(name: String): Int {
        val location = GLES30.glGetUniformLocation(
            programId,
            name
        )

        check(location >= 0) {
            "Uniform '$name' não encontrado no programa OpenGL."
        }

        return location
    }

    private fun compileShader(
        type: Int,
        source: String
    ): Int {
        val shader = GLES30.glCreateShader(type)

        check(shader != 0) {
            "Não foi possível criar o shader OpenGL."
        }

        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)

        val compilationStatus = IntArray(1)

        GLES30.glGetShaderiv(
            shader,
            GLES30.GL_COMPILE_STATUS,
            compilationStatus,
            0
        )

        if (compilationStatus[0] == GLES30.GL_FALSE) {
            val log = GLES30.glGetShaderInfoLog(shader)

            GLES30.glDeleteShader(shader)

            error("Falha ao compilar shader: $log")
        }

        return shader
    }

    private fun linkProgram(
        vertexShader: Int,
        fragmentShader: Int
    ): Int {
        val newProgram = GLES30.glCreateProgram()

        if (newProgram == 0) {
            GLES30.glDeleteShader(vertexShader)
            GLES30.glDeleteShader(fragmentShader)

            error("Não foi possível criar o programa OpenGL.")
        }

        GLES30.glAttachShader(newProgram, vertexShader)
        GLES30.glAttachShader(newProgram, fragmentShader)
        GLES30.glLinkProgram(newProgram)

        val linkStatus = IntArray(1)

        GLES30.glGetProgramiv(
            newProgram,
            GLES30.GL_LINK_STATUS,
            linkStatus,
            0
        )

        val programLog = if (
            linkStatus[0] == GLES30.GL_FALSE
        ) {
            GLES30.glGetProgramInfoLog(newProgram)
        } else {
            null
        }

        GLES30.glDetachShader(newProgram, vertexShader)
        GLES30.glDetachShader(newProgram, fragmentShader)
        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)

        if (programLog != null) {
            GLES30.glDeleteProgram(newProgram)
            error("Falha ao vincular programa OpenGL: $programLog")
        }

        return newProgram
    }

    private fun checkGlError(operation: String) {
        val errors = mutableListOf<String>()
        var errorCode = GLES30.glGetError()

        while (errorCode != GLES30.GL_NO_ERROR) {
            errors += "0x${errorCode.toString(16)}"
            errorCode = GLES30.glGetError()
        }

        check(errors.isEmpty()) {
            "$operation gerou erro OpenGL: ${errors.joinToString()}"
        }
    }

    private fun isCreated(): Boolean {
        return programId != 0 &&
                vertexArrayId != 0 &&
                vertexBufferId != 0
    }

    private fun resetHandles() {
        programId = 0
        vertexArrayId = 0
        vertexBufferId = 0

        mvpMatrixUniform = -1
        colorUniform = -1
        pointSizeUniform = -1
        roundPointUniform = -1

        supportedLineWidth = 1f
        supportedAnchorSize = requestedAnchorSizePx
        supportedSingleAnchorSize = requestedSingleAnchorSizePx
    }

    private companion object {
        const val MATRIX_SIZE = 16
        const val FLOAT_SIZE_BYTES = 4
        const val COORDINATES_PER_VERTEX = 3
        const val MAX_VERTEX_COUNT = 2

        const val MAX_VERTEX_FLOAT_COUNT =
            MAX_VERTEX_COUNT * COORDINATES_PER_VERTEX

        const val POSITION_ATTRIBUTE_LOCATION = 0

        val LINE_COLOR = floatArrayOf(
            0.345f,
            0.651f,
            1f,
            1f
        )

        val ANCHOR_COLOR = floatArrayOf(
            0.247f,
            0.725f,
            0.314f,
            1f
        )

        val VERTEX_SHADER_SOURCE = """
            #version 300 es

            layout(location = 0) in vec3 a_Position;

            uniform mat4 u_MvpMatrix;
            uniform float u_PointSize;

            void main() {
                gl_Position = u_MvpMatrix * vec4(a_Position, 1.0);
                gl_PointSize = u_PointSize;
            }
        """.trimIndent()

        val FRAGMENT_SHADER_SOURCE = """
            #version 300 es

            precision mediump float;

            uniform vec4 u_Color;
            uniform int u_RoundPoint;

            out vec4 o_FragColor;

            void main() {
                if (u_RoundPoint == 1) {
                    vec2 centeredCoordinate = gl_PointCoord - vec2(0.5);

                    if (dot(centeredCoordinate, centeredCoordinate) > 0.25) {
                        discard;
                    }
                }

                o_FragColor = u_Color;
            }
        """.trimIndent()
    }
}