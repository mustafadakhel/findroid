package dev.jdtech.jellyfin.utils

import androidx.media3.ui.PlayerView
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.PlayerActivity
import dev.jdtech.jellyfin.utils.seeker.Seeker
import dev.jdtech.jellyfin.utils.volume.VolumeControl

class ScrollGestureActionHandler(
    activity: PlayerActivity,
    playerView: PlayerView,
    preferences: AppPreferences,
    seeker: Seeker,
    volumeControl: VolumeControl
) {

    private val swipeSeekActionHandler = SwipeSeekActionHandler(
        activity = activity,
        playerView = playerView,
        seeker = seeker,
        preferences = preferences
    )

    private val volumeActionHandler = VolumeActionHandler(
        activity = activity,
        playerView = playerView,
        volumeControl = volumeControl,
        preferences = preferences
    )

    private val brightnessActionHandler = BrightnessActionHandler(
        activity = activity,
        playerView = playerView,
        preferences = preferences
    )

    private val isSwipeSeekActive get() = swipeSeekActionHandler.isActive

    private val isBrightnessChangeActive get() = brightnessActionHandler.isActive

    private val isVolumeChangeActive get() = volumeActionHandler.isActive

    val isActive: Boolean
        get() = isSwipeSeekActive || isBrightnessChangeActive || isVolumeChangeActive

    fun handle(params: PlayerGestureAction.Scroll.Params): Boolean {
        if (isSwipeSeekActive) {
            return swipeSeekActionHandler.handle(params)
        }
        if (isBrightnessChangeActive) {
            return brightnessActionHandler.handle(params)
        }

        if (isVolumeChangeActive) {
            return volumeActionHandler.handle(params)
        }

        val handledBySwipeSeek = swipeSeekActionHandler.handle(params)

        if (handledBySwipeSeek) {
            return true
        }

        val handledByVolume = volumeActionHandler.handle(params)

        if (handledByVolume) {
            return true
        }

        val handledByBrightness = brightnessActionHandler.handle(params)

        return handledByBrightness
    }

    fun releaseAction() {
        volumeActionHandler.releaseAction()
        brightnessActionHandler.releaseAction()
        swipeSeekActionHandler.releaseAction()
    }
}
