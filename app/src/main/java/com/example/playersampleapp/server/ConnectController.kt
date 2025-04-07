package com.example.playersampleapp.server

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.datasource.cache.Cache
import com.example.playersampleapp.BuildConfig
import com.example.playersampleapp.server.model.DeviceConnectDTO
import com.example.playersampleapp.server.model.DeviceStatusDTO
import com.example.playersampleapp.server.model.MediaCommands
import com.example.playersampleapp.viewModel.PlayerViewModel

class ConnectController(
    val mediaHttpServer: EmbeddedMediaHttpServer,
    private val viewModel: PlayerViewModel,
    private val nsdController: NSDController,
    @SuppressLint("UnsafeOptInUsageError") private val cache: Cache,
) {
    private var mContext: Context? = null
    private var nsdStarted: Boolean = false
    private val _serverAddress = MutableLiveData<Pair<String, String>>()
    val serverAddress: LiveData<Pair<String, String>> get() = _serverAddress

    private val status: ServiceStatusListener = {
        it?.let {
            println("status change - $it")
        }
    }

    private val mediaServerCallback : HttpCallbackResponse = {

        it?.payload?.onSuccess { command ->
            when (command) {
                is MediaCommands.Connect -> {

                    this.mContext?.let {
                        val toast = Toast.makeText(it, "Connected to " + command.connectDTO.name, Toast.LENGTH_LONG)
                        toast.show()
                    }
                }
                is MediaCommands.Disconnect -> {
                    this.mContext?.let {
                        val toast =
                            Toast.makeText(it, "Disconnected from device", Toast.LENGTH_LONG)
                        toast.show()
                    }
                }
                is AddressInfo -> {
                    println("[ACC] callback received ${command.host} - ${command.port}")
                    _serverAddress.postValue(Pair(command.host, command.port.toString()))

                    nsdController.status = status
                    nsdStarted = nsdController.start(command.port)
                }
                is MediaCommands.Load -> {
                    println("[ACC] load")
                    viewModel.load(command.playlist)
                }
                is MediaCommands.Pause -> {
                    println("[ACC] pause")
                    viewModel.pause(command.value)
                }
                is MediaCommands.Play -> {
                    println("[ACC] play")
                    readStatus()
                    viewModel.play(command.play.id)
                }
                is MediaCommands.Previous -> {
                    println("[ACC] previous")
                    viewModel.previous()
                }
                is MediaCommands.Next -> {
                    println("[ACC] next")
                    viewModel.next()
                }
                is MediaCommands.Stop -> {
                    println("[ACC] stop")
                    viewModel.stop()
                }
                is MediaCommands.SeekTo -> {
                    val position = command.seekDTO.position
                    println("[ACC] seek to $position")
                    viewModel.seekTo((position * 1000).toLong())
                }
                is MediaCommands.Mute -> {
                    val mute = command.muteDTO
                    println("[ACC] mute to $mute")
                    viewModel.mute(mute.value)
                }
                is MediaCommands.SetVolume -> {
                    val volume = command.volumeDTO.value
                    println("[ACC] volume to $volume")
                    viewModel.setVolume((volume * 100).toInt())
                }
            }
        }
    }

    private var lastStatus: DeviceStatusDTO? = null

    private fun readStatus() {
        lastStatus = viewModel.status()
    }

    fun start(context: Context) {
        mContext = context
        Thread{
            mediaHttpServer.stop()
            nsdController.stop()
            viewModel.setupWith(context)
            nsdController.setupWith(context)
            Thread.sleep(2000)
            mediaHttpServer.callback = mediaServerCallback
            mediaHttpServer.requestCallbackChannel = object : RequestCallbackChannel {
                override fun status(): DeviceStatusDTO? {
                    Handler(Looper.getMainLooper()).post  {
                        readStatus()
                    }
                    return lastStatus
                }

                override fun device() : DeviceConnectDTO? {
                    val name = BuildConfig.SERVICE_NAME
                        .plus( "-")
                        .plus( Build.MANUFACTURER)
                        .plus(" ")
                        .plus(Build.MODEL)
                        .plus(" v")
                        .plus(Build.VERSION.RELEASE)

                    val version = BuildConfig.VERSION_NAME
                        .plus(" v")
                        .plus(BuildConfig.APP_BUILD_CODE)

                    return DeviceConnectDTO(name, version )
                }
            }
            mediaHttpServer.start()

        }.start()

    }

    fun stop() {
        mediaHttpServer.stop()
        if (nsdStarted) nsdController.stop()
    }
}