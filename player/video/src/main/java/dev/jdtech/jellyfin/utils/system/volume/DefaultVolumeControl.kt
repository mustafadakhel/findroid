package dev.jdtech.jellyfin.utils.system.volume

import android.content.Context
import android.media.AudioManager

fun Context.createVolumeControl() = getSystemService(AudioManager::class.java).createVolumeControl()

fun AudioManager.createVolumeControl(): VolumeControl {
    return DefaultVolumeControl(this)
}

class DefaultVolumeControl(private val audioManager: AudioManager) : VolumeControl {
    override var currentVolumeRatio = getVolume().toFloat()

    override fun getVolume() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    override fun getMaxVolume() = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    override fun setVolume(value: Int) =
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            value,
            0,
        )

    override fun changeVolumeByRatio(ratio: Float) {
        val maxVolume = getMaxVolume()
        val change = ratio * maxVolume
        currentVolumeRatio = (currentVolumeRatio + change).coerceIn(0f, maxVolume.toFloat())

        setVolume(currentVolumeRatio.toInt())
    }

    override fun mute() =
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_MUTE,
            0,
        )

    override fun unMute() =
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_UNMUTE,
            0,
        )

    override fun changeMuteState(): VolumeControl.MuteState {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_TOGGLE_MUTE,
            0,
        )
        return getMuteState()
    }

    override fun getMuteState() =
        if (
            audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
        ) {
            VolumeControl.MuteState.Muted
        } else {
            VolumeControl.MuteState.UnMuted
        }
}
