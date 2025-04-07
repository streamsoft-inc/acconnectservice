package com.example.playersampleapp.server

import android.os.Handler
import android.os.Looper
import com.example.playersampleapp.server.model.ConnectDTO
import com.example.playersampleapp.server.model.DeviceCapabilitiesDTO
import com.example.playersampleapp.server.model.DeviceConnectDTO
import com.example.playersampleapp.server.model.DeviceStatusDTO
import com.example.playersampleapp.server.model.ErrorResponceDTO
import com.example.playersampleapp.server.model.MediaCommands
import com.example.playersampleapp.server.model.MuteDTO
import com.example.playersampleapp.server.model.PlayDTO
import com.example.playersampleapp.server.model.PlayerAPIEndpoint
import com.example.playersampleapp.server.model.PlaylistItemDTO
import com.example.playersampleapp.server.model.SeekDTO
import com.example.playersampleapp.server.model.StatusType
import com.example.playersampleapp.server.model.VolumeDTO
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Enumeration

typealias HttpCallbackResponse = (HttpMediaServerEvent<*>?) -> Unit
data class HttpMediaServerEvent<T>(val payload: Result<T>)

data class AddressInfo(val host: String, val port: Int)

interface RequestCallbackChannel {
    fun status() : DeviceStatusDTO?
    fun device() : DeviceConnectDTO?
}

class EmbeddedMediaHttpServer(val endpoints: PlayerAPIEndpoint) {

    var callback : HttpCallbackResponse = {}

    var requestCallbackChannel : RequestCallbackChannel = object : RequestCallbackChannel {
        override fun status(): DeviceStatusDTO? {
            return null
        }

        override fun device(): DeviceConnectDTO? {
            return null
        }
    }


    private fun success(mediaCommands: MediaCommands) {
        Handler(Looper.getMainLooper()).post {
            callback.invoke(HttpMediaServerEvent(Result.success(mediaCommands)))
        }
    }

    private fun error(throwable: Throwable) {
        callback.invoke(HttpMediaServerEvent(Result.failure<Throwable>(throwable)))
    }
    private var engine: ApplicationEngine? = null

    @OptIn(ExperimentalSerializationApi::class)
    fun start() {

        Thread {
            // listen on local network and bind on "all interfaces"
            engine = embeddedServer(Netty, port = 0, host = "0.0.0.0") {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    })
                }
                routing {
                    post(endpoints.load) {
                        println("[ACC] server - request load")
                        try {

                            val list = call.receive<List<PlaylistItemDTO>>()
                            success(MediaCommands.Load(list))

                            call.respond(HttpStatusCode.NoContent)
                        } catch (t: Throwable) {
                            t.printStackTrace()
                        }
                    }
                    post(endpoints.play) {
                        println("[ACC] server - request play")
                        try {

                            val payload = call.receive<PlayDTO>()
                            success(MediaCommands.Play(payload))

                            call.respond(HttpStatusCode.NoContent)
                        } catch (t: Throwable) {
                            t.printStackTrace()
                        }
                    }
                    post(endpoints.pause) {
                        println("[ACC] server - request pause")
                        try {
                            val param = call.request.queryParameters["value"] ?: "true"
                            success(MediaCommands.Pause( param.toBoolean() ))
                            call.respond(HttpStatusCode.NoContent)
                        } catch (t: Throwable) {
                            t.printStackTrace()
                        }
                    }
                    post(endpoints.previous) {
                        println("[ACC] server - request previuos")
                        try {

                            success(MediaCommands.Previous)
                            call.respond(HttpStatusCode.NoContent)
                        } catch (t: Throwable) {
                            t.printStackTrace()
                        }
                    }
                    post(endpoints.next) {
                        println("[ACC] server - request next")
                        try {

                            success(MediaCommands.Next)
                            call.respond(HttpStatusCode.NoContent)
                        } catch (t: Throwable) {
                            t.printStackTrace()
                        }
                    }
                    post(endpoints.seek) {
                        try {
                            val payload = call.receive<SeekDTO>()
                            success(MediaCommands.SeekTo(payload))
                            call.respond(HttpStatusCode.NoContent)
                        } catch (t: Throwable) {
                            t.printStackTrace()
                        }
                    }
                    post(endpoints.stop) {
                        try {
                            success(MediaCommands.Stop)
                            call.respond(HttpStatusCode.NoContent)
                        } catch (t: Throwable) {
                            t.printStackTrace()
                        }
                    }
                    post(endpoints.mute) {
                        try {
                            val payload = call.receive<MuteDTO>()
                            success(MediaCommands.Mute(payload))
                            call.respond(HttpStatusCode.NoContent)
                        } catch (t: Throwable) {
                            t.printStackTrace()
                        }
                    }
                    post(endpoints.volume) {
                        try {
                            println("[ACC] server - volume")
                            val volumeDTO = call.receive<VolumeDTO>()

                            success(MediaCommands.SetVolume(volumeDTO))
                            call.respond(HttpStatusCode.NoContent)
                        }catch (e: Exception) {
                            println("[ACC] failure - volume")
                            e.printStackTrace()
                            call.respond(HttpStatusCode.BadRequest, ErrorResponceDTO("Device doesn't support volume level change.", code = null))
                        }
                    }
                    post(endpoints.connect) {
                        try {
                            println("[ACC] server - connect")
                            val connectDTO = call.receive<ConnectDTO>()
                            success(MediaCommands.Connect(connectDTO))
                            val device = requestCallbackChannel.device()
                            device?.let {
                                println("[ACC] server - respond $it")
                                call.respond(HttpStatusCode.OK, device)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    post(endpoints.disconnect){
                        println("[ACC] server - disconnect")
                        success(MediaCommands.Disconnect())
                        call.respond(HttpStatusCode.OK)
                    }
                    get(endpoints.status) {
                        println("[ACC] server call - status")
                        try {
                            val status = requestCallbackChannel.status()
                            status?.let {
                                call.respond(HttpStatusCode.OK, status)
                            } ?: call.respond(HttpStatusCode.OK, DeviceStatusDTO("1", StatusType.ENDED, 0.0f))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            call.respond(HttpStatusCode.BadRequest, ErrorResponceDTO("Error, unable to get current status.", code = null))
                        }
                    }
                    get(endpoints.capabilities) {
                        try {
                            call.respond(HttpStatusCode.OK, DeviceCapabilitiesDTO(volume = true, video = true))
                        } catch (e: Exception) {
                            println("[ACC] ERROR - status")
                            e.printStackTrace()
                            call.respond(HttpStatusCode.BadRequest, ErrorResponceDTO("Error, unable to get current capabilities.", code = null))
                        }
                    }
                }
            }
            engine?.start(wait = false)

            println("server init - engine created")
            (engine?.application?.environment as ApplicationEngineEnvironment?)?.connectors?.forEach {
                println("server init, address - ${it.host}:${it.port}")
            }
            val port = runBlocking { engine?.resolvedConnectors()?.firstOrNull()?.port } ?: return@Thread
            println("server init - read - port $port")

            getIP()?.let { ipAdress ->
                callback.invoke(HttpMediaServerEvent(
                    Result.success(AddressInfo(ipAdress, port))
                ))
            }

        }.start()
    }
    fun stop() {
        println("server init - stop req")
        engine?.stop()
        println("server init - stop done")
    }
    fun getIP(): String? {
        println("ip - ${InetAddress.getLocalHost().hostName} ${InetAddress.getLocalHost().hostAddress}")
        try {
            val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf: NetworkInterface = en.nextElement()
                val enumIpAddr: Enumeration<InetAddress> = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress: InetAddress = enumIpAddr.nextElement()
                    val ipAddress = inetAddress.hostAddress
                    if (!inetAddress.isLoopbackAddress && ipAddress != null && !ipAddress.contains(":")) {
                        println("ip-address: $ipAddress")
                        return ipAddress
                    }
                }
            }
        } catch (ex: SocketException) {
            ex.printStackTrace()
        }
        return null
    }
}