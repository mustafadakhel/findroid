package dev.jdtech.jellyfin.utils.system

import dev.jdtech.jellyfin.utils.system.brightness.BrightnessControl
import dev.jdtech.jellyfin.utils.system.volume.VolumeControl

class DefaultSystemControl(
    private val brightnessControl: BrightnessControl,
    private val volumeControl: VolumeControl,
) : SystemControl, BrightnessControl by brightnessControl, VolumeControl by volumeControl
