package org.aetheris.app.presentation.components

import android.opengl.GLSurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.aetheris.app.data.arcore.ArCoreSessionManager
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@Composable
fun ArCameraFeed(
    sessionManager: ArCoreSessionManager,
    modifier: Modifier = Modifier,
    onFrameAvailable: (com.google.ar.core.Frame) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    sessionManager.resume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    sessionManager.pause()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    sessionManager.destroy()
                }
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
                        sessionManager.initializeSession()
                    }

                    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                        sessionManager.session?.setDisplayGeometry(
                            android.view.Surface.ROTATION_0,
                            width,
                            height
                        )
                    }

                    override fun onDrawFrame(gl: GL10?) {
                        val session = sessionManager.session ?: return
                        try {
                            val frame = session.update()
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