package com.example.playersampleapp.activity

import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.example.playersampleapp.R
import com.example.playersampleapp.server.model.PlaylistItemDTO
import com.example.playersampleapp.viewModel.PlayerViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


class FullScreenActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private val playerViewModel: PlayerViewModel by viewModel()

    @RequiresApi(Build.VERSION_CODES.R)
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen)
        hideStatusBar()

        playerView = findViewById(R.id.playerView)

        val items: ArrayList<PlaylistItemDTO>? = intent.getParcelableArrayListExtra("VIDEO_ITEMS")

        playerViewModel.player.stop()
        playerViewModel.player.clearMediaItems()

        items?.let {
            val mediaSources: List<MediaSource> = createMediaSources(it)
            playerViewModel.player.setMediaSources(mediaSources)
            playerViewModel.player.prepare()
        }

        playerView.player = playerViewModel.player
        playerViewModel.player.seekTo(playerViewModel.playbackPosition)
        playerViewModel.player.playWhenReady = true
    }

    @OptIn(UnstableApi::class)
    fun createMediaSources(playlist: List<PlaylistItemDTO>): List<MediaSource> {
        val dataSourceFactory = DefaultHttpDataSource.Factory()

        return playlist.map { item ->
            val mediaItem = MediaItem.Builder()
                .setUri(item.url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.metadata.title)
                        .setArtist(item.metadata.artistName)
                        .setAlbumTitle(item.metadata.albumName)
                        .setArtworkUri(item.metadata.artworkUrl?.let { Uri.parse(it) })
                        .build()
                )
                .setTag(item) // You can keep the full DTO for later reference
                .build()

            val type = Util.inferContentType(Uri.parse(item.url))

            when (type) {
                C.CONTENT_TYPE_DASH -> DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
                C.CONTENT_TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
                C.CONTENT_TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
                else -> throw IllegalStateException("Unsupported media type: ${item.url}")
            }
        }
    }


    override fun onStop() {
        super.onStop()
        playerViewModel.playbackPosition = playerViewModel.player.currentPosition
        playerViewModel.player.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun releasePlayer() {
        playerViewModel.player.release()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun hideStatusBar() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}