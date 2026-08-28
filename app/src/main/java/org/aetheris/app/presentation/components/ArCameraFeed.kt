package org.aetheris.app.presentation.components

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.view.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ar.core.Frame
import org.aetheris.app.data.arcore.ArCoreSessionManager
import org.aetheris.app.data.opengl.BackgroundRenderer
import org.aetheris.app.data.opengl.SpatialLineRenderer
import org.aetheris.app.domain.model.Point3D
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@Composable
fun ArCameraFeed(
    sessionManager: ArCoreSessionManager,
    modifier: Modifier = Modifier,
    startPoint: Point3D? = null,
    endPoint: Point3D? = null,
    onSurfaceChanged: (width: Int, height: Int) -> Unit = { _, _ -> },
    onFrameAvailable: (Frame) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Garante leitura síncrona dos pontos pela GL Thread sem recriar o renderer
    val currentStartPoint by rememberUpdatedState(startPoint)
    val currentEndPoint by rememberUpdatedState(endPoint)

    val backgroundRenderer = remember { BackgroundRenderer() }
    val spatialLineRenderer = remember { SpatialLineRenderer() }

    val viewMatrix = remember { FloatArray(16) }
    val projectionMatrix = remember { FloatArray(16) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> sessionManager.resume()
                Lifecycle.Event.ON_PAUSE -> sessionManager.pause()
                Lifecycle.Event.ON_DESTROY -> sessionManager.destroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            GLSurfaceView(ctx).apply {
                preserveEGLContextOnPause = true
                setEGLContextClientVersion(3)
                setRenderer(object : GLSurfaceView.Renderer {
                    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
                        sessionManager.initializeSession()

                        // Compilação dos shaders na thread com contexto EGL ativo
                        backgroundRenderer.createOnGlThread()
                        spatialLineRenderer.createOnGlThread()

                        // Vincula a textura OES do BackgroundRenderer à sessão ARCore
                        sessionManager.session?.setCameraTextureNames(intArrayOf(backgroundRenderer.textureId))
                    }

                    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                        GLES30.glViewport(0, 0, width, height)
                        sessionManager.session?.setDisplayGeometry(
                            Surface.ROTATION_0,
                            width,
                            height
                        )
                        onSurfaceChanged(width, height)
                    }

                    override fun onDrawFrame(gl: GL10?) {
                        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

                        val session = sessionManager.session ?: return

                        // Garante que o ID da textura continue vinculado após pausas do app
                        session.setCameraTextureNames(intArrayOf(backgroundRenderer.textureId))

                        try {
                            val frame = session.update()
                            val camera = frame.camera

                            // 1. Renderiza o feed de vídeo da câmera via textura externa OES
                            backgroundRenderer.draw(frame)

                            // 2. Extrai matrizes de visualização e projeção da câmera ARCore
                            camera.getViewMatrix(viewMatrix, 0)
                            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)

                            // 3. Renderiza o vetor métrico 3D (linhas e âncoras) sobre o plano
                            spatialLineRenderer.draw(
                                viewMatrix = viewMatrix,
                                projectionMatrix = projectionMatrix,
                                startPoint = currentStartPoint,
                                endPoint = currentEndPoint
                            )

                            // 4. Notifica processadores de telemetria e raycasting
                            onFrameAvailable(frame)
                        } catch (e: Exception) {
                            // Trata variações de frame rate ou perda temporária de tracking
                        }
                    }
                })
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
        }
    )
}