package dev.jdtech.jellyfin.utils.seeker

import kotlin.time.Duration

interface Seeker {
    fun seekTo(millis: Long)
    fun seekTo(seconds: Double)
    fun seekTo(duration: Duration)
    fun fastForward()
    fun rewind()
}
