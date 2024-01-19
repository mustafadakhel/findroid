package dev.jdtech.jellyfin.utils.gestureDSL.actions.handlers

import android.view.View
import dev.jdtech.jellyfin.utils.gestureDSL.actions.PlayerGestureAction
import dev.jdtech.jellyfin.utils.gestureDSL.actions.handlers.base.PlayerGestureActionHandler
import dev.jdtech.jellyfin.utils.gestureDSL.inExclusionArea
import dev.jdtech.jellyfin.utils.gestureDSL.isVerticalSwipe
import kotlin.math.abs

private const val MINIMUM_HORIZONTAL_SWIPE_DISTANCE = 50

interface HorizontalSwipeActions {
    fun onHorizontalSwipeStarted()

    fun onHorizontalSwipeReleased()

    fun onHorizontalSwipeValueChanged(difference: Long)
}

class HorizontalSwipeActionHandler(
    private val horizontalSwipeActions: HorizontalSwipeActions,
    private val rootView: View,
    override var enabled: Boolean,
) : PlayerGestureActionHandler<PlayerGestureAction.Swipe.Params> {
    override var active: Boolean = false

    override fun meetsRequirements(params: PlayerGestureAction.Swipe.Params): Boolean {
        return shouldPerformSwipeSeek(params)
    }

    private fun shouldPerformSwipeSeek(params: PlayerGestureAction.Swipe.Params): Boolean {
        if (active) return true

        // Disables gestures if volume gestures are disabled
        if (enabled.not()) return false

        // Excludes area where app gestures conflicting with system gestures
        if (params.firstEvent.inExclusionArea(rootView)) return false

        // Check whether swipe was oriented vertically
        if (isVerticalSwipe(params.distanceX, params.distanceY)) return false

        // Check if swipe was accidental
        val accidentalSwipeThreshold = params.currentEvent.x - (params.firstEvent.x)
        val accidental = abs(accidentalSwipeThreshold) <= MINIMUM_HORIZONTAL_SWIPE_DISTANCE
        if (accidental) return false

        return true
    }

    override fun performAction(params: PlayerGestureAction.Swipe.Params) {
        val difference = ((params.currentEvent.x - params.firstEvent.x) * 90).toLong()

        if (active.not()) {
            horizontalSwipeActions.onHorizontalSwipeStarted()
        }

        horizontalSwipeActions.onHorizontalSwipeValueChanged(difference)
        active = true
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun releaseAction() {
        if (active.not()) return
        horizontalSwipeActions.onHorizontalSwipeReleased()
        active = false
    }
}
