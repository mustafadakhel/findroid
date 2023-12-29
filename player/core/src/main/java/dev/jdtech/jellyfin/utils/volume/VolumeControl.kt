package dev.jdtech.jellyfin.utils.volume

interface VolumeControl {

    enum class MuteState {
        Muted,
        UnMuted
    }

    var currentVolumeRatio: Float

    fun getVolume(): Int
    fun getMaxVolume(): Int
    fun setVolume(value: Int)
    fun changeVolumeByRatio(ratio: Float)
    fun mute()
    fun unMute()
    fun changeMuteState(): MuteState
    fun getMuteState(): MuteState
}
