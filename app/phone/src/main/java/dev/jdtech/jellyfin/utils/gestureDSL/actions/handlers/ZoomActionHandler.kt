package dev.jdtech.jellyfin.utils.gestureDSL.actions.handlers

import dev.jdtech.jellyfin.isControlsLocked
import dev.jdtech.jellyfin.utils.gestureDSL.actions.PlayerGestureAction
import dev.jdtech.jellyfin.utils.gestureDSL.actions.handlers.base.PlayerGestureActionHandler
import kotlin.math.abs

private const val ZOOM_SCALE_BASE = 1f
private const val ZOOM_SCALE_THRESHOLD = 0.01f

interface ZoomGestureActions {
    fun onZoomIn()

    fun onZoomOut()
}

class ZoomActionHandler(
    private val zoomGestureActions: ZoomGestureActions,
    override var enabled: Boolean,
) : PlayerGestureActionHandler<PlayerGestureAction.Zoom.Params> {
    override var active: Boolean = false

    override fun meetsRequirements(params: PlayerGestureAction.Zoom.Params): Boolean {
        // Disables gestures if volume gestures are disabled
        if (enabled.not()) return false

        // Disables zoom gesture if view is locked
        if (isControlsLocked) return false

        val scaleFactor = params.scaleFactor

        return abs(scaleFactor - ZOOM_SCALE_BASE) > ZOOM_SCALE_THRESHOLD
    }

    override fun performAction(params: PlayerGestureAction.Zoom.Params) {
        if (params.scaleFactor > 1) {
            zoomGestureActions.onZoomIn()
        } else {
            zoomGestureActions.onZoomOut()
        }
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun releaseAction() {
        active = false
    }
}
