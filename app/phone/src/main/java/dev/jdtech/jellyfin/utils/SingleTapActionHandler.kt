package dev.jdtech.jellyfin.utils

import androidx.media3.ui.PlayerView

class SingleTapActionHandler(
    private val playerView: PlayerView
) : PlayerGestureActionHandler<PlayerGestureAction.SingleTap.Params> {

    override var isActive: Boolean = false

    override fun meetsRequirements(params: PlayerGestureAction.SingleTap.Params): Boolean {
        return true
    }

    override fun performAction(params: PlayerGestureAction.SingleTap.Params) {
        playerView.showHideController()
    }

    private fun PlayerView.showHideController() {
        if (!isControllerFullyVisible) showController() else hideController()
    }
}
