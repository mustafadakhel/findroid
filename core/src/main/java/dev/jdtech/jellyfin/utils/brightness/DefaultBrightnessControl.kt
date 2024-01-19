package dev.jdtech.jellyfin.utils.brightness

import android.app.Activity
import android.content.ContentResolver
import android.provider.Settings
import android.view.Window
import android.view.WindowManager
import dev.jdtech.jellyfin.utils.system.brightness.BrightnessControl
import timber.log.Timber

fun Activity.createBrightnessControl(): BrightnessControl {
    return DefaultBrightnessControl(
        contentResolver,
        window,
    )
}

class DefaultBrightnessControl(
    private val contentResolver: ContentResolver,
    private val window: Window,
) : BrightnessControl {
    private val brightnessRange =
        WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF..WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL

    override fun getBrightness(): Float {
        // Initialize on first swipe
        val brightness = window.attributes.screenBrightness
        Timber.d(
            "Brightness ${
                Settings.System.getFloat(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                )
            }",
        )
        return when (brightness) {
            in brightnessRange -> brightness
            else ->
                Settings.System.getFloat(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                ) / 255
        }
    }

    override fun setBrightness(value: Float) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = value
        window.attributes = layoutParams
    }

    override fun changeBrightnessByRatio(ratio: Float) {
        val newBrightness =
            getBrightness()
                .plus(ratio)
                .coerceIn(brightnessRange)

        setBrightness(newBrightness)
    }

    override fun getMaxBrightness(): Float {
        return WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
    }
}
