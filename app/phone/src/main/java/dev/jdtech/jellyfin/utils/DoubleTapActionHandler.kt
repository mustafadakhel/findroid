package dev.jdtech.jellyfin.utils

import android.view.ViewPropertyAnimator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.media3.ui.PlayerView
import dev.jdtech.jellyfin.PlayerActivity
import dev.jdtech.jellyfin.isControlsLocked
import dev.jdtech.jellyfin.utils.seeker.Seeker

class DoubleTapActionHandler(
    private val activity: PlayerActivity,
    private val playerView: PlayerView,
    private val seeker: Seeker
) : PlayerGestureActionHandler<PlayerGestureAction.DoubleTap.Params> {
    override var isActive: Boolean = false

    override fun meetsRequirements(params: PlayerGestureAction.DoubleTap.Params): Boolean {
        return !isControlsLocked
    }

    override fun performAction(params: PlayerGestureAction.DoubleTap.Params) {
        // Disables double tap gestures if view is locked
        val doubleTapArea = DoubleTapArea.from(
            params.event.x.toInt(),
            playerView.measuredWidth
        )
        performDoubleTapAction(doubleTapArea)
    }

    private fun performDoubleTapAction(area: DoubleTapArea) {
        when (area) {
            is DoubleTapArea.RightmostArea -> fastForward()
            is DoubleTapArea.LeftmostArea -> rewind()
            is DoubleTapArea.MiddleArea -> togglePlayback()
        }
    }

    private fun fastForward() {
        seeker.fastForward()
        animateRipple(activity.binding.imageFfwdAnimationRipple)
    }

    private fun rewind() {
        seeker.rewind()
        animateRipple(activity.binding.imageRewindAnimationRipple)
    }

    private fun togglePlayback() {
        playerView.player?.playWhenReady = !playerView.player?.playWhenReady!!
        animateRipple(activity.binding.imagePlaybackAnimationRipple)
    }

    private fun animateRipple(image: ImageView) {
        image
            .animateSeekingRippleStart()
            .withEndAction {
                resetRippleImage(image)
            }
            .start()
    }

    private fun ImageView.animateSeekingRippleStart(): ViewPropertyAnimator {
        val rippleImageHeight = this.height
        val playerViewHeight = playerView.height.toFloat()
        val playerViewWidth = playerView.width.toFloat()
        val scaleDifference = playerViewHeight / rippleImageHeight
        val playerViewAspectRatio = playerViewWidth / playerViewHeight
        val scaleValue = scaleDifference * playerViewAspectRatio
        return animate()
            .alpha(1f)
            .scaleX(scaleValue)
            .scaleY(scaleValue)
            .setDuration(180)
            .setInterpolator(DecelerateInterpolator())
    }

    private fun resetRippleImage(image: ImageView) {
        image
            .animateSeekingRippleEnd()
            .withEndAction {
                image.scaleX = 1f
                image.scaleY = 1f
            }
            .start()
    }

    private fun ImageView.animateSeekingRippleEnd() = animate()
        .alpha(0f)
        .setDuration(150)
        .setInterpolator(AccelerateInterpolator())


    sealed interface DoubleTapArea {
        companion object {
            fun from(
                x: Int,
                playerViewWidth: Int
            ): DoubleTapArea {
                // Divide the view into 5 parts: 2:1:2
                val areaWidth = playerViewWidth / 5

                // Define the areas and their boundaries
                val middleAreaStart = areaWidth * 2
                val rightmostAreaStart = middleAreaStart + areaWidth

                return when {
                    x < middleAreaStart -> LeftmostArea
                    x > rightmostAreaStart -> RightmostArea
                    else -> MiddleArea
                }
            }
        }

        data object LeftmostArea : DoubleTapArea
        data object MiddleArea : DoubleTapArea
        data object RightmostArea : DoubleTapArea
    }
}
