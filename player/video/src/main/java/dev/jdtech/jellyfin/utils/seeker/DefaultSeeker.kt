package dev.jdtech.jellyfin.utils.seeker

import androidx.media3.common.Player
import kotlin.time.Duration

class DefaultSeeker(
    private val seekParameters: SeekParameters,
    private val player: Player
) : Seeker {

    override fun seekTo(millis: Long) = player.seekTo(millis)

    override fun seekTo(seconds: Double) = seekTo((seconds * 1000).toLong())

    override fun seekTo(duration: Duration) = seekTo(duration.inWholeMilliseconds)

    override fun fastForward() {
        val currentPosition = player.currentPosition
        val increment = seekParameters.playerFastForwardIncrement
        val newPosition = currentPosition + increment
        seekTo(newPosition)
    }

    override fun rewind() {
        val currentPosition = player.currentPosition
        val increment = seekParameters.playerRewindIncrement
        val newPosition = (currentPosition - increment).coerceAtLeast(0)
        seekTo(newPosition)
    }
}
