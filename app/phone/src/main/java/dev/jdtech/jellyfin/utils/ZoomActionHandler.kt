package dev.jdtech.jellyfin.utils

import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.isControlsLocked
import dev.jdtech.jellyfin.mpv.MPVPlayer
import kotlin.math.abs

private const val ZoomScaleBase = 1f
private const val ZoomScaleThreshold = 0.01f

class ZoomActionHandler(
    private val playerView: PlayerView,
    private val preferences: AppPreferences
) : PlayerGestureActionHandler<PlayerGestureAction.Zoom.Params> {
    override var isActive: Boolean = false
    private var isZoomEnabled = false

    override fun meetsRequirements(params: PlayerGestureAction.Zoom.Params): Boolean {
        // Disables gestures if volume gestures are disabled
        if (preferences.playerGesturesZoom.not()) return false

        // Disables zoom gesture if view is locked
        if (isControlsLocked) return false

        val scaleFactor = params.scaleFactor

        return abs(scaleFactor - ZoomScaleBase) > ZoomScaleThreshold
    }

    override fun performAction(params: PlayerGestureAction.Zoom.Params) {
        isZoomEnabled = params.scaleFactor > 1
        updateZoomMode(isZoomEnabled)
    }

    private fun updateZoomMode(enabled: Boolean) {
        if (playerView.player is MPVPlayer) {
            (playerView.player as MPVPlayer).updateZoomMode(enabled)
        } else {
            playerView.resizeMode = if (enabled) {
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            } else {
                AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        }
    }
}
