package dev.jdtech.jellyfin.utils.system.brightness

interface BrightnessControl {
    fun getBrightness(): Float

    fun setBrightness(value: Float)

    fun changeBrightnessByRatio(ratio: Float)

    fun getMaxBrightness(): Float
}
