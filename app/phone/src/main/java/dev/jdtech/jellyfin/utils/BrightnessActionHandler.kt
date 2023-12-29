package dev.jdtech.jellyfin.utils

import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.media3.ui.PlayerView
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.PlayerActivity
import dev.jdtech.jellyfin.isControlsLocked
import dev.jdtech.jellyfin.utils.gesture.inExclusionArea
import dev.jdtech.jellyfin.utils.gesture.isInRightHalfOf
import dev.jdtech.jellyfin.utils.gesture.isVerticalSwipe
import timber.log.Timber

class BrightnessActionHandler(
    private val activity: PlayerActivity,
    private val playerView: PlayerView,
    private val preferences: AppPreferences
) : PlayerGestureActionHandler<PlayerGestureAction.Scroll.Params> {

    override var isActive: Boolean = false

    private var swipeGestureValueTrackerBrightness = -1f

    override fun meetsRequirements(params: PlayerGestureAction.Scroll.Params): Boolean {
        return params.firstEvent.shouldPerformBrightnessChangeSwipe(
            distanceX = params.distanceX,
            distanceY = params.distanceY
        )
    }

    private fun MotionEvent.shouldPerformBrightnessChangeSwipe(
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (isActive) return true

        // Disables gestures if view is locked
        if (isControlsLocked) return false

        // Disables gestures if volume gestures are disabled
        if (preferences.playerGesturesVB.not()) return false

        // Excludes area where app gestures conflicting with system gestures
        if (inExclusionArea(playerView)) return false

        if (isInRightHalfOf(playerView)) return false

        if (isVerticalSwipe(distanceX, distanceY).not()) return false
        return true
    }

    override fun performAction(params: PlayerGestureAction.Scroll.Params) {
        val distanceFull = playerView.measuredHeight * FullSwipeRangeScreenRatio
        val ratioChange = params.distanceY / distanceFull

        performBrightnessChange(ratioChange)
        isActive = true
    }

    private fun performBrightnessChange(ratioChange: Float) {
        val window = activity.window
        val brightnessRange =
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF..WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL

        // Initialize on first swipe
        if (swipeGestureValueTrackerBrightness == -1f) {
            val brightness = window.attributes.screenBrightness
            Timber.d(
                "Brightness ${
                    Settings.System.getFloat(
                        activity.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS
                    )
                }"
            )
            swipeGestureValueTrackerBrightness = when (brightness) {
                in brightnessRange -> brightness
                else -> Settings.System.getFloat(
                    activity.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS
                ) / 255
            }
        }

        swipeGestureValueTrackerBrightness = swipeGestureValueTrackerBrightness
            .plus(ratioChange)
            .coerceIn(brightnessRange)

        val layoutParams = window.attributes
        layoutParams.screenBrightness = swipeGestureValueTrackerBrightness
        window.attributes = layoutParams

        adjustBrightnessViews(layoutParams.screenBrightness)
    }

    private fun adjustBrightnessViews(screenBrightness: Float) {
        activity.binding.gestureBrightnessLayout.visibility = View.VISIBLE
        activity.binding.gestureBrightnessProgressBar.max =
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL.times(100).toInt()
        activity.binding.gestureBrightnessProgressBar.progress =
            screenBrightness.times(100).toInt()
        val process =
            (screenBrightness / WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL).times(100)
                .toInt()
        activity.binding.gestureBrightnessText.text = "$process%"
        activity.binding.gestureBrightnessImage.setImageLevel(process)
    }

    private val hideGestureBrightnessIndicatorOverlayAction = Runnable {
        activity.binding.gestureBrightnessLayout.visibility = View.GONE
        if (preferences.playerBrightnessRemember) {
            preferences.playerBrightness = activity.window.attributes.screenBrightness
        }
    }

    override fun releaseAction() {
        activity.binding.gestureBrightnessLayout.apply {
            if (visibility == View.VISIBLE) {
                removeCallbacks(hideGestureBrightnessIndicatorOverlayAction)
                postDelayed(hideGestureBrightnessIndicatorOverlayAction, 1000)
                super.releaseAction()
            }
        }
    }
}
