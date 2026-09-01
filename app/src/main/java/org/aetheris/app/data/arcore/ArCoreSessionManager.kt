package org.aetheris.app.data.arcore

import android.content.Context
import androidx.annotation.MainThread
import com.google.ar.core.Config
import com.google.ar.core.Session

/**
 * Gerencia a criação e o ciclo de vida da sessão ARCore.
 *
 * As operações de resume, pause e destroy devem ser
 * executadas na thread principal.
 */
class ArCoreSessionManager(
    context: Context
) {

    private val applicationContext =
        context.applicationContext

    @Volatile
    var session: Session? = null
        private set

    @Volatile
    var isRunning: Boolean = false
        private set

    @Volatile
    var isDepthSupported: Boolean = false
        private set

    @Volatile
    var isDepthEnabled: Boolean = false
        private set

    @Volatile
    var isInstantPlacementEnabled: Boolean = false
        private set

    /**
     * Cria ou retoma a sessão ARCore.
     */
    @MainThread
    fun resume(): Result<Session> {
        val existingSession =
            session

        if (
            existingSession != null &&
            isRunning
        ) {
            return Result.success(
                existingSession
            )
        }

        return runCatching {
            val currentSession =
                existingSession
                    ?: createSession().also {
                        session = it
                    }

            currentSession.resume()

            /*
             * Esta escrita volátil acontece depois da atribuição
             * da sessão, garantindo sua visibilidade para a
             * thread de renderização.
             */
            isRunning = true

            currentSession
        }.onFailure {
            isRunning = false
        }
    }

    /**
     * Pausa a sessão sem destruir seus dados
     * de rastreamento.
     */
    @MainThread
    fun pause() {
        if (!isRunning) {
            return
        }

        try {
            session?.pause()
        } catch (_: RuntimeException) {
            /*
             * A sessão pode já estar pausada durante mudanças
             * rápidas no ciclo de vida da Activity.
             */
        } finally {
            isRunning = false
        }
    }

    /**
     * Pausa e encerra definitivamente a sessão ARCore.
     */
    @MainThread
    fun destroy() {
        pause()

        /*
         * Remove primeiro a referência compartilhada para impedir
         * que a thread OpenGL utilize uma sessão em encerramento.
         */
        val sessionToClose =
            session

        session = null
        isRunning = false
        isDepthEnabled = false
        isDepthSupported = false
        isInstantPlacementEnabled = false

        try {
            sessionToClose?.close()
        } catch (_: RuntimeException) {
            /*
             * Evita que uma falha durante a liberação do pipeline
             * nativo interrompa o ciclo de vida da Activity.
             */
        }
    }

    /**
     * Cria e configura uma nova sessão ARCore.
     */
    private fun createSession(): Session {
        val newSession =
            Session(applicationContext)

        return try {
            isDepthSupported =
                newSession.isDepthModeSupported(
                    Config.DepthMode.AUTOMATIC
                )

            val config =
                Config(newSession).apply {
                    planeFindingMode =
                        Config.PlaneFindingMode
                            .HORIZONTAL_AND_VERTICAL

                    lightEstimationMode =
                        Config.LightEstimationMode
                            .AMBIENT_INTENSITY

                    updateMode =
                        Config.UpdateMode
                            .LATEST_CAMERA_IMAGE

                    focusMode =
                        Config.FocusMode.AUTO

                    /*
                     * Permite criar um ponto espacial aproximado
                     * quando o ARCore ainda não construiu um plano
                     * ou não encontrou um ponto rastreável sob
                     * a coordenada consultada.
                     *
                     * O resultado aproximado ainda deverá ser
                     * tratado pelo ArCoreHitTestProcessor antes
                     * de ser aceito como medição definitiva.
                     */
                    instantPlacementMode =
                        Config.InstantPlacementMode
                            .LOCAL_Y_UP

                    /*
                     * Alguns aparelhos informam suporte ao modo
                     * Depth automático, mas apresentam falha no
                     * pipeline nativo durante ComputeDisparity.
                     *
                     * A Depth API permanece desativada enquanto
                     * planos, pontos rastreáveis, point cloud e
                     * Instant Placement continuam disponíveis.
                     */
                    depthMode =
                        Config.DepthMode.DISABLED
                }

            newSession.configure(config)

            isDepthEnabled =
                config.depthMode !=
                        Config.DepthMode.DISABLED

            isInstantPlacementEnabled =
                config.instantPlacementMode ==
                        Config.InstantPlacementMode
                            .LOCAL_Y_UP

            newSession
        } catch (exception: Exception) {
            try {
                newSession.close()
            } catch (_: RuntimeException) {
                /*
                 * Não existe outra ação de recuperação caso
                 * o encerramento da sessão incompleta falhe.
                 */
            }

            isDepthSupported = false
            isDepthEnabled = false
            isInstantPlacementEnabled = false

            throw exception
        }
    }
}