package org.aetheris.app.data.opengl

import android.opengl.GLES11Ext
import android.opengl.GLES30
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class BackgroundRenderer {

    var textureId: Int = -1
        private set

    private var program: Int = 0
    private var positionAttribute: Int = 0
    private var texCoordAttribute: Int = 0
    private var textureUniform: Int = 0

    // Coordenadas dos vértices cobrindo toda a tela (NDC)
    private val quadCoords = floatArrayOf(
        -1.0f, -1.0f,
        +1.0f, -1.0f,
        -1.0f, +1.0f,
        +1.0f, +1.0f
    )

    // Coordenadas UV normalizadas no espaço de visualização
    private val quadTexCoords = floatArrayOf(
        0.0f, 1.0f,
        1.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 0.0f
    )

    private val quadCoordBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(quadCoords.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(quadCoords)
            position(0)
        }

    private val quadTexCoordBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(quadTexCoords.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(quadTexCoords)
            position(0)
        }

    private val transformedTexCoordBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(quadTexCoords.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(quadTexCoords)
            position(0)
        }

    fun createOnGlThread() {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        textureId = textures[0]

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)

        val vertexShaderSource = """
            #version 300 es
            layout(location = 0) in vec4 a_Position;
            layout(location = 1) in vec2 a_TexCoord;
            out vec2 v_TexCoord;
            void main() {
                gl_Position = a_Position;
                v_TexCoord = a_TexCoord;
            }
        """.trimIndent()

        val fragmentShaderSource = """
            #version 300 es
            #extension GL_OES_EGL_image_external_essl3 : require
            precision mediump float;
            uniform samplerExternalOES u_Texture;
            in vec2 v_TexCoord;
            out vec4 o_FragColor;
            void main() {
                o_FragColor = texture(u_Texture, v_TexCoord);
            }
        """.trimIndent()

        val vShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderSource)
        val fShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderSource)

        program = GLES30.glCreateProgram().also { prog ->
            GLES30.glAttachShader(prog, vShader)
            GLES30.glAttachShader(prog, fShader)
            GLES30.glLinkProgram(prog)
        }

        positionAttribute = GLES30.glGetAttribLocation(program, "a_Position")
        texCoordAttribute = GLES30.glGetAttribLocation(program, "a_TexCoord")
        textureUniform = GLES30.glGetUniformLocation(program, "u_Texture")
    }

    fun draw(frame: Frame) {
        // Converte as coordenadas para o aspecto real e rotação do display
        frame.transformCoordinates2d(
            Coordinates2d.VIEW_NORMALIZED,
            quadTexCoordBuffer,
            Coordinates2d.TEXTURE_NORMALIZED,
            transformedTexCoordBuffer
        )

        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(false)

        GLES30.glUseProgram(program)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES30.glUniform1i(textureUniform, 0)

        quadCoordBuffer.position(0)
        GLES30.glEnableVertexAttribArray(positionAttribute)
        GLES30.glVertexAttribPointer(positionAttribute, 2, GLES30.GL_FLOAT, false, 0, quadCoordBuffer)

        transformedTexCoordBuffer.position(0)
        GLES30.glEnableVertexAttribArray(texCoordAttribute)
        GLES30.glVertexAttribPointer(texCoordAttribute, 2, GLES30.GL_FLOAT, false, 0, transformedTexCoordBuffer)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(positionAttribute)
        GLES30.glDisableVertexAttribArray(texCoordAttribute)

        GLES30.glDepthMask(true)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        return shader
    }
}