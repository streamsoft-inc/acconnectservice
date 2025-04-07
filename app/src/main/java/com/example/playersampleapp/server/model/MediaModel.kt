package com.example.playersampleapp.server.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

sealed class MediaCommands(var hasBody: Boolean = false) {
    class Load(var playlist: List<PlaylistItemDTO> = emptyList()): MediaCommands(true)
    class Play(var play: PlayDTO): MediaCommands(hasBody = true)
    class Pause(var value:Boolean = true): MediaCommands()
    object Next: MediaCommands()
    object Previous: MediaCommands()
    object Stop: MediaCommands()
    class Mute(var muteDTO: MuteDTO): MediaCommands()
    class SetVolume(var volumeDTO: VolumeDTO): MediaCommands()
    class SeekTo(var seekDTO: SeekDTO): MediaCommands(hasBody = true)
    class Connect(var connectDTO: ConnectDTO): MediaCommands()
    class Disconnect(): MediaCommands()
}

class PlayerAPIEndpoint(
    val load: String,
    val play: String,
    val pause: String,
    val next: String,
    val previous: String,
    val seek: String,
    val stop: String,
    val capabilities: String,
    val status: String,
    val volume: String,
    val mute: String,
    val connect: String,
    val disconnect: String,
)

val basePlayerAPIEndpoint = PlayerAPIEndpoint(
    load = "/media/load/playlist",
    play = "/media/play",
    pause = "/media/pause",
    next = "/media/next",
    previous = "/media/previous",
    seek = "/media/seek",
    stop = "/media/stop",
    capabilities = "/device/capabilities",
    status = "/media/status",
    mute = "/media/mute",
    volume = "/media/volume",
    connect = "/device/connect",
    disconnect = "/device/disconnect"
)

@Serializable
class PlayDTO(val id: String)

@Serializable
class SeekDTO(val position: Float)

@Serializable
class MuteDTO(val value: Boolean)

@Serializable
class VolumeDTO(val value: Float)

@Serializable
class ConnectDTO(val name: String, val version: String)

@Serializable
@Parcelize
data class PlaylistItemDTO(
    val id: String,
    val url: String,
    val type: String,
    val duration: String,
    val metadata: MetadataDTO
) : Parcelable

@Serializable
@Parcelize
data class MetadataDTO(
    val title: String,
    val artistName: String,
    val albumName: String,
    val artworkUrl: String? = null,
    val format: String
) : Parcelable

@Serializable
class DeviceConnectDTO(
    var name:String,
    var version:String
)
@Serializable
class DeviceCapabilitiesDTO(
    val volume: Boolean,
    val video: Boolean,
    val stream: Array<String> = arrayOf("mpegh-dash", "hls"),
    val format: Array<String> = arrayOf("AURO-CODEC", "AURO-CX", "OTHER"),
    val device: String = "speaker",
    val channel: Array<String> = arrayOf("1", "2", "5.1", "7.1.4", "9.1.6")
)

@Serializable
class DeviceStatusDTO(
    val id: String,
    val state: StatusType,
    val position: Float? = 0.0f
)

@Serializable
class ErrorResponceDTO(
    val error:String,
    val code: Int?
)

enum class StatusType {
    PLAYING, PAUSED, BUFFERING, ENDED
}