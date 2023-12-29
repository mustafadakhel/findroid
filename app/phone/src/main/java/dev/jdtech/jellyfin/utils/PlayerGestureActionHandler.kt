package dev.jdtech.jellyfin.utils

interface PlayerGestureActionHandler<Params : PlayerGestureAction.GestureActionParams> {
    var isActive: Boolean

    fun handle(params: Params): Boolean {
        if (meetsRequirements(params).not()) return false

        performAction(params)

        return true
    }

    fun meetsRequirements(params: Params): Boolean

    fun performAction(params: Params)

    fun releaseAction() {
        isActive = false
    }
}
