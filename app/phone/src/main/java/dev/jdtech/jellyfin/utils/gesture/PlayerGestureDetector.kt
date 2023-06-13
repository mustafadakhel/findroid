package dev.jdtech.jellyfin.utils.gesture

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector

fun gestureDetector(
    context: Context,
    scope: PlayerGestureDetectorScope.() -> Unit
): GestureDetector = PlayerGestureDetector().apply(scope).create(context)

fun scaleGestureDetector(
    context: Context,
    scope: PlayerScaleGestureDetectorScope.() -> Unit
): ScaleGestureDetector = PlayerScaleGestureDetector().apply(scope).create(context)

private typealias OnScrollAction = (
    firstEvent: MotionEvent,
    currentEvent: MotionEvent,
    distanceX: Float,
    distanceY: Float
) -> Boolean

private typealias OnSingleTapConfirmedAction = (e: MotionEvent) -> Boolean
private typealias OnDoubleTapAction = (e: MotionEvent) -> Boolean
private typealias OnScaleBeginAction = (detector: ScaleGestureDetector) -> Boolean
private typealias OnScaleEndAction = (detector: ScaleGestureDetector) -> Unit
private typealias OnScaleAction = (detector: ScaleGestureDetector) -> Boolean

interface PlayerGestureDetectorScope {
    fun onScroll(onScrollAction: OnScrollAction)
    fun onSingleTapConfirmed(onSingleTapConfirmedAction: OnSingleTapConfirmedAction)
    fun onDoubleTap(onDoubleTapAction: OnDoubleTapAction)
}

class PlayerGestureDetector : PlayerGestureDetectorScope {
    private var onScrollAction: OnScrollAction? = null
    private var onSingleTapConfirmedAction: OnSingleTapConfirmedAction? = null
    private var onDoubleTapAction: OnDoubleTapAction? = null

    override fun onScroll(onScrollAction: OnScrollAction) {
        this.onScrollAction = onScrollAction
    }

    override fun onSingleTapConfirmed(onSingleTapConfirmedAction: OnSingleTapConfirmedAction) {
        this.onSingleTapConfirmedAction = onSingleTapConfirmedAction
    }

    override fun onDoubleTap(onDoubleTapAction: OnDoubleTapAction) {
        this.onDoubleTapAction = onDoubleTapAction
    }

    fun create(context: Context): GestureDetector {
        return GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    return onSingleTapConfirmedAction?.invoke(e) ?: super.onSingleTapConfirmed(e)
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    return onDoubleTapAction?.invoke(e) ?: super.onDoubleTap(e)
                }

                override fun onScroll(
                    firstEvent: MotionEvent,
                    secondEvent: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    return onScrollAction?.invoke(
                        firstEvent,
                        secondEvent,
                        distanceX,
                        distanceY
                    ) ?: super.onScroll(firstEvent, secondEvent, distanceX, distanceY)
                }
            }
        )
    }
}

interface PlayerScaleGestureDetectorScope {
    fun onScaleBegin(onScaleBeginAction: OnScaleBeginAction)
    fun onScaleEnd(onScaleEndAction: OnScaleEndAction)
    fun onScale(onScaleAction: OnScaleAction)
}

class PlayerScaleGestureDetector : PlayerScaleGestureDetectorScope {
    private var onScaleBeginAction: OnScaleBeginAction? = null
    private var onScaleEndAction: OnScaleEndAction? = null
    private var onScaleAction: OnScaleAction? = null

    override fun onScaleBegin(onScaleBeginAction: OnScaleBeginAction) {
        this.onScaleBeginAction = onScaleBeginAction
    }

    override fun onScaleEnd(onScaleEndAction: OnScaleEndAction) {
        this.onScaleEndAction = onScaleEndAction
    }

    override fun onScale(onScaleAction: OnScaleAction) {
        this.onScaleAction = onScaleAction
    }

    fun create(context: Context): ScaleGestureDetector {
        return ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.OnScaleGestureListener {
                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    return onScaleBeginAction?.invoke(detector) ?: true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {
                    onScaleEndAction?.invoke(detector)
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    return onScaleAction?.invoke(detector) ?: false
                }
            }
        )
    }
}
