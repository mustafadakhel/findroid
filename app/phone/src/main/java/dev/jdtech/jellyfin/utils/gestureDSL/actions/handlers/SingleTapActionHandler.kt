package dev.jdtech.jellyfin.utils.gestureDSL.actions.handlers

import dev.jdtech.jellyfin.utils.gestureDSL.actions.PlayerGestureAction
import dev.jdtech.jellyfin.utils.gestureDSL.actions.handlers.base.PlayerGestureActionHandler

fun interface SingleTapActions {
    fun singleTapConfirmed()
}

class SingleTapActionHandler(
    private val singleTapActions: SingleTapActions,
    override var enabled: Boolean = true,
) : PlayerGestureActionHandler<PlayerGestureAction.SingleTap.Params> {
    override var active: Boolean = false

    override fun meetsRequirements(params: PlayerGestureAction.SingleTap.Params): Boolean {
        return enabled
    }

    override fun performAction(params: PlayerGestureAction.SingleTap.Params) {
        singleTapActions.singleTapConfirmed()
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun releaseAction() {
        active = false
    }
}
