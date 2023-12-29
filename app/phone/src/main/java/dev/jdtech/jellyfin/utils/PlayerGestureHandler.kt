package dev.jdtech.jellyfin.utils

import android.os.SystemClock
import android.view.MotionEvent
import androidx.media3.ui.PlayerView
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.PlayerActivity
import dev.jdtech.jellyfin.utils.gesture.gestureDetector
import dev.jdtech.jellyfin.utils.gesture.scaleGestureDetector
import dev.jdtech.jellyfin.utils.seeker.Seeker
import dev.jdtech.jellyfin.utils.volume.VolumeControl

const val FullSwipeRangeScreenRatio = 0.66f

class PlayerGestureHandler(
    private val activity: PlayerActivity,
    appPreferences: AppPreferences,
    seeker: Seeker,
    volumeControl: VolumeControl
) {
    private val playerView: PlayerView
        get() = activity.binding.playerView

    private var lastScaleEvent: Long = 0

    private val doubleTapActionHandler = DoubleTapActionHandler(
        activity = activity,
        playerView = playerView,
        seeker = seeker
    )

    private val singleTapActionHandler = SingleTapActionHandler(playerView)

    private val scrollGestureActionHandler = ScrollGestureActionHandler(
        activity = activity,
        playerView = playerView,
        preferences = appPreferences,
        seeker = seeker,
        volumeControl = volumeControl
    )

    private val gestureDetector = gestureDetector(playerView.context) {
        onSingleTapConfirmed {
            singleTapActionHandler.handle(PlayerGestureAction.SingleTap.Params(it))
        }
        onDoubleTap {
            doubleTapActionHandler.handle(PlayerGestureAction.DoubleTap.Params(it))
        }
        onScroll { firstEvent, currentEvent, distanceX, distanceY ->
            firstEvent?.let {
                scrollGestureActionHandler.handle(
                    PlayerGestureAction.Scroll.Params(
                        firstEvent = firstEvent,
                        currentEvent = currentEvent,
                        distanceX = distanceX,
                        distanceY = distanceY
                    )
                )
            } ?: false
        }
    }

    private val zoomActionHandler = ZoomActionHandler(
        playerView = playerView,
        preferences = appPreferences
    )

    private val scaleGestureDetector = scaleGestureDetector(
        playerView.context
    ) {
        onScale { detector ->
            if (scrollGestureActionHandler.isActive) {
                return@onScale false
            }
            val scaleFactor = detector.scaleFactor
            val handled = zoomActionHandler.handle(
                PlayerGestureAction.Zoom.Params(
                    scaleFactor = scaleFactor
                )
            )
            if (handled) {
                lastScaleEvent = SystemClock.elapsedRealtime()
            }

            return@onScale handled
        }
    }.apply { isQuickScaleEnabled = false }

    init {
        if (appPreferences.playerBrightnessRemember) {
            activity.window.attributes.screenBrightness = appPreferences.playerBrightness
        }

        @Suppress("ClickableViewAccessibility")
        playerView.setOnTouchListener { _, event ->
            if (playerView.useController) {
                when (event.pointerCount) {
                    1 -> gestureDetector.onTouchEvent(event)

                    2 -> scaleGestureDetector.onTouchEvent(event)
                }
            }
            if (event.action == MotionEvent.ACTION_UP) {
                releaseActions()
            }
            true
        }
    }

    private fun releaseActions() {
        scrollGestureActionHandler.releaseAction()
    }
}
