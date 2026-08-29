package org.aetheris.app.presentation.components

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.SessionPausedException
import org.aetheris.app.data.arcore.ArCoreSessionManager
import org.aetheris.app.data.opengl.BackgroundRenderer
import org.aetheris.app.data.opengl.SpatialLineRenderer
import org.aetheris.app.domain.model.Point3D
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Exibe o feed da câmera do ARCore e renderiza
 * a medição espacial.
 *
 * Regras de concorrência:
 *
 * - onFrameAvailable é executado na GL thread;
 * - o Frame deve ser consumido sincronamente;
 * - o Frame não deve ser armazenado na UI ou na ViewModel;
 * - onMatricesUpdated entrega cópias defensivas;
 * - onError é enviado para a main thread.
 */
@Composable
fun ArCameraFeed(
    sessionManager: ArCoreSessionManager,
    modifier: Modifier = Modifier,
    startPoint: Point3D? = null,
    endPoint: Point3D? = null,
    onSurfaceChanged: (
        width: Int,
        height: Int
    ) -> Unit = { _, _ -> },
    onMatricesUpdated: (
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray
    ) -> Unit = { _, _ -> },
    onFrameAvailable: (Frame) -> Unit = {},
    onError: (Throwable) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val composeView = LocalView.current

    val startPointState =
        rememberUpdatedState(startPoint)

    val endPointState =
        rememberUpdatedState(endPoint)

    val surfaceChangedCallback =
        rememberUpdatedState(onSurfaceChanged)

    val matricesUpdatedCallback =
        rememberUpdatedState(onMatricesUpdated)

    val frameAvailableCallback =
        rememberUpdatedState(onFrameAvailable)

    val errorCallback =
        rememberUpdatedState(onError)

    val backgroundRenderer = remember {
        BackgroundRenderer()
    }

    val spatialLineRenderer = remember {
        SpatialLineRenderer()
    }

    val mainHandler = remember {
        Handler(Looper.getMainLooper())
    }

    val surfaceViewReference = remember {
        AtomicReference<GLSurfaceView?>(null)
    }

    val displayRotation =
        composeView.display?.rotation
            ?: Surface.ROTATION_0

    val displayRotationReference = remember {
        AtomicInteger(displayRotation)
    }

    SideEffect {
        displayRotationReference.set(
            displayRotation
        )
    }

    val glRenderer = remember(
        sessionManager,
        backgroundRenderer,
        spatialLineRenderer
    ) {
        ArCameraGlRenderer(
            sessionManager = sessionManager,
            backgroundRenderer = backgroundRenderer,
            spatialLineRenderer = spatialLineRenderer,
            startPointProvider = {
                startPointState.value
            },
            endPointProvider = {
                endPointState.value
            },
            displayRotationProvider = {
                displayRotationReference.get()
            },
            onSurfaceChanged = { width, height ->
                surfaceChangedCallback.value(
                    width,
                    height
                )
            },
            onMatricesUpdated = { view, projection ->
                matricesUpdatedCallback.value(
                    view,
                    projection
                )
            },
            onFrameAvailable = { frame ->
                /*
                 * Executado sincronamente na GL thread.
                 * O frame permanece válido durante o callback.
                 */
                frameAvailableCallback.value(frame)
            },
            onError = { error ->
                mainHandler.post {
                    errorCallback.value(error)
                }
            }
        )
    }

    DisposableEffect(
        lifecycleOwner,
        sessionManager,
        glRenderer
    ) {
        var surfaceIsActive = false
        var destroyed = false

        fun resumeAr() {
            if (destroyed) {
                return
            }

            val surfaceView =
                surfaceViewReference.get()

            sessionManager.resume()
                .onSuccess {
                    /*
                     * Invalida a associação anterior da textura.
                     * Ela será refeita na GL thread antes do
                     * próximo Session.update().
                     */
                    surfaceView?.queueEvent {
                        glRenderer
                            .onSessionResumedOnGlThread()
                    }

                    surfaceView?.onResume()
                    surfaceIsActive = true
                }
                .onFailure { error ->
                    errorCallback.value(error)
                }
        }

        fun pauseAr() {
            if (destroyed) {
                return
            }

            /*
             * Interrompe a GL thread antes de pausar
             * a sessão, evitando Session.update()
             * durante Session.pause().
             */
            if (surfaceIsActive) {
                surfaceViewReference
                    .get()
                    ?.onPause()

                surfaceIsActive = false
            }

            sessionManager.pause()
        }

        fun destroyAr() {
            if (destroyed) {
                return
            }

            destroyed = true

            val surfaceView =
                surfaceViewReference.get()

            /*
             * Libera os recursos OpenGL enquanto
             * a GL thread ainda está disponível.
             */
            if (surfaceIsActive) {
                surfaceView?.queueEvent {
                    glRenderer.releaseOnGlThread()
                }

                surfaceView?.onPause()
                surfaceIsActive = false
            }

            sessionManager.destroy()
        }

        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        resumeAr()
                    }

                    Lifecycle.Event.ON_PAUSE -> {
                        pauseAr()
                    }

                    Lifecycle.Event.ON_DESTROY -> {
                        destroyAr()
                    }

                    else -> Unit
                }
            }

        lifecycleOwner.lifecycle.addObserver(
            observer
        )

        if (
            lifecycleOwner.lifecycle.currentState
                .isAtLeast(Lifecycle.State.RESUMED)
        ) {
            resumeAr()
        } else {
            surfaceViewReference
                .get()
                ?.onPause()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(
                observer
            )

            destroyAr()

            mainHandler.removeCallbacksAndMessages(
                null
            )

            surfaceViewReference.set(null)
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            GLSurfaceView(context).apply {
                setEGLContextClientVersion(
                    OPENGL_ES_VERSION
                )

                preserveEGLContextOnPause = true

                setRenderer(glRenderer)

                renderMode =
                    GLSurfaceView
                        .RENDERMODE_CONTINUOUSLY
            }.also { surfaceView ->
                surfaceViewReference.set(
                    surfaceView
                )
            }
        },
        update = { surfaceView ->
            surfaceViewReference.set(
                surfaceView
            )
        }
    )
}

private class ArCameraGlRenderer(
    private val sessionManager: ArCoreSessionManager,
    private val backgroundRenderer: BackgroundRenderer,
    private val spatialLineRenderer: SpatialLineRenderer,
    private val startPointProvider: () -> Point3D?,
    private val endPointProvider: () -> Point3D?,
    private val displayRotationProvider: () -> Int,
    private val onSurfaceChanged: (
        width: Int,
        height: Int
    ) -> Unit,
    private val onMatricesUpdated: (
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray
    ) -> Unit,
    private val onFrameAvailable: (Frame) -> Unit,
    private val onError: (Throwable) -> Unit
) : GLSurfaceView.Renderer {

    private val viewMatrix =
        FloatArray(MATRIX_SIZE)

    private val projectionMatrix =
        FloatArray(MATRIX_SIZE)

    private var viewportWidth = 0
    private var viewportHeight = 0

    private var lastDisplayRotation =
        INVALID_ROTATION

    private var configuredSession: Session? = null
    private var displayGeometryDirty = true
    private var renderingBlocked = false
    private var errorReported = false

    override fun onSurfaceCreated(
        gl: GL10?,
        config: EGLConfig?
    ) {
        configuredSession = null
        displayGeometryDirty = true
        renderingBlocked = false
        errorReported = false
        lastDisplayRotation = INVALID_ROTATION

        try {
            GLES30.glClearColor(
                0f,
                0f,
                0f,
                1f
            )

            GLES30.glEnable(
                GLES30.GL_DEPTH_TEST
            )

            GLES30.glDepthFunc(
                GLES30.GL_LEQUAL
            )

            backgroundRenderer.createOnGlThread()
            spatialLineRenderer.createOnGlThread()
        } catch (error: RuntimeException) {
            renderingBlocked = true
            reportErrorOnce(error)
        }
    }

    override fun onSurfaceChanged(
        gl: GL10?,
        width: Int,
        height: Int
    ) {
        if (width <= 0 || height <= 0) {
            return
        }

        viewportWidth = width
        viewportHeight = height
        displayGeometryDirty = true

        GLES30.glViewport(
            0,
            0,
            width,
            height
        )

        onSurfaceChanged(
            width,
            height
        )
    }

    override fun onDrawFrame(
        gl: GL10?
    ) {
        GLES30.glClear(
            GLES30.GL_COLOR_BUFFER_BIT or
                    GLES30.GL_DEPTH_BUFFER_BIT
        )

        if (renderingBlocked) {
            return
        }

        /*
         * isRunning é @Volatile. Sua leitura ocorre antes
         * do acesso à referência da sessão compartilhada.
         */
        if (!sessionManager.isRunning) {
            return
        }

        val session =
            sessionManager.session
                ?: return

        try {
            prepareSession(session)

            /*
             * Session.update() precisa ocorrer antes
             * dos consumidores do frame.
             */
            val frame = session.update()

            backgroundRenderer.draw(frame)

            if (frame.timestamp == 0L) {
                return
            }

            val camera = frame.camera

            camera.getViewMatrix(
                viewMatrix,
                0
            )

            camera.getProjectionMatrix(
                projectionMatrix,
                0,
                NEAR_CLIP_METERS,
                FAR_CLIP_METERS
            )

            /*
             * As matrizes internas são reutilizadas.
             * Por isso, o callback recebe cópias.
             */
            onMatricesUpdated(
                viewMatrix.copyOf(),
                projectionMatrix.copyOf()
            )

            spatialLineRenderer.draw(
                viewMatrix = viewMatrix,
                projectionMatrix =
                    projectionMatrix,
                startPoint =
                    startPointProvider(),
                endPoint =
                    endPointProvider()
            )

            /*
             * Deve ser o último consumidor do Frame
             * e terminar antes do próximo update().
             */
            onFrameAvailable(frame)

            errorReported = false
        } catch (_: SessionPausedException) {
            /*
             * Pode acontecer durante uma transição
             * normal do ciclo de vida.
             */
        } catch (
            error: CameraNotAvailableException
        ) {
            configuredSession = null
            renderingBlocked = true
            reportErrorOnce(error)
        } catch (error: RuntimeException) {
            renderingBlocked = true
            reportErrorOnce(error)
        }
    }

    /**
     * Deve ser chamado com queueEvent()
     * depois de Session.resume().
     */
    fun onSessionResumedOnGlThread() {
        configuredSession = null
        displayGeometryDirty = true
        renderingBlocked = false
        errorReported = false
        lastDisplayRotation = INVALID_ROTATION
    }

    /**
     * Deve ser chamado na GL thread.
     */
    fun releaseOnGlThread() {
        backgroundRenderer.destroyOnGlThread()
        spatialLineRenderer.destroyOnGlThread()

        configuredSession = null
        displayGeometryDirty = true
        renderingBlocked = true
        errorReported = false
        lastDisplayRotation = INVALID_ROTATION
    }

    private fun prepareSession(
        session: Session
    ) {
        if (configuredSession !== session) {
            session.setCameraTextureName(
                backgroundRenderer.textureId
            )

            configuredSession = session
            displayGeometryDirty = true
        }

        val displayRotation =
            displayRotationProvider()

        val geometryNeedsUpdate =
            displayGeometryDirty ||
                    displayRotation !=
                    lastDisplayRotation

        if (
            geometryNeedsUpdate &&
            viewportWidth > 0 &&
            viewportHeight > 0
        ) {
            session.setDisplayGeometry(
                displayRotation,
                viewportWidth,
                viewportHeight
            )

            lastDisplayRotation =
                displayRotation

            displayGeometryDirty = false
        }
    }

    private fun reportErrorOnce(
        error: Throwable
    ) {
        if (errorReported) {
            return
        }

        errorReported = true
        onError(error)
    }

    private companion object {
        const val MATRIX_SIZE = 16
        const val INVALID_ROTATION = -1

        const val NEAR_CLIP_METERS = 0.1f
        const val FAR_CLIP_METERS = 100f
    }
}

private const val OPENGL_ES_VERSION = 3