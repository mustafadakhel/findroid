package dev.jdtech.jellyfin.utils.gestureDSL.actions.handlers

import android.view.MotionEvent
import androidx.media3.ui.PlayerView
import dev.jdtech.jellyfin.utils.gestureDSL.actions.PlayerGestureAction
import dev.jdtech.jellyfin.utils.gestureDSL.gestureDetector
import dev.jdtech.jellyfin.utils.gestureDSL.scaleGestureDetector

fun PlayerView.installPlayerGestureHandler(
    prefs: PlayerGesturePrefs,
    gestureHandlerScope: PlayerGestureHandlerScope.() -> Unit,
) {
    val scope = PlayerGestureHandler().apply(gestureHandlerScope)

    val doubleTapActionHandler = DoubleTapActionHandler(
        doubleTapActions = scope,
        rootView = this,
        enabled = prefs.enableDoubleTapAction,
    )

    val singleTapActionHandler = SingleTapActionHandler(
        singleTapActions = scope,
        enabled = prefs.enableSingleTapAction,
    )

    val swipeGestureActionHandler = SwipeGestureActionHandler(
        swipeGestureActions = scope,
        rootView = this,
        preferences = prefs.swipePrefs(),
    )

    val gestureDetector = gestureDetector(this.context) {
        onSingleTapConfirmed {
            singleTapActionHandler.handle(PlayerGestureAction.SingleTap.Params(it))
        }
        onDoubleTap {
            doubleTapActionHandler.handle(PlayerGestureAction.DoubleTap.Params(it))
        }
        onScroll { firstEvent, currentEvent, distanceX, distanceY ->
            firstEvent?.let {
                swipeGestureActionHandler.handle(
                    PlayerGestureAction.Swipe.Params(
                        firstEvent = firstEvent,
                        currentEvent = currentEvent,
                        distanceX = distanceX,
                        distanceY = distanceY,
                    ),
                )
            } ?: false
        }
    }

    val zoomGestureActionHandler = ZoomActionHandler(
        zoomGestureActions = scope,
        enabled = prefs.enableZoomGesture,
    )

    val scaleGestureDetector = scaleGestureDetector(
        this.context,
    ) {
        onScale { detector ->
            if (swipeGestureActionHandler.isActive) {
                return@onScale false
            }
            val scaleFactor = detector.scaleFactor
            return@onScale zoomGestureActionHandler.handle(
                PlayerGestureAction.Zoom.Params(
                    scaleFactor = scaleFactor,
                ),
            )
        }
    }.apply { isQuickScaleEnabled = false }

    @Suppress("ClickableViewAccessibility")
    setOnTouchListener { _, event ->
        if (useController) {
            when (event.pointerCount) {
                1 -> gestureDetector.onTouchEvent(event)

                2 -> scaleGestureDetector.onTouchEvent(event)
            }
        }
        if (event.action == MotionEvent.ACTION_UP) {
            swipeGestureActionHandler.releaseAction()
            zoomGestureActionHandler.releaseAction()
        }
        true
    }
}

interface PlayerGestureHandlerScope {
    fun onZoomIn(zoomIn: () -> Unit)
    fun onZoomOut(zoomOut: () -> Unit)
    fun onDoubleTapConfirmed(doubleTapConfirmed: (area: DoubleTapActionHandler.DoubleTapArea) -> Unit)
    fun onSingleTapConfirmed(singleTapConfirmed: () -> Unit)
    fun onHorizontalSwipeStarted(horizontalSwipeStarted: () -> Unit)
    fun onHorizontalSwipeValueChanged(horizontalSwipeValueChanged: (difference: Long) -> Unit)
    fun onHorizontalSwipeReleased(horizontalSwipeReleased: () -> Unit)
    fun onRightSideVerticalSwipeStarted(rightSideVerticalSwipeStarted: () -> Unit)
    fun onRightSideVerticalSwipeValueChanged(rightSideVerticalSwipeValueChanged: (ratio: Float) -> Unit)
    fun onRightSideVerticalSwipeReleased(rightSideVerticalSwipeReleased: () -> Unit)
    fun onLeftSideVerticalSwipeStarted(leftSideVerticalSwipeStarted: () -> Unit)
    fun onLeftSideVerticalSwipeValueChanged(leftSideVerticalSwipeValueChanged: (ratio: Float) -> Unit)
    fun onLeftSideVerticalSwipeReleased(leftSideVerticalSwipeReleased: () -> Unit)
}

class PlayerGestureHandler :
    PlayerGestureHandlerScope,
    DoubleTapActions,
    SingleTapActions,
    SwipeGestureActions,
    ZoomGestureActions {
    var zoomIn: () -> Unit = {}
    var zoomOut: () -> Unit = {}
    var doubleTapConfirmed: (area: DoubleTapActionHandler.DoubleTapArea) -> Unit = {}
    var singleTapConfirmed: () -> Unit = {}
    var horizontalSwipeStarted: () -> Unit = {}
    var horizontalSwipeValueChanged: (difference: Long) -> Unit = {}
    var horizontalSwipeReleased: () -> Unit = {}
    var rightSideVerticalSwipeStarted: () -> Unit = {}
    var rightSideVerticalSwipeValueChanged: (ratio: Float) -> Unit = {}
    var rightSideVerticalSwipeReleased: () -> Unit = {}
    var leftSideVerticalSwipeStarted: () -> Unit = {}
    var leftSideVerticalSwipeValueChanged: (ratio: Float) -> Unit = {}
    var leftSideVerticalSwipeReleased: () -> Unit = {}

    override fun onZoomIn(zoomIn: () -> Unit) {
        this.zoomIn = zoomIn
    }

    override fun onZoomOut(zoomOut: () -> Unit) {
        this.zoomOut = zoomOut
    }

    override fun onDoubleTapConfirmed(doubleTapConfirmed: (area: DoubleTapActionHandler.DoubleTapArea) -> Unit) {
        this.doubleTapConfirmed = doubleTapConfirmed
    }

    override fun onSingleTapConfirmed(singleTapConfirmed: () -> Unit) {
        this.singleTapConfirmed = singleTapConfirmed
    }

    override fun onHorizontalSwipeStarted(horizontalSwipeStarted: () -> Unit) {
        this.horizontalSwipeStarted = horizontalSwipeStarted
    }

    override fun onHorizontalSwipeValueChanged(horizontalSwipeValueChanged: (difference: Long) -> Unit) {
        this.horizontalSwipeValueChanged = horizontalSwipeValueChanged
    }

    override fun onHorizontalSwipeReleased(horizontalSwipeReleased: () -> Unit) {
        this.horizontalSwipeReleased = horizontalSwipeReleased
    }

    override fun onRightSideVerticalSwipeStarted(rightSideVerticalSwipeStarted: () -> Unit) {
        this.rightSideVerticalSwipeStarted = rightSideVerticalSwipeStarted
    }

    override fun onRightSideVerticalSwipeValueChanged(rightSideVerticalSwipeValueChanged: (ratio: Float) -> Unit) {
        this.rightSideVerticalSwipeValueChanged = rightSideVerticalSwipeValueChanged
    }

    override fun onRightSideVerticalSwipeReleased(rightSideVerticalSwipeReleased: () -> Unit) {
        this.rightSideVerticalSwipeReleased = rightSideVerticalSwipeReleased
    }

    override fun onLeftSideVerticalSwipeStarted(leftSideVerticalSwipeStarted: () -> Unit) {
        this.leftSideVerticalSwipeStarted = leftSideVerticalSwipeStarted
    }

    override fun onLeftSideVerticalSwipeValueChanged(leftSideVerticalSwipeValueChanged: (ratio: Float) -> Unit) {
        this.leftSideVerticalSwipeValueChanged = leftSideVerticalSwipeValueChanged
    }

    override fun onLeftSideVerticalSwipeReleased(leftSideVerticalSwipeReleased: () -> Unit) {
        this.leftSideVerticalSwipeReleased = leftSideVerticalSwipeReleased
    }

    override fun onDoubleTapConfirmed(area: DoubleTapActionHandler.DoubleTapArea) {
        doubleTapConfirmed(area)
    }

    override fun singleTapConfirmed() {
        singleTapConfirmed()
    }

    override fun onHorizontalSwipeStarted() {
        horizontalSwipeStarted()
    }

    override fun onHorizontalSwipeValueChanged(difference: Long) {
        horizontalSwipeValueChanged(difference)
    }

    override fun onHorizontalSwipeReleased() {
        horizontalSwipeReleased()
    }

    override fun onRightSideVerticalSwipeStarted() {
        rightSideVerticalSwipeStarted()
    }

    override fun onRightSideVerticalSwipeValueChanged(ratioChange: Float) {
        rightSideVerticalSwipeValueChanged(ratioChange)
    }

    override fun onRightSideVerticalSwipeReleased() {
        rightSideVerticalSwipeReleased()
    }

    override fun onLeftSideVerticalSwipeStarted() {
        leftSideVerticalSwipeStarted()
    }

    override fun onLeftSideVerticalSwipeValueChanged(ratioChange: Float) {
        leftSideVerticalSwipeValueChanged(ratioChange)
    }

    override fun onLeftSideVerticalSwipeReleased() {
        leftSideVerticalSwipeReleased()
    }

    override fun onZoomIn() {
        zoomIn()
    }

    override fun onZoomOut() {
        zoomOut()
    }
}

data class PlayerGesturePrefs(
    val enableLeftVerticalSwipeGesture: Boolean,
    val enableRightVerticalSwipeGesture: Boolean,
    val enableHorizontalSwipeGesture: Boolean,
    val enableDoubleTapAction: Boolean = true,
    val enableSingleTapAction: Boolean = true,
    val enableZoomGesture: Boolean,
)

private fun PlayerGesturePrefs.swipePrefs() =
    SwipeGestureActionHandler.Prefs(
        enableLeftSideVerticalGesture = enableLeftVerticalSwipeGesture,
        enableRightSideVerticalGesture = enableRightVerticalSwipeGesture,
        enableHorizontalGesture = enableHorizontalSwipeGesture,
    )
