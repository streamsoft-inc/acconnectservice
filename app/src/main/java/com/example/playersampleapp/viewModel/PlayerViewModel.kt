package com.example.playersampleapp.viewModel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.playersampleapp.activity.FullScreenActivity
import com.example.playersampleapp.server.model.DeviceStatusDTO
import com.example.playersampleapp.server.model.PlaylistItemDTO
import com.example.playersampleapp.server.model.StatusType
import kotlin.math.roundToInt

class PlayerViewModel(application: Application, val player: ExoPlayer) : AndroidViewModel(application) {
    var playbackPosition: Long = 0
    private var currentPlaylist: List<PlaylistItemDTO> = listOf()
    private var mContext: Context? = null


    fun load(playlist: List<PlaylistItemDTO>) {
        currentPlaylist = playlist
        mContext?.let {
            val intent = Intent(mContext, FullScreenActivity::class.java)
            intent.putParcelableArrayListExtra("VIDEO_ITEMS", ArrayList(playlist))
            it.startActivity(intent)
        }
    }

    fun pause(value:Boolean) {
        if(value) {
            // pause
            player.pause()
            player.playWhenReady = false
        } else {
            // resume
            player.play()
        }
    }

    fun play(mediaId: String) {
        currentPlaylist.indexOfFirst { item -> mediaId == item.id }.takeIf { it != -1 }?.let {
            val index = player.currentMediaItemIndex
            if (index != it) player.seekTo(it, C.TIME_UNSET)
            player.playWhenReady = true
        }
    }

    fun previous() {
        player.seekToPrevious()
    }

    fun next() {
        if (player.hasNextMediaItem()) {
            player.seekToNext()
            player.playWhenReady = true
        }
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    fun stop() {
        player.stop()
        player.release()
    }

    fun mute(mute: Boolean) {
        player.volume = if (mute) 0f else 1f
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun setVolume(volume: Int) {
        val audioManager = mContext?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isVolumeControlSupported = audioManager.isVolumeFixed.not()
        val canAdjustMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != AudioManager.ADJUST_MUTE

        if (!isVolumeControlSupported || !canAdjustMusicVolume) {
            throw RuntimeException("Volume adjustment not supported")
        }

        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val minVolume = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        val targetVolume = minVolume + (volume * (maxVolume - minVolume) / 100f).roundToInt()

        player.volume = targetVolume.toFloat() / maxVolume
    }

    fun status(): DeviceStatusDTO {
        val index = player.currentMediaItemIndex
        val item = currentPlaylist.getOrNull(index)
        return if (item != null) {
            val isPlaying = player.isPlaying
            val isBuffering = isBuffering()
            val isPaused = isPaused()
            val position = player.currentPosition / 1000.0f
            val status = when {
                isPlaying -> StatusType.PLAYING
                isBuffering -> StatusType.BUFFERING
                isPaused -> StatusType.PAUSED
                else -> StatusType.ENDED
            }
            DeviceStatusDTO(item.id, status, position)
        } else {
            DeviceStatusDTO("1", StatusType.ENDED, 0.0f)
        }
    }

    private fun isBuffering() : Boolean {
        return player.playbackState == Player.STATE_BUFFERING
    }

    private fun isPaused() = player.run {
        playbackState == Player.STATE_READY && !playWhenReady
    }

    fun setupWith(context: Context) {
        mContext = context
    }
}