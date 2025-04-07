package com.example.playersampleapp.extension

import android.content.Context
import android.content.pm.PackageManager
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Enumeration

fun Context.isOnTV(): Boolean {
    return (packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
            || packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK))
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
