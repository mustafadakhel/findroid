package dev.jdtech.jellyfin.utils

import androidx.media3.ui.PlayerView
import dev.jdtech.jellyfin.utils.gestureDSL.actions.handlers.DoubleTapActionHandler
import dev.jdtech.jellyfin.utils.gestureDSL.actions.handlers.PlayerGesturePrefs
import dev.jdtech.jellyfin.utils.gestureDSL.actions.handlers.installPlayerGestureHandler
import dev.jdtech.jellyfin.utils.playerControl.PlayerPlaybackControl
import dev.jdtech.jellyfin.utils.system.SystemControl
import kotlin.math.abs

const val FULL_SWIPE_RANGE_SCREEN_RATIO = 0.66f

interface SwipeSeekViewControl {
    fun hideSwipeSeekLayout()

    fun showSwipeSeekLayout()

    fun setSwipeSeekText(swipeSeekText: String)
}

interface VolumeSwipeViewControl {
    fun hideVolumeLayout()

    fun showVolumeLayout()

    fun setVolumeProgress(
        maxVolume: Int,
        currentVolume: Int,
    )

    fun setVolumeText(volumeText: String)

    fun setVolumeImageLevel(level: Int)
}

interface BrightnessSwipeViewControl {
    fun hideBrightnessLayout()

    fun showBrightnessLayout()

    fun setBrightnessProgress(
        maxBrightness: Float,
        currentBrightness: Float,
    )

    fun setBrightnessText(brightnessText: String)

    fun setBrightnessImageLevel(level: Int)
}

fun interface ZoomViewControl {
    fun updateZoomMode(zoom: Boolean)
}

fun interface DoubleTapViewControl {
    fun showDoubleTapReaction(doubleTapArea: DoubleTapActionHandler.DoubleTapArea)
}

fun interface SingleTapViewControl {
    fun showHideController()
}

interface PlayerGestureViewControl :
    SwipeSeekViewControl,
    VolumeSwipeViewControl,
    BrightnessSwipeViewControl,
    DoubleTapViewControl,
    SingleTapViewControl,
    ZoomViewControl {
    val useController: Boolean
}

fun PlayerView.installPlayerGestureHandler(
    playerGestureViewControl: PlayerGestureViewControl,
    playerPlaybackControl: PlayerPlaybackControl,
    systemControl: SystemControl,
    prefs: PlayerGesturePrefs,
) {
    var seekProgress = -1L
    var maxVolume = 0
    var screenBrightness = 0f

    installPlayerGestureHandler(
        prefs = prefs,
    ) {
        onDoubleTapConfirmed { area ->
            playerGestureViewControl.showDoubleTapReaction(area)
            when (area) {
                DoubleTapActionHandler.DoubleTapArea.LeftmostArea -> {
                    playerPlaybackControl.rewind()
                }

                DoubleTapActionHandler.DoubleTapArea.MiddleArea -> {
                    playerPlaybackControl.togglePlayback()
                }

                DoubleTapActionHandler.DoubleTapArea.RightmostArea -> {
                    playerPlaybackControl.fastForward()
                }
            }
        }
        onSingleTapConfirmed {
            playerGestureViewControl.showHideController()
        }
        onHorizontalSwipeStarted {
            playerGestureViewControl.showSwipeSeekLayout()
        }
        onHorizontalSwipeValueChanged { difference ->
            seekProgress = playerPlaybackControl.calculateNewSeekPosByDifference(difference)
            val scrubberText =
                "${difference.toTimestamp()} [${seekProgress.toTimestamp(true)}]"
            playerGestureViewControl.setSwipeSeekText(scrubberText)
        }
        onHorizontalSwipeReleased {
            playerPlaybackControl.seekTo(seekProgress)
            playerGestureViewControl.hideSwipeSeekLayout()
            seekProgress = -1L
        }
        onRightSideVerticalSwipeStarted {
            playerGestureViewControl.showVolumeLayout()
            maxVolume = systemControl.getMaxVolume()
        }
        onRightSideVerticalSwipeValueChanged { ratioChange ->
            systemControl.changeVolumeByRatio(ratioChange)

            val newVolumeRatio = systemControl.currentVolumeRatio

            playerGestureViewControl.setVolumeProgress(maxVolume, newVolumeRatio.toInt())
            val process = (newVolumeRatio / maxVolume.toFloat()).times(100).toInt()
            playerGestureViewControl.setVolumeText("$process%")
            playerGestureViewControl.setVolumeImageLevel(process)
        }
        onRightSideVerticalSwipeReleased {
            playerGestureViewControl.hideVolumeLayout()
        }
        onLeftSideVerticalSwipeStarted {
            playerGestureViewControl.showBrightnessLayout()
            screenBrightness = systemControl.getBrightness()
        }
        onLeftSideVerticalSwipeValueChanged { ratioChange ->
            systemControl.changeBrightnessByRatio(ratioChange)

            playerGestureViewControl.showBrightnessLayout()

            playerGestureViewControl.setBrightnessProgress(
                maxBrightness = systemControl.getMaxBrightness().times(100),
                currentBrightness = screenBrightness.times(100),
            )
            val process =
                (screenBrightness / systemControl.getMaxBrightness()).times(100)
                    .toInt()
            playerGestureViewControl.setBrightnessText("$process%")
            playerGestureViewControl.setBrightnessImageLevel(process)
        }
        onLeftSideVerticalSwipeReleased {
            playerGestureViewControl.hideBrightnessLayout()
        }
        onZoomIn {
            playerGestureViewControl.updateZoomMode(true)
        }
        onZoomOut {
            playerGestureViewControl.updateZoomMode(false)
        }
    }
}

fun Long.toTimestamp(noSign: Boolean = false): String {
    val sign =
        if (noSign) {
            ""
        } else if (this < 0) {
            "-"
        } else {
            "+"
        }
    val seconds = abs(this).div(1000)

    return String.format(
        "%s%02d:%02d:%02d",
        sign,
        seconds / 3600,
        (seconds / 60) % 60,
        seconds % 60,
    )
}
