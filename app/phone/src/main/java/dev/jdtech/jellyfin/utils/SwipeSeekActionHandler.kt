package dev.jdtech.jellyfin.utils

import android.view.View
import androidx.media3.ui.PlayerView
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.PlayerActivity
import dev.jdtech.jellyfin.isControlsLocked
import dev.jdtech.jellyfin.utils.gesture.inExclusionArea
import dev.jdtech.jellyfin.utils.gesture.isVerticalSwipe
import dev.jdtech.jellyfin.utils.seeker.Seeker
import kotlin.math.abs

private const val MinimumSeekSwipeDistance = 50

class SwipeSeekActionHandler(
    private val activity: PlayerActivity,
    private val playerView: PlayerView,
    private val seeker: Seeker,
    private val preferences: AppPreferences
) : PlayerGestureActionHandler<PlayerGestureAction.Scroll.Params> {

    override var isActive: Boolean = false

    private var progress: Long = -1L

    override fun meetsRequirements(params: PlayerGestureAction.Scroll.Params): Boolean {
        return shouldPerformSwipeSeek(params)
    }

    private fun shouldPerformSwipeSeek(
        params: PlayerGestureAction.Scroll.Params
    ): Boolean {
        if (isActive) return true

        // Disables gestures if view is locked
        if (isControlsLocked) return false

        // Disables gestures if volume gestures are disabled
        if (preferences.playerGesturesSeek.not()) return false

        // Excludes area where app gestures conflicting with system gestures
        if (params.firstEvent.inExclusionArea(playerView)) return false

        // Check whether swipe was oriented vertically
        if (isVerticalSwipe(params.distanceX, params.distanceY)) return false

        // Check if swipe was accidental
        val accidentalSwipeThreshold = params.currentEvent.x - (params.firstEvent.x)
        val accidental = abs(accidentalSwipeThreshold) <= MinimumSeekSwipeDistance
        if (accidental) return false

        return true
    }

    override fun performAction(params: PlayerGestureAction.Scroll.Params) {
        val currentPos = playerView.player?.currentPosition ?: 0
        val vidDuration = (playerView.player?.duration ?: 0).coerceAtLeast(0)

        val difference = ((params.currentEvent.x - params.firstEvent.x) * 90).toLong()
        val newPos = (currentPos + difference).coerceIn(0, vidDuration)

        activity.binding.progressScrubberLayout.visibility = View.VISIBLE
        setSwipeSeekScrubberText(difference, newPos)
        progress = newPos
        isActive = true
    }

    private fun setSwipeSeekScrubberText(difference: Long, newPos: Long) {
        val scrubberText = "${difference.toTimestamp()} [${newPos.toTimestamp(true)}]"
        activity.binding.progressScrubberText.text = scrubberText
    }

    private fun Long.toTimestamp(noSign: Boolean = false): String {
        val sign = if (noSign) "" else if (this < 0) "-" else "+"
        val seconds = abs(this).div(1000)

        return String.format(
            "%s%02d:%02d:%02d",
            sign,
            seconds / 3600,
            (seconds / 60) % 60,
            seconds % 60
        )
    }

    private val hideGestureProgressOverlayAction = Runnable {
        activity.binding.progressScrubberLayout.visibility = View.GONE
    }

    override fun releaseAction() {
        activity.binding.progressScrubberLayout.apply {
            if (visibility == View.VISIBLE) {
                if (progress > -1) {
                    seeker.seekTo(progress)
                }
                removeCallbacks(hideGestureProgressOverlayAction)
                postDelayed(hideGestureProgressOverlayAction, 1000)
                progress = -1L
                super.releaseAction()
            }
        }
    }
}
