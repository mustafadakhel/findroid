package dev.jdtech.jellyfin.utils.gestureDSL.actions.handlers.base

import dev.jdtech.jellyfin.utils.gestureDSL.actions.PlayerGestureAction

interface PlayerGestureActionHandler<Params : PlayerGestureAction.GestureActionParams> {
    val enabled: Boolean
    val active: Boolean

    fun setEnabled(enabled: Boolean)

    fun handle(params: Params): Boolean {
        if (meetsRequirements(params).not()) return false

        performAction(params)

        return true
    }

    fun meetsRequirements(params: Params): Boolean

    fun performAction(params: Params)

    fun releaseAction()
}
