package com.redlimerl.mcsrlauncher.data.device

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import oshi.PlatformEnum
import oshi.SystemInfo

@Serializable
enum class DeviceOSType {

    @SerialName("windows") WINDOWS,
    @SerialName("linux") LINUX,
    @SerialName("osx") OSX;

    companion object {
        val CURRENT_OS = let {
            val osClassifier = when (SystemInfo.getCurrentPlatform()) {
                PlatformEnum.WINDOWS -> WINDOWS
                PlatformEnum.LINUX -> LINUX
                PlatformEnum.MACOS -> OSX
                else -> throw IllegalArgumentException("Unknown OS: ${SystemInfo.getCurrentPlatform().getName()}")
            }
            osClassifier
        }
    }

    fun isOn(): Boolean {
        return this == CURRENT_OS
    }

}