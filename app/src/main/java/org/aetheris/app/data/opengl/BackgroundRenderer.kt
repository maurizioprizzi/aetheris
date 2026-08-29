package org.aetheris.app.data.opengl

import android.opengl.GLES11Ext
import android.opengl.GLES30
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Renderiza a imagem da câmera do ARCore
 * como plano de fundo da cena OpenGL.
 *
 * Todos os métodos devem ser chamados na thread OpenGL.
 */
class BackgroundRenderer {

    var textureId: Int = INVALID_TEXTURE_ID
        private set

    private var programId = 0
    private var vertexArrayId = 0
    private var positionBufferId = 0
    private var textureCoordinateBufferId = 0

    private var textureUniformLocation =
        INVALID_UNIFORM_LOCATION

    private var textureCoordinatesInitialized =
        false

    private val quadPositionBuffer: FloatBuffer =
        createFloatBuffer(
            values = QUAD_COORDINATES
        )

    private val transformedTextureCoordinateBuffer:
            FloatBuffer =
        createEmptyFloatBuffer(
            floatCount = QUAD_COORDINATES.size
        )

    /**
     * Cria a textura externa, o programa e os buffers.
     *
     * Deve ser chamado na thread OpenGL, normalmente
     * durante onSurfaceCreated().
     */
    fun createOnGlThread() {
        check(!isCreated()) {
            "BackgroundRenderer já foi criado."
        }

        resetHandles()

        try {
            textureId = createCameraTexture()

            programId = createProgram(
                vertexShaderSource =
                    VERTEX_SHADER_SOURCE,
                fragmentShaderSource =
                    FRAGMENT_SHADER_SOURCE
            )

            textureUniformLocation =
                requireUniform(
                    name = TEXTURE_UNIFORM_NAME
                )

            createGeometryBuffers()

            textureCoordinatesInitialized = false

            checkGlError(
                operation =
                    "Criação do BackgroundRenderer"
            )
        } catch (exception: RuntimeException) {
            destroyOnGlThread()
            throw exception
        }
    }

    /**
     * Desenha a imagem da câmera.
     *
     * Deve ser chamado antes da renderização
     * dos objetos virtuais da cena.
     */
    fun draw(frame: Frame) {
        check(isCreated()) {
            "BackgroundRenderer ainda não foi criado na thread OpenGL."
        }

        updateTextureCoordinatesIfNecessary(
            frame = frame
        )

        /*
         * Um timestamp igual a zero indica que a câmera
         * ainda não produziu sua primeira imagem.
         */
        if (frame.timestamp == 0L) {
            return
        }

        GLES30.glDisable(
            GLES30.GL_DEPTH_TEST
        )

        GLES30.glDepthMask(false)

        GLES30.glUseProgram(programId)

        try {
            GLES30.glActiveTexture(
                GLES30.GL_TEXTURE0
            )

            GLES30.glBindTexture(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                textureId
            )

            GLES30.glUniform1i(
                textureUniformLocation,
                TEXTURE_UNIT_INDEX
            )

            GLES30.glBindVertexArray(
                vertexArrayId
            )

            GLES30.glDrawArrays(
                GLES30.GL_TRIANGLE_STRIP,
                0,
                VERTEX_COUNT
            )
        } finally {
            GLES30.glBindVertexArray(0)

            GLES30.glBindTexture(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                0
            )

            GLES30.glUseProgram(0)

            /*
             * Restaura o estado esperado para renderizar
             * a geometria tridimensional após o fundo.
             */
            GLES30.glDepthMask(true)

            GLES30.glEnable(
                GLES30.GL_DEPTH_TEST
            )
        }

        checkGlError(
            operation =
                "Renderização do fundo"
        )
    }

    /**
     * Libera os recursos pertencentes ao contexto OpenGL.
     */
    fun destroyOnGlThread() {
        val handle = IntArray(1)

        if (positionBufferId != 0) {
            handle[0] = positionBufferId

            GLES30.glDeleteBuffers(
                1,
                handle,
                0
            )
        }

        if (textureCoordinateBufferId != 0) {
            handle[0] =
                textureCoordinateBufferId

            GLES30.glDeleteBuffers(
                1,
                handle,
                0
            )
        }

        if (vertexArrayId != 0) {
            handle[0] = vertexArrayId

            GLES30.glDeleteVertexArrays(
                1,
                handle,
                0
            )
        }

        if (programId != 0) {
            GLES30.glDeleteProgram(
                programId
            )
        }

        if (textureId != INVALID_TEXTURE_ID) {
            handle[0] = textureId

            GLES30.glDeleteTextures(
                1,
                handle,
                0
            )
        }

        resetHandles()
    }

    private fun updateTextureCoordinatesIfNecessary(
        frame: Frame
    ) {
        if (
            textureCoordinatesInitialized &&
            !frame.hasDisplayGeometryChanged()
        ) {
            return
        }

        quadPositionBuffer.position(0)

        transformedTextureCoordinateBuffer
            .position(0)

        frame.transformCoordinates2d(
            Coordinates2d
                .OPENGL_NORMALIZED_DEVICE_COORDINATES,
            quadPositionBuffer,
            Coordinates2d.TEXTURE_NORMALIZED,
            transformedTextureCoordinateBuffer
        )

        transformedTextureCoordinateBuffer
            .position(0)

        GLES30.glBindBuffer(
            GLES30.GL_ARRAY_BUFFER,
            textureCoordinateBufferId
        )

        try {
            GLES30.glBufferSubData(
                GLES30.GL_ARRAY_BUFFER,
                0,
                transformedTextureCoordinateBuffer
                    .capacity() * FLOAT_SIZE_BYTES,
                transformedTextureCoordinateBuffer
            )
        } finally {
            GLES30.glBindBuffer(
                GLES30.GL_ARRAY_BUFFER,
                0
            )
        }

        textureCoordinatesInitialized = true
    }

    private fun createGeometryBuffers() {
        val vertexArrays = IntArray(1)

        GLES30.glGenVertexArrays(
            1,
            vertexArrays,
            0
        )

        vertexArrayId = vertexArrays[0]

        check(vertexArrayId != 0) {
            "Não foi possível criar o VAO do fundo."
        }

        val buffers = IntArray(2)

        GLES30.glGenBuffers(
            2,
            buffers,
            0
        )

        positionBufferId = buffers[0]

        textureCoordinateBufferId =
            buffers[1]

        check(
            positionBufferId != 0 &&
                    textureCoordinateBufferId != 0
        ) {
            "Não foi possível criar os buffers do fundo."
        }

        GLES30.glBindVertexArray(
            vertexArrayId
        )

        try {
            configurePositionBuffer()
            configureTextureCoordinateBuffer()
        } finally {
            GLES30.glBindBuffer(
                GLES30.GL_ARRAY_BUFFER,
                0
            )

            GLES30.glBindVertexArray(0)
        }
    }

    private fun configurePositionBuffer() {
        quadPositionBuffer.position(0)

        GLES30.glBindBuffer(
            GLES30.GL_ARRAY_BUFFER,
            positionBufferId
        )

        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            quadPositionBuffer.capacity() *
                    FLOAT_SIZE_BYTES,
            quadPositionBuffer,
            GLES30.GL_STATIC_DRAW
        )

        GLES30.glEnableVertexAttribArray(
            POSITION_ATTRIBUTE_LOCATION
        )

        GLES30.glVertexAttribPointer(
            POSITION_ATTRIBUTE_LOCATION,
            COORDINATES_PER_VERTEX,
            GLES30.GL_FLOAT,
            false,
            VERTEX_STRIDE_BYTES,
            0
        )
    }

    private fun configureTextureCoordinateBuffer() {
        GLES30.glBindBuffer(
            GLES30.GL_ARRAY_BUFFER,
            textureCoordinateBufferId
        )

        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            transformedTextureCoordinateBuffer
                .capacity() * FLOAT_SIZE_BYTES,
            null,
            GLES30.GL_DYNAMIC_DRAW
        )

        GLES30.glEnableVertexAttribArray(
            TEXTURE_COORDINATE_ATTRIBUTE_LOCATION
        )

        GLES30.glVertexAttribPointer(
            TEXTURE_COORDINATE_ATTRIBUTE_LOCATION,
            TEXTURE_COORDINATES_PER_VERTEX,
            GLES30.GL_FLOAT,
            false,
            VERTEX_STRIDE_BYTES,
            0
        )
    }

    private fun createCameraTexture(): Int {
        val textures = IntArray(1)

        GLES30.glGenTextures(
            1,
            textures,
            0
        )

        val generatedTextureId =
            textures[0]

        check(generatedTextureId != 0) {
            "Não foi possível criar a textura externa da câmera."
        }

        GLES30.glBindTexture(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            generatedTextureId
        )

        try {
            GLES30.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES30.GL_TEXTURE_WRAP_S,
                GLES30.GL_CLAMP_TO_EDGE
            )

            GLES30.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES30.GL_TEXTURE_WRAP_T,
                GLES30.GL_CLAMP_TO_EDGE
            )

            GLES30.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_LINEAR
            )

            GLES30.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_LINEAR
            )
        } finally {
            GLES30.glBindTexture(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                0
            )
        }

        return generatedTextureId
    }

    private fun createProgram(
        vertexShaderSource: String,
        fragmentShaderSource: String
    ): Int {
        val vertexShader = compileShader(
            type = GLES30.GL_VERTEX_SHADER,
            source = vertexShaderSource
        )

        val fragmentShader = try {
            compileShader(
                type = GLES30.GL_FRAGMENT_SHADER,
                source = fragmentShaderSource
            )
        } catch (exception: RuntimeException) {
            GLES30.glDeleteShader(
                vertexShader
            )

            throw exception
        }

        val createdProgram =
            GLES30.glCreateProgram()

        if (createdProgram == 0) {
            GLES30.glDeleteShader(
                vertexShader
            )

            GLES30.glDeleteShader(
                fragmentShader
            )

            error(
                "Não foi possível criar o programa OpenGL."
            )
        }

        GLES30.glAttachShader(
            createdProgram,
            vertexShader
        )

        GLES30.glAttachShader(
            createdProgram,
            fragmentShader
        )

        GLES30.glLinkProgram(
            createdProgram
        )

        val linkStatus = IntArray(1)

        GLES30.glGetProgramiv(
            createdProgram,
            GLES30.GL_LINK_STATUS,
            linkStatus,
            0
        )

        val programLog =
            if (
                linkStatus[0] ==
                GLES30.GL_FALSE
            ) {
                GLES30.glGetProgramInfoLog(
                    createdProgram
                )
            } else {
                null
            }

        GLES30.glDetachShader(
            createdProgram,
            vertexShader
        )

        GLES30.glDetachShader(
            createdProgram,
            fragmentShader
        )

        GLES30.glDeleteShader(
            vertexShader
        )

        GLES30.glDeleteShader(
            fragmentShader
        )

        if (programLog != null) {
            GLES30.glDeleteProgram(
                createdProgram
            )

            error(
                "Falha ao vincular o programa OpenGL: $programLog"
            )
        }

        return createdProgram
    }

    private fun compileShader(
        type: Int,
        source: String
    ): Int {
        val shader =
            GLES30.glCreateShader(type)

        check(shader != 0) {
            "Não foi possível criar o shader OpenGL."
        }

        GLES30.glShaderSource(
            shader,
            source
        )

        GLES30.glCompileShader(shader)

        val compilationStatus =
            IntArray(1)

        GLES30.glGetShaderiv(
            shader,
            GLES30.GL_COMPILE_STATUS,
            compilationStatus,
            0
        )

        if (
            compilationStatus[0] ==
            GLES30.GL_FALSE
        ) {
            val log =
                GLES30.glGetShaderInfoLog(
                    shader
                )

            GLES30.glDeleteShader(
                shader
            )

            error(
                "Falha ao compilar shader OpenGL: $log"
            )
        }

        return shader
    }

    private fun requireUniform(
        name: String
    ): Int {
        val location =
            GLES30.glGetUniformLocation(
                programId,
                name
            )

        check(location >= 0) {
            "Uniform '$name' não encontrado."
        }

        return location
    }

    private fun checkGlError(
        operation: String
    ) {
        val errors =
            mutableListOf<String>()

        var errorCode =
            GLES30.glGetError()

        while (
            errorCode != GLES30.GL_NO_ERROR
        ) {
            errors +=
                "0x${errorCode.toString(16)}"

            errorCode =
                GLES30.glGetError()
        }

        check(errors.isEmpty()) {
            "$operation gerou erro OpenGL: " +
                    errors.joinToString()
        }
    }

    private fun isCreated(): Boolean {
        return textureId != INVALID_TEXTURE_ID &&
                programId != 0 &&
                vertexArrayId != 0 &&
                positionBufferId != 0 &&
                textureCoordinateBufferId != 0 &&
                textureUniformLocation >= 0
    }

    private fun resetHandles() {
        textureId = INVALID_TEXTURE_ID
        programId = 0
        vertexArrayId = 0
        positionBufferId = 0
        textureCoordinateBufferId = 0

        textureUniformLocation =
            INVALID_UNIFORM_LOCATION

        textureCoordinatesInitialized = false
    }

    private fun createFloatBuffer(
        values: FloatArray
    ): FloatBuffer {
        return ByteBuffer
            .allocateDirect(
                values.size *
                        FLOAT_SIZE_BYTES
            )
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(values)
                position(0)
            }
    }

    private fun createEmptyFloatBuffer(
        floatCount: Int
    ): FloatBuffer {
        return ByteBuffer
            .allocateDirect(
                floatCount *
                        FLOAT_SIZE_BYTES
            )
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    }

    private companion object {
        const val INVALID_TEXTURE_ID = -1
        const val INVALID_UNIFORM_LOCATION = -1

        const val FLOAT_SIZE_BYTES = 4
        const val VERTEX_COUNT = 4
        const val COORDINATES_PER_VERTEX = 2
        const val TEXTURE_COORDINATES_PER_VERTEX = 2

        const val POSITION_ATTRIBUTE_LOCATION = 0

        const val TEXTURE_COORDINATE_ATTRIBUTE_LOCATION =
            1

        const val VERTEX_STRIDE_BYTES = 0
        const val TEXTURE_UNIT_INDEX = 0

        const val TEXTURE_UNIFORM_NAME =
            "u_Texture"

        /*
         * OpenGL Normalized Device Coordinates:
         * valores no intervalo entre -1 e 1.
         *
         * A ordem é apropriada para GL_TRIANGLE_STRIP.
         */
        val QUAD_COORDINATES = floatArrayOf(
            -1f, -1f,
            +1f, -1f,
            -1f, +1f,
            +1f, +1f
        )

        val VERTEX_SHADER_SOURCE = """
            #version 300 es

            layout(location = 0) in vec2 a_Position;
            layout(location = 1) in vec2 a_TexCoord;

            out vec2 v_TexCoord;

            void main() {
                gl_Position =
                    vec4(a_Position, 0.0, 1.0);

                v_TexCoord = a_TexCoord;
            }
        """.trimIndent()

        val FRAGMENT_SHADER_SOURCE = """
            #version 300 es

            #extension GL_OES_EGL_image_external_essl3 : require

            precision mediump float;

            uniform samplerExternalOES u_Texture;

            in vec2 v_TexCoord;
            out vec4 o_FragColor;

            void main() {
                o_FragColor =
                    texture(
                        u_Texture,
                        v_TexCoord
                    );
            }
        """.trimIndent()
    }
}