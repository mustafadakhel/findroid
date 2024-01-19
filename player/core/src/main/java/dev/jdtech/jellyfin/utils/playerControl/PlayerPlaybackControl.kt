package dev.jdtech.jellyfin.utils.playerControl

import dev.jdtech.jellyfin.utils.playerControl.seeker.Seeker

interface PlayerPlaybackControl : Seeker {
    fun togglePlayback()
}
