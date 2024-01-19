package dev.jdtech.jellyfin.utils.gestureDSL.actions.handlers

import android.view.MotionEvent
import android.view.View
import dev.jdtech.jellyfin.utils.FULL_SWIPE_RANGE_SCREEN_RATIO
import dev.jdtech.jellyfin.utils.gestureDSL.actions.PlayerGestureAction
import dev.jdtech.jellyfin.utils.gestureDSL.actions.handlers.base.PlayerGestureActionHandler
import dev.jdtech.jellyfin.utils.gestureDSL.inExclusionArea
import dev.jdtech.jellyfin.utils.gestureDSL.isInRightHalfOf
import dev.jdtech.jellyfin.utils.gestureDSL.isVerticalSwipe

interface LeftSideVerticalSwipeActions {
    fun onLeftSideVerticalSwipeStarted()

    fun onLeftSideVerticalSwipeValueChanged(ratioChange: Float)

    fun onLeftSideVerticalSwipeReleased()
}

class LeftSideVerticalSwipeHandler(
    private val leftSideVerticalSwipeActions: LeftSideVerticalSwipeActions,
    private val rootView: View,
    override var enabled: Boolean,
) : PlayerGestureActionHandler<PlayerGestureAction.Swipe.Params> {
    override var active: Boolean = false

    override fun meetsRequirements(params: PlayerGestureAction.Swipe.Params): Boolean {
        return params.firstEvent.shouldPerformBrightnessChangeSwipe(
            distanceX = params.distanceX,
            distanceY = params.distanceY,
        )
    }

    private fun MotionEvent.shouldPerformBrightnessChangeSwipe(
        distanceX: Float,
        distanceY: Float,
    ): Boolean {
        if (active) return true

        // Disables gestures if volume gestures are disabled
        if (enabled.not()) return false

        // Excludes area where app gestures conflicting with system gestures
        if (inExclusionArea(rootView)) return false

        if (isInRightHalfOf(rootView)) return false

        if (isVerticalSwipe(distanceX, distanceY).not()) return false
        return true
    }

    override fun performAction(params: PlayerGestureAction.Swipe.Params) {
        val distanceFull = rootView.measuredHeight * FULL_SWIPE_RANGE_SCREEN_RATIO
        val ratioChange = params.distanceY / distanceFull

        if (active.not()) {
            leftSideVerticalSwipeActions.onLeftSideVerticalSwipeStarted()
        }

        leftSideVerticalSwipeActions.onLeftSideVerticalSwipeValueChanged(ratioChange)

        active = true
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun releaseAction() {
        if (active.not()) return
        leftSideVerticalSwipeActions.onLeftSideVerticalSwipeReleased()
        active = false
    }
}
