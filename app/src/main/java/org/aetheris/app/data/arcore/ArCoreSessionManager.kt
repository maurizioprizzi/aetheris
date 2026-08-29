package org.aetheris.app.data.arcore

import android.content.Context
import androidx.annotation.MainThread
import com.google.ar.core.Config
import com.google.ar.core.Session

class ArCoreSessionManager(
    context: Context
) {
    private val applicationContext = context.applicationContext

    var session: Session? = null
        private set

    @Volatile
    var isRunning: Boolean = false
        private set

    var isDepthSupported: Boolean = false
        private set

    var isDepthEnabled: Boolean = false
        private set

    @MainThread
    fun resume(): Result<Session> {
        session?.takeIf { isRunning }?.let {
            return Result.success(it)
        }

        return runCatching {
            val currentSession = session ?: createSession().also {
                session = it
            }

            currentSession.resume()
            isRunning = true

            currentSession
        }.onFailure {
            isRunning = false
        }
    }

    @MainThread
    fun pause() {
        if (!isRunning) return

        try {
            session?.pause()
        } catch (_: RuntimeException) {
            // A sessão pode já estar pausada durante mudanças rápidas
            // no ciclo de vida da Activity.
        } finally {
            isRunning = false
        }
    }

    @MainThread
    fun destroy() {
        pause()

        try {
            session?.close()
        } catch (_: RuntimeException) {
            // Evita falhas durante a liberação do pipeline nativo.
        } finally {
            session = null
            isRunning = false
            isDepthEnabled = false
            isDepthSupported = false
        }
    }

    private fun createSession(): Session {
        val newSession = Session(applicationContext)

        return try {
            isDepthSupported = newSession.isDepthModeSupported(
                Config.DepthMode.AUTOMATIC
            )

            val config = Config(newSession).apply {
                planeFindingMode =
                    Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL

                lightEstimationMode =
                    Config.LightEstimationMode.AMBIENT_INTENSITY

                updateMode =
                    Config.UpdateMode.LATEST_CAMERA_IMAGE

                focusMode =
                    Config.FocusMode.AUTO

                /*
                 * O aparelho informa suporte ao Depth automático, mas o
                 * pipeline nativo está apresentando erro em ComputeDisparity.
                 *
                 * Planos, hit tests, âncoras e point cloud continuam
                 * funcionando sem a Depth API.
                 */
                depthMode = Config.DepthMode.DISABLED
            }

            newSession.configure(config)
            isDepthEnabled = false

            newSession
        } catch (exception: Exception) {
            try {
                newSession.close()
            } catch (_: RuntimeException) {
            }

            isDepthSupported = false
            isDepthEnabled = false

            throw exception
        }
    }
}