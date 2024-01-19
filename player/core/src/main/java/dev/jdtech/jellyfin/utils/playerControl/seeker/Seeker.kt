package dev.jdtech.jellyfin.utils.playerControl.seeker

import kotlin.time.Duration

interface Seeker {
    fun seekTo(millis: Long)

    fun seekTo(seconds: Double)

    fun seekTo(duration: Duration)

    fun fastForward()

    fun rewind()

    fun calculateNewSeekPosByDifference(difference: Long): Long
}
