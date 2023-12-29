package dev.jdtech.jellyfin.utils

import android.view.MotionEvent
import android.view.View
import androidx.media3.ui.PlayerView
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.PlayerActivity
import dev.jdtech.jellyfin.isControlsLocked
import dev.jdtech.jellyfin.utils.gesture.inExclusionArea
import dev.jdtech.jellyfin.utils.gesture.isInRightHalfOf
import dev.jdtech.jellyfin.utils.gesture.isVerticalSwipe
import dev.jdtech.jellyfin.utils.volume.VolumeControl

class VolumeActionHandler(
    private val activity: PlayerActivity,
    private val playerView: PlayerView,
    private val volumeControl: VolumeControl,
    private val preferences: AppPreferences
) : PlayerGestureActionHandler<PlayerGestureAction.Scroll.Params> {

    override var isActive: Boolean = false

    override fun meetsRequirements(params: PlayerGestureAction.Scroll.Params): Boolean {
        return params.firstEvent.shouldPerformVolumeChangeSwipe(
            distanceX = params.distanceX,
            distanceY = params.distanceY
        )
    }

    private fun MotionEvent.shouldPerformVolumeChangeSwipe(
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

        if (isInRightHalfOf(playerView).not()) return false

        if (isVerticalSwipe(distanceX, distanceY).not()) return false
        return true
    }

    override fun performAction(params: PlayerGestureAction.Scroll.Params) {
        // Distance to swipe to go from min to max
        val distanceFull = playerView.measuredHeight * FullSwipeRangeScreenRatio
        val ratioChange = params.distanceY / distanceFull

        performVolumeChange(ratioChange)
        isActive = true
    }

    private fun performVolumeChange(ratioChange: Float) {
        val maxVolume = volumeControl.getMaxVolume()

        volumeControl.changeVolumeByRatio(ratioChange)

        val newVolumeRatio = volumeControl.currentVolumeRatio

        adjustVolumeViews(maxVolume, newVolumeRatio)
    }

    private fun adjustVolumeViews(maxVolume: Int, newVolumeRatio: Float) {
        activity.binding.gestureVolumeLayout.visibility = View.VISIBLE
        activity.binding.gestureVolumeProgressBar.max = maxVolume
        activity.binding.gestureVolumeProgressBar.progress = newVolumeRatio.toInt()
        val process = (newVolumeRatio / maxVolume.toFloat()).times(100).toInt()
        activity.binding.gestureVolumeText.text = "$process%"
        activity.binding.gestureVolumeImage.setImageLevel(process)
    }

    private val hideGestureVolumeIndicatorOverlayAction = Runnable {
        activity.binding.gestureVolumeLayout.visibility = View.GONE
    }

    override fun releaseAction() {
        activity.binding.gestureVolumeLayout.apply {
            if (visibility == View.VISIBLE) {
                removeCallbacks(hideGestureVolumeIndicatorOverlayAction)
                postDelayed(hideGestureVolumeIndicatorOverlayAction, 1000)
                super.releaseAction()
            }
        }
    }

}
