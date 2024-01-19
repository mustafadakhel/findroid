package dev.jdtech.jellyfin.utils.gestureDSL.actions.handlers

import android.view.View
import dev.jdtech.jellyfin.utils.gestureDSL.actions.PlayerGestureAction
import dev.jdtech.jellyfin.utils.gestureDSL.actions.handlers.base.PlayerGestureActionHandler

fun interface DoubleTapActions {
    fun onDoubleTapConfirmed(area: DoubleTapActionHandler.DoubleTapArea)
}

class DoubleTapActionHandler(
    private val doubleTapActions: DoubleTapActions,
    private val rootView: View,
    override var enabled: Boolean = true,
) : PlayerGestureActionHandler<PlayerGestureAction.DoubleTap.Params> {
    override var active: Boolean = false

    override fun meetsRequirements(params: PlayerGestureAction.DoubleTap.Params): Boolean {
        return enabled
    }

    override fun performAction(params: PlayerGestureAction.DoubleTap.Params) {
        // Disables double tap gestures if view is locked
        val doubleTapArea =
            DoubleTapArea.from(
                params.event.x.toInt(),
                rootView.measuredWidth,
            )
        performDoubleTapAction(doubleTapArea)
    }

    private fun performDoubleTapAction(area: DoubleTapArea) {
        doubleTapActions.onDoubleTapConfirmed(area)
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun releaseAction() {
        active = false
    }

    sealed interface DoubleTapArea {
        companion object {
            fun from(
                x: Int,
                playerViewWidth: Int,
            ): DoubleTapArea {
                // Divide the view into 5 parts: 2:1:2
                val areaWidth = playerViewWidth / 5

                // Define the areas and their boundaries
                val middleAreaStart = areaWidth * 2
                val rightmostAreaStart = middleAreaStart + areaWidth

                return when {
                    x < middleAreaStart -> LeftmostArea
                    x > rightmostAreaStart -> RightmostArea
                    else -> MiddleArea
                }
            }
        }

        data object LeftmostArea : DoubleTapArea

        data object MiddleArea : DoubleTapArea

        data object RightmostArea : DoubleTapArea
    }
}
