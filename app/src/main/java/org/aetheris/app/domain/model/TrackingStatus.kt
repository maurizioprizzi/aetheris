package org.aetheris.app.domain.model

enum class TrackingStatus {
    INITIALIZING,
    TRACKING,
    EXCESSIVE_MOTION,
    INSUFFICIENT_FEATURES,
    PAUSED,
    UNAVAILABLE
}