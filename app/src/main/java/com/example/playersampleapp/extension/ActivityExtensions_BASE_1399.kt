package com.example.playersampleapp.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.util.TypedValue
import android.view.WindowManager
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Enumeration


fun Activity.allowScreenshot(allow: Boolean) {
    if (allow) {
        window?.clearFlags(
            WindowManager.LayoutParams.FLAG_SECURE
        )
    } else {
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

    }
}

fun Activity.requireUserOrientation() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
}

fun Activity.requirePortraitOrientation() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
}

fun Activity.requireLandscapeOrientation() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
}

fun Context.isOnTV(): Boolean {
    return (packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
            || packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK))
}

fun Intent.openedFromHistory(): Boolean {
    return ((flags ?: 0) and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
}

fun Context.getDPSize(value: Float): Float {
    val metrics = resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, metrics)
}

fun Context.deviceSupports360Audio(): Boolean {
    return packageManager.hasSystemFeature("com.sony.360ra")
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
