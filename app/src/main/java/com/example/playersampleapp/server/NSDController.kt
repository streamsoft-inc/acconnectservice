package com.example.playersampleapp.server

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import java.net.InetAddress

typealias ServiceStatusListener = (NSDController.ServiceRegisterStatus?) -> Unit

class NSDController {

    sealed class ServiceRegisterStatus {
        class Registered(val nsdServiceInfo: NsdServiceInfo?): ServiceRegisterStatus()
        class Unregistered(val nsdServiceInfo: NsdServiceInfo?): ServiceRegisterStatus()
        class RegisterFailed(val nsdServiceInfo: NsdServiceInfo?, errorCode: Int): ServiceRegisterStatus()
        class UnregisterFailed(val serviceInfo: NsdServiceInfo?, errorCode: Int): ServiceRegisterStatus()
    }

    private val acService = "_acconnect_streaming._tcp"
    private var manager : NsdManager? = null

    var status : ServiceStatusListener = {}
    var lastStatus : ServiceRegisterStatus? = null


    private val advertiseListener =  object : NsdManager.RegistrationListener {
        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            lastStatus = ServiceRegisterStatus.RegisterFailed(serviceInfo, errorCode)
            status(lastStatus)

            manager?.unregisterService(object : NsdManager.RegistrationListener{
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {

                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {

                }

                override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {

                }

                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {

                }

            })
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            lastStatus = ServiceRegisterStatus.UnregisterFailed(serviceInfo, errorCode)
            status(lastStatus)
        }

        override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
            lastStatus = ServiceRegisterStatus.Registered(serviceInfo)
            status(lastStatus)
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
            lastStatus = ServiceRegisterStatus.Unregistered(serviceInfo)
            status(lastStatus)
        }
    }

    fun setupWith(context: Context) {
        this.manager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    fun stop() {
        try {
            manager?.unregisterService(advertiseListener)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    fun start(port: Int): Boolean {
        println("nsd - start with $port")
        try {
            manager?.registerService(NsdServiceInfo().apply {
                serviceName = "Artist Connection TV"
                serviceType = acService
                setAttribute("manufacturer", "Streamsoft Inc.")
                setAttribute("modelName", Build.MANUFACTURER
                    .plus(" ")
                    .plus(Build.MODEL)
                    .plus(" v")
                    .plus(Build.VERSION.RELEASE))
                setPort(port)
                host = InetAddress.getLocalHost()
            }, NsdManager.PROTOCOL_DNS_SD, advertiseListener)
            return true
        } catch (e: java.lang.IllegalArgumentException) {
            e.printStackTrace()
        }
        return false
    }
}