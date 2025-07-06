package com.redlimerl.mcsrlauncher.util

import com.redlimerl.mcsrlauncher.data.device.DeviceOSType
import oshi.SystemInfo

object OSUtils {

    val systemInfo = SystemInfo()

    fun getOSType(): DeviceOSType {
        return DeviceOSType.CURRENT_OS
    }

    fun getOSVersion(): String {
        return System.getProperty("os.version")!!
    }

    fun getJavaVersion(): Int {
        val version = System.getProperty("java.version")
        return version.split(".").let {
            if (it[0] == "1") it[1].toInt()
            else it[0].toInt()
        }
    }

}