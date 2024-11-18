package com.github.syafiqq.nsd_test_001.helper

import android.content.Context
import android.net.wifi.WifiManager

fun Context.setupWifiMulticast(): WifiManager.MulticastLock? {
    val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
    val multicastLock = wifiManager.createMulticastLock("multicastLock")
    multicastLock?.setReferenceCounted(true)
    multicastLock?.acquire()

    return multicastLock
}