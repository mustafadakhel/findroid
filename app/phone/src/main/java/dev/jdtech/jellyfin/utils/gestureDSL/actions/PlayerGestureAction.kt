package dev.jdtech.jellyfin.utils.gestureDSL.actions

import android.view.MotionEvent

sealed interface PlayerGestureAction {
    sealed interface GestureActionParams

    data object SingleTap : PlayerGestureAction {
        data class Params(
            val event: MotionEvent,
        ) : GestureActionParams
    }

    data object DoubleTap : PlayerGestureAction {
        data class Params(
            val event: MotionEvent,
        ) : GestureActionParams
    }

    sealed interface Swipe : PlayerGestureAction {
        data class Params(
            val firstEvent: MotionEvent,
            val currentEvent: MotionEvent,
            val distanceX: Float,
            val distanceY: Float,
        ) : GestureActionParams

        data object SwipeSeek : Swipe

        data object Volume : Swipe

        data object Brightness : Swipe
    }

    data object Zoom : PlayerGestureAction {
        data class Params(
            val scaleFactor: Float,
        ) : GestureActionParams
    }
}
