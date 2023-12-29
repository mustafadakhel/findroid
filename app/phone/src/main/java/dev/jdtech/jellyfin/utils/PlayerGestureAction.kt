package dev.jdtech.jellyfin.utils

import android.view.MotionEvent

sealed interface PlayerGestureAction {
    sealed interface GestureActionParams

    data object SingleTap : PlayerGestureAction {
        data class Params(
            val event: MotionEvent
        ) : GestureActionParams
    }

    data object DoubleTap : PlayerGestureAction {
        data class Params(
            val event: MotionEvent
        ) : GestureActionParams
    }

    sealed interface Scroll : PlayerGestureAction {
        data class Params(
            val firstEvent: MotionEvent,
            val currentEvent: MotionEvent,
            val distanceX: Float,
            val distanceY: Float
        ) : GestureActionParams

        data object SwipeSeek : Scroll
        data object Volume : Scroll
        data object Brightness : Scroll
    }

    data object Zoom : PlayerGestureAction {
        data class Params(
            val scaleFactor: Float
        ) : GestureActionParams
    }
}
