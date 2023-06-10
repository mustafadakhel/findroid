package dev.jdtech.jellyfin.utils

import android.annotation.SuppressLint
import android.content.res.Resources
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.PlayerActivity
import dev.jdtech.jellyfin.isControlsLocked
import dev.jdtech.jellyfin.mpv.MPVPlayer
import dev.jdtech.jellyfin.utils.gesture.gestureDetector
import dev.jdtech.jellyfin.utils.gesture.scaleGestureDetector
import dev.jdtech.jellyfin.utils.seeker.Seeker
import kotlin.math.abs
import timber.log.Timber

private const val MinimumSeekSwipeDistance = 50
private const val MinimumPostScaleWaitTimeMillis = 200
private const val GestureExclusionAreaVertical = 48
private const val GestureExclusionAreaHorizontal = 24
private const val FullSwipeRangeScreenRatio = 0.66f
private const val ZoomScaleBase = 1f
private const val ZoomScaleThreshold = 0.01f

class PlayerGestureHelper(
    private val appPreferences: AppPreferences,
    private val activity: PlayerActivity,
    private val playerView: PlayerView,
    private val audioManager: AudioManager,
    private val seeker: Seeker
) {
    /**
     * Tracks whether video content should fill the screen, cutting off unwanted content on the sides.
     * Useful on wide-screen phones to remove black bars from some movies.
     */
    private var isZoomEnabled = false

    /**
     * Tracks a value during a swipe gesture (between multiple onScroll calls).
     * When the gesture starts it's reset to an initial value and gets increased or decreased
     * (depending on the direction) as the gesture progresses.
     */

    private var swipeGestureValueTrackerVolume = -1f
    private var swipeGestureValueTrackerBrightness = -1f
    private var swipeGestureValueTrackerProgress = -1L

    private var swipeGestureVolumeOpen = false
    private var swipeGestureBrightnessOpen = false
    private var swipeGestureProgressOpen = false

    private var lastScaleEvent: Long = 0

    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    private val screenHeight = Resources.getSystem().displayMetrics.heightPixels

    private val tapGestureDetector = gestureDetector(playerView.context) {
        onSingleTapConfirmed {
            playerView.showHideController()

            return@onSingleTapConfirmed true
        }
        onDoubleTap(::doubleTapSeek)
    }

    private fun PlayerView.showHideController() {
        if (!isControllerFullyVisible) showController() else hideController()
    }

    private fun doubleTapSeek(event: MotionEvent): Boolean {
        // Disables double tap gestures if view is locked
        if (isControlsLocked) return false

        val isFastForward = event.isInRightHalfOf(playerView)
        performDoubleTapSeek(isFastForward)

        return true
    }

    private fun MotionEvent.isInRightHalfOf(view: View): Boolean {
        val viewCenterX = view.measuredWidth / 2
        return x.toInt() > viewCenterX
    }

    private fun performDoubleTapSeek(fastForward: Boolean) {
        if (fastForward)
            seeker.fastForward()
        else seeker.rewind()
    }

    @SuppressLint("SetTextI18n")
    private val seekGestureDetector = gestureDetector(
        playerView.context
    ) {
        onScroll { firstEvent: MotionEvent,
                   currentEvent: MotionEvent,
                   distanceX: Float,
                   distanceY: Float ->
            // Excludes area where app gestures conflicting with system gestures
            if (firstEvent.inExclusionArea()) return@onScroll false
            // Disables seek gestures if view is locked
            if (isControlsLocked) return@onScroll false
            // Check whether swipe was oriented vertically
            if (isVerticalSwipe(distanceX, distanceY).not())
                return@onScroll true
            if (shouldPerformSwipeSeek(firstEvent.x, currentEvent.x).not())
                return@onScroll false

            performSwipeSeek(firstEvent.x, currentEvent.x)

            return@onScroll true
        }
    }

    private fun performSwipeSeek(oldX: Float, newX: Float) {
        val currentPos = playerView.player?.currentPosition ?: 0
        val vidDuration = (playerView.player?.duration ?: 0).coerceAtLeast(0)

        val difference = ((newX - oldX) * 90).toLong()
        val newPos = (currentPos + difference).coerceIn(0, vidDuration)

        activity.binding.progressScrubberLayout.visibility = View.VISIBLE
        setSwipeSeekScrubberText(difference, newPos)
        swipeGestureValueTrackerProgress = newPos
        swipeGestureProgressOpen = true
    }

    private fun setSwipeSeekScrubberText(difference: Long, newPos: Long) {
        val scrubberText = "${difference.toTimestamp()} [${newPos.toTimestamp(true)}]"
        activity.binding.progressScrubberText.text = scrubberText
    }

    private fun isVerticalSwipe(
        distanceX: Float,
        distanceY: Float
    ) = abs(distanceY / distanceX) < 2

    private fun shouldPerformSwipeSeek(oldX: Float, newX: Float): Boolean {
        val notAccidental = abs(newX - oldX) > MinimumSeekSwipeDistance
        val elapsedTime = SystemClock.elapsedRealtime()
        val scaleEventFinished = (elapsedTime - lastScaleEvent) > MinimumPostScaleWaitTimeMillis
        return (notAccidental || swipeGestureProgressOpen) &&
               !swipeGestureBrightnessOpen &&
               !swipeGestureVolumeOpen && scaleEventFinished

    }

    @SuppressLint("SetTextI18n")
    private val vbGestureDetector = gestureDetector(
        playerView.context
    ) {
        onScroll { firstEvent: MotionEvent,
                   _: MotionEvent,
                   distanceX: Float,
                   distanceY: Float ->
            // Excludes area where app gestures conflicting with system gestures
            if (firstEvent.inExclusionArea()) return@onScroll false
            // Disables volume gestures when player is locked
            if (isControlsLocked) return@onScroll false

            if (abs(distanceY / distanceX) < 2) return@onScroll false

            if (swipeGestureValueTrackerProgress > -1 || swipeGestureProgressOpen)
                return@onScroll false

            val viewCenterX = playerView.measuredWidth / 2

            // Distance to swipe to go from min to max
            val distanceFull =
                playerView.measuredHeight * FullSwipeRangeScreenRatio
            val ratioChange = distanceY / distanceFull

            if (firstEvent.x.toInt() > viewCenterX) {
                // Swiping on the right, change volume

                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (swipeGestureValueTrackerVolume == -1f) swipeGestureValueTrackerVolume =
                    currentVolume.toFloat()

                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val change = ratioChange * maxVolume
                swipeGestureValueTrackerVolume =
                    (swipeGestureValueTrackerVolume + change).coerceIn(0f, maxVolume.toFloat())

                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    swipeGestureValueTrackerVolume.toInt(),
                    0
                )

                activity.binding.gestureVolumeLayout.visibility = View.VISIBLE
                activity.binding.gestureVolumeProgressBar.max = maxVolume
                activity.binding.gestureVolumeProgressBar.progress =
                    swipeGestureValueTrackerVolume.toInt()
                val process =
                    (swipeGestureValueTrackerVolume / maxVolume.toFloat()).times(100).toInt()
                activity.binding.gestureVolumeText.text = "$process%"
                activity.binding.gestureVolumeImage.setImageLevel(process)

                swipeGestureVolumeOpen = true
            } else {
                // Swiping on the left, change brightness
                val window = activity.window
                val brightnessRange = BRIGHTNESS_OVERRIDE_OFF..BRIGHTNESS_OVERRIDE_FULL

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
                swipeGestureValueTrackerBrightness =
                    (swipeGestureValueTrackerBrightness + ratioChange).coerceIn(brightnessRange)
                val lp = window.attributes
                lp.screenBrightness = swipeGestureValueTrackerBrightness
                window.attributes = lp

                activity.binding.gestureBrightnessLayout.visibility = View.VISIBLE
                activity.binding.gestureBrightnessProgressBar.max =
                    BRIGHTNESS_OVERRIDE_FULL.times(100).toInt()
                activity.binding.gestureBrightnessProgressBar.progress =
                    lp.screenBrightness.times(100).toInt()
                val process =
                    (lp.screenBrightness / BRIGHTNESS_OVERRIDE_FULL).times(100).toInt()
                activity.binding.gestureBrightnessText.text = "$process%"
                activity.binding.gestureBrightnessImage.setImageLevel(process)

                swipeGestureBrightnessOpen = true
            }
            return@onScroll true
        }
    }

    private val hideGestureVolumeIndicatorOverlayAction = Runnable {
        activity.binding.gestureVolumeLayout.visibility = View.GONE
    }

    private val hideGestureBrightnessIndicatorOverlayAction = Runnable {
        activity.binding.gestureBrightnessLayout.visibility = View.GONE
        if (appPreferences.playerBrightnessRemember) {
            appPreferences.playerBrightness = activity.window.attributes.screenBrightness
        }
    }

    private val hideGestureProgressOverlayAction = Runnable {
        activity.binding.progressScrubberLayout.visibility = View.GONE
    }

    /**
     * Handles scale/zoom gesture
     */
    private val zoomGestureDetector = scaleGestureDetector(
        playerView.context
    ) {
        onScale { detector ->
            // Disables zoom gesture if view is locked
            if (isControlsLocked) return@onScale false
            lastScaleEvent = SystemClock.elapsedRealtime()
            val scaleFactor = detector.scaleFactor
            if (abs(scaleFactor - ZoomScaleBase) > ZoomScaleThreshold) {
                isZoomEnabled = scaleFactor > 1
                updateZoomMode(isZoomEnabled)
            }
            return@onScale true
        }
    }.apply { isQuickScaleEnabled = false }

    private fun updateZoomMode(enabled: Boolean) {
        if (playerView.player is MPVPlayer) {
            (playerView.player as MPVPlayer).updateZoomMode(enabled)
        } else {
            playerView.resizeMode =
                if (enabled) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    private fun releaseAction(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_UP) {
            activity.binding.gestureVolumeLayout.apply {
                if (visibility == View.VISIBLE) {
                    removeCallbacks(hideGestureVolumeIndicatorOverlayAction)
                    postDelayed(hideGestureVolumeIndicatorOverlayAction, 1000)
                    swipeGestureVolumeOpen = false
                }
            }
            activity.binding.gestureBrightnessLayout.apply {
                if (visibility == View.VISIBLE) {
                    removeCallbacks(hideGestureBrightnessIndicatorOverlayAction)
                    postDelayed(hideGestureBrightnessIndicatorOverlayAction, 1000)
                    swipeGestureBrightnessOpen = false
                }
            }
            activity.binding.progressScrubberLayout.apply {
                if (visibility == View.VISIBLE) {
                    if (swipeGestureValueTrackerProgress > -1) {
                        playerView.player?.seekTo(swipeGestureValueTrackerProgress)
                    }
                    removeCallbacks(hideGestureProgressOverlayAction)
                    postDelayed(hideGestureProgressOverlayAction, 1000)
                    swipeGestureProgressOpen = false

                    swipeGestureValueTrackerProgress = -1L
                }
            }
        }
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

    private fun MotionEvent.inExclusionArea(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insets =
                playerView.rootWindowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemGestures())

            if ((x < insets.left) || (x > (screenWidth - insets.right)) ||
                (y < insets.top) || (y > (screenHeight - insets.bottom))
            )
                return true
        } else if (y < playerView.resources.dip(GestureExclusionAreaVertical) ||
            y > screenHeight - playerView.resources.dip(GestureExclusionAreaVertical) ||
            x < playerView.resources.dip(GestureExclusionAreaHorizontal) ||
            x > screenWidth - playerView.resources.dip(GestureExclusionAreaHorizontal)
        )
            return true
        return false
    }

    init {
        if (appPreferences.playerBrightnessRemember) {
            activity.window.attributes.screenBrightness = appPreferences.playerBrightness
        }

        @Suppress("ClickableViewAccessibility")
        playerView.setOnTouchListener { _, event ->
            if (playerView.useController) {
                when (event.pointerCount) {
                    1 -> {
                        tapGestureDetector.onTouchEvent(event)
                        if (appPreferences.playerGesturesVB) vbGestureDetector.onTouchEvent(event)
                        if (appPreferences.playerGesturesSeek) seekGestureDetector.onTouchEvent(
                            event
                        )
                    }

                    2 -> {
                        if (appPreferences.playerGesturesZoom) zoomGestureDetector.onTouchEvent(
                            event
                        )
                    }
                }
            }
            releaseAction(event)
            true
        }
    }
}
