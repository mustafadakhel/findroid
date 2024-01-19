package dev.jdtech.jellyfin.utils.gestureDSL.actions.handlers

import android.view.View
import dev.jdtech.jellyfin.utils.gestureDSL.actions.PlayerGestureAction

interface SwipeGestureActions :
    HorizontalSwipeActions,
    RightSideSwipeActions,
    LeftSideVerticalSwipeActions

class SwipeGestureActionHandler(
    swipeGestureActions: SwipeGestureActions,
    rootView: View,
    preferences: Prefs,
) {
    private val horizontalSwipeActionHandler =
        HorizontalSwipeActionHandler(
            horizontalSwipeActions = swipeGestureActions,
            rootView = rootView,
            enabled = preferences.enableHorizontalGesture,
        )

    private val leftSideVerticalSwipeHandler =
        LeftSideVerticalSwipeHandler(
            leftSideVerticalSwipeActions = swipeGestureActions,
            rootView = rootView,
            enabled = preferences.enableLeftSideVerticalGesture,
        )

    private val rightSideVerticalSwipeHandler =
        RightSideVerticalSwipeHandler(
            rightSideSwipeActions = swipeGestureActions,
            rootView = rootView,
            enabled = preferences.enableRightSideVerticalGesture,
        )

    private val isHorizontalSwipeActive get() = horizontalSwipeActionHandler.active
    private val isLeftSideVerticalSwipeActive get() = leftSideVerticalSwipeHandler.active
    private val isRightSideVerticalSwipeActive get() = rightSideVerticalSwipeHandler.active

    val isActive: Boolean
        get() = isHorizontalSwipeActive || isLeftSideVerticalSwipeActive || isRightSideVerticalSwipeActive

    fun handle(params: PlayerGestureAction.Swipe.Params): Boolean {
        if (isHorizontalSwipeActive) {
            return horizontalSwipeActionHandler.handle(params)
        }
        if (isLeftSideVerticalSwipeActive) {
            return leftSideVerticalSwipeHandler.handle(params)
        }

        if (isRightSideVerticalSwipeActive) {
            return rightSideVerticalSwipeHandler.handle(params)
        }

        val handledByHorizontalSwipe = horizontalSwipeActionHandler.handle(params)

        if (handledByHorizontalSwipe) {
            return true
        }

        val handledByRightSideVertical = rightSideVerticalSwipeHandler.handle(params)

        if (handledByRightSideVertical) {
            return true
        }

        val handledByLeftSideVertical = leftSideVerticalSwipeHandler.handle(params)

        return handledByLeftSideVertical
    }

    fun releaseAction() {
        leftSideVerticalSwipeHandler.releaseAction()
        rightSideVerticalSwipeHandler.releaseAction()
        horizontalSwipeActionHandler.releaseAction()
    }

    data class Prefs(
        val enableLeftSideVerticalGesture: Boolean,
        val enableRightSideVerticalGesture: Boolean,
        val enableHorizontalGesture: Boolean,
    )
}
