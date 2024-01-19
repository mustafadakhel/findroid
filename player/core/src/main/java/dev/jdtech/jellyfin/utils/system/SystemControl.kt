package dev.jdtech.jellyfin.utils.system

import dev.jdtech.jellyfin.utils.system.brightness.BrightnessControl
import dev.jdtech.jellyfin.utils.system.volume.VolumeControl

interface SystemControl : BrightnessControl, VolumeControl
