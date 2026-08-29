package org.aetheris.app.presentation.components

import android.content.Context
import android.hardware.display.DisplayManager
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.view.Display
import android.view.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.core.exceptions.SessionPausedException
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

    val currentStartPoint by rememberUpdatedState(startPoint)
    val currentEndPoint by rememberUpdatedState(endPoint)

    val backgroundRenderer = remember { BackgroundRenderer() }
    val spatialLineRenderer = remember { SpatialLineRenderer() }

    val viewMatrix = remember { FloatArray(16) }
    val projectionMatrix = remember { FloatArray(16) }

    var glSurfaceViewRef by remember { mutableStateOf<GLSurfaceView?>(null) }
    var isTextureBound by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    sessionManager.resume()
                    glSurfaceViewRef?.onResume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    glSurfaceViewRef?.onPause()
                    sessionManager.pause()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    glSurfaceViewRef?.onPause()
                    sessionManager.destroy()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            sessionManager.resume()
            glSurfaceViewRef?.onResume()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glSurfaceViewRef?.onPause()
            sessionManager.pause()
            sessionManager.destroy()
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
                        isTextureBound = false

                        backgroundRenderer.createOnGlThread()
                        spatialLineRenderer.createOnGlThread()
                    }

                    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                        GLES30.glViewport(0, 0, width, height)

                        val displayManager = ctx.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                        val rotation = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0

                        sessionManager.session?.setDisplayGeometry(rotation, width, height)
                        onSurfaceChanged(width, height)
                    }

                    override fun onDrawFrame(gl: GL10?) {
                        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

                        val session = sessionManager.session ?: return
                        if (!sessionManager.isRunning) return

                        // Vincula o ID da textura OES ao ARCore assim que a sessão estiver pronta
                        if (!isTextureBound && backgroundRenderer.textureId != -1) {
                            session.setCameraTextureNames(intArrayOf(backgroundRenderer.textureId))
                            isTextureBound = true
                        }

                        try {
                            val frame = session.update()
                            val camera = frame.camera

                            // 1. Renderiza o vídeo real da câmera
                            backgroundRenderer.draw(frame)

                            // 2. Extrai as matrizes de projeção do mundo 3D
                            camera.getViewMatrix(viewMatrix, 0)
                            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)

                            // 3. Renderiza as linhas e pontos 3D
                            spatialLineRenderer.draw(
                                viewMatrix = viewMatrix,
                                projectionMatrix = projectionMatrix,
                                startPoint = currentStartPoint,
                                endPoint = currentEndPoint
                            )

                            // 4. Notifica a UI com o frame atualizado
                            onFrameAvailable(frame)
                        } catch (e: SessionPausedException) {
                            // Ignora frames durante transições de ciclo de vida
                        } catch (e: CameraNotAvailableException) {
                            // Câmera temporariamente ocupada
                        } catch (e: NotYetAvailableException) {
                            // Frame intermediário
                        } catch (e: Throwable) {
                            // Protege o loop gráfico contra quedas
                        }
                    }
                })
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }.also { glSurfaceViewRef = it }
        }
    )
}