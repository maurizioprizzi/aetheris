package org.aetheris.app.data.arcore

import android.content.Context
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException

class ArCoreSessionManager(
    private val context: Context
) {
    var session: Session? = null
        private set

    fun initializeSession(): Result<Session> {
        return try {
            val arSession = Session(context).apply {
                val config = Config(this).apply {
                    focusMode = Config.FocusMode.AUTO
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL

                    if (isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        depthMode = Config.DepthMode.AUTOMATIC
                    } else {
                        depthMode = Config.DepthMode.DISABLED
                    }
                }
                configure(config)
            }
            this.session = arSession
            Result.success(arSession)
        } catch (e: UnavailableArcoreNotInstalledException) {
            Result.failure(Exception("Google Play Services for AR não instalado."))
        } catch (e: UnavailableDeviceNotCompatibleException) {
            Result.failure(Exception("Dispositivo incompatível com ARCore."))
        } catch (e: UnavailableApkTooOldException) {
            Result.failure(Exception("Versão do ARCore desatualizada."))
        } catch (e: UnavailableSdkTooOldException) {
            Result.failure(Exception("SDK Android incompatível com esta versão do ARCore."))
        } catch (e: Exception) {
            Result.failure(Exception("Falha ao inicializar sessão AR: ${e.localizedMessage}"))
        }
    }

    fun resume() {
        session?.resume()
    }

    fun pause() {
        session?.pause()
    }

    fun destroy() {
        session?.close()
        session = null
    }
}