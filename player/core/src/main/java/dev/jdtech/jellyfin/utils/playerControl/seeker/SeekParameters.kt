package dev.jdtech.jellyfin.utils.playerControl.seeker

import dev.jdtech.jellyfin.AppPreferences

data class SeekParameters(
    val playerFastForwardIncrement: Long,
    val playerRewindIncrement: Long,
) {
    companion object {
        fun fromUserPreferences(appPreferences: AppPreferences) =
            SeekParameters(
                playerFastForwardIncrement = appPreferences.playerSeekForwardIncrement,
                playerRewindIncrement = appPreferences.playerSeekBackIncrement,
            )
    }
}
