package org.aetheris.app.domain.model

/**
 * Estado consolidado de rastreamento espacial
 * apresentado ao domínio e à interface.
 */
enum class TrackingStatus {
    /**
     * O ARCore ainda está inicializando o mapa de pontos
     * e estabelecendo o rastreamento inicial.
     */
    INITIALIZING,

    /**
     * Rastreamento espacial 6-DoF ativo.
     */
    TRACKING,

    /**
     * O dispositivo está sendo movimentado rápido demais
     * para o rastreamento visual acompanhar.
     */
    EXCESSIVE_MOTION,

    /**
     * A cena não possui textura ou detalhes visuais
     * suficientes para o rastreamento espacial.
     */
    INSUFFICIENT_FEATURES,

    /**
     * A iluminação do ambiente é insuficiente
     * para o sensor da câmera.
     */
    INSUFFICIENT_LIGHT,

    /**
     * A câmera está temporariamente ocupada
     * ou inacessível pelo sistema operacional.
     */
    CAMERA_UNAVAILABLE,

    /**
     * A sessão foi pausada intencionalmente
     * pelo ciclo de vida do aplicativo.
     */
    PAUSED,

    /**
     * O rastreamento não está disponível
     * ou ocorreu uma falha irrecuperável.
     */
    UNAVAILABLE;

    /**
     * Indica se o estado atual permite solicitar
     * a criação de novas âncoras.
     */
    val allowsAnchorPlacement: Boolean
        get() = this == TRACKING

    /**
     * Indica se o usuário pode ajudar a recuperar o rastreamento
     * ajustando o movimento, a iluminação ou a superfície observada.
     */
    val isRecoverableByUser: Boolean
        get() = when (this) {
            EXCESSIVE_MOTION,
            INSUFFICIENT_FEATURES,
            INSUFFICIENT_LIGHT -> true

            INITIALIZING,
            TRACKING,
            CAMERA_UNAVAILABLE,
            PAUSED,
            UNAVAILABLE -> false
        }

    /**
     * Indica se a interface deve mostrar orientações
     * para recuperar ou iniciar o rastreamento.
     */
    val requiresCoachingHint: Boolean
        get() = this == INITIALIZING || isRecoverableByUser
}