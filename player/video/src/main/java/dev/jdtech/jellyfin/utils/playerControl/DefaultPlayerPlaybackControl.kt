package dev.jdtech.jellyfin.utils.playerControl

import androidx.media3.common.Player
import dev.jdtech.jellyfin.utils.playerControl.seeker.Seeker

class DefaultPlayerPlaybackControl(
    private val seeker: Seeker,
    private val player: Player,
) : PlayerPlaybackControl, Seeker by seeker {
    override fun togglePlayback() {
        player.playWhenReady = !player.playWhenReady
    }
}
