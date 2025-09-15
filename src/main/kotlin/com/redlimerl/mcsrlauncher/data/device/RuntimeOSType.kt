package com.redlimerl.mcsrlauncher.data.device

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RuntimeOSType(private val osType: DeviceOSType, private val architecture: DeviceArchitectureType? = null) {

    @SerialName("windows") WINDOWS(DeviceOSType.WINDOWS),
    @SerialName("linux") LINUX(DeviceOSType.LINUX),
    @SerialName("mac-os") MAC_OS(DeviceOSType.MACOS),
    @SerialName("osx") OSX(DeviceOSType.MACOS),
    @SerialName("windows-x64") WINDOWS_64(DeviceOSType.WINDOWS, DeviceArchitectureType.X64),
    @SerialName("windows-arm64") WINDOWS_ARM64(DeviceOSType.WINDOWS, DeviceArchitectureType.ARM64),
    @SerialName("windows-x86") WINDOWS_86(DeviceOSType.WINDOWS, DeviceArchitectureType.X86),
    @SerialName("linux-x64") LINUX_64(DeviceOSType.LINUX, DeviceArchitectureType.X64),
    @SerialName("linux-arm32") LINUX_ARM32(DeviceOSType.LINUX, DeviceArchitectureType.ARM32),
    @SerialName("linux-arm64") LINUX_ARM64(DeviceOSType.LINUX, DeviceArchitectureType.ARM64),
    @SerialName("mac-os-x64") MAC_OS_64(DeviceOSType.MACOS, DeviceArchitectureType.X64),
    @SerialName("mac-os-arm64") MAC_OS_ARM64(DeviceOSType.MACOS, DeviceArchitectureType.ARM64),
    @SerialName("osx-x64") OSX_64(DeviceOSType.MACOS, DeviceArchitectureType.X64),
    @SerialName("osx-arm64") OSX_ARM64(DeviceOSType.MACOS, DeviceArchitectureType.ARM64);

    fun isOn(): Boolean {
        return this.osType.isOn() && (this.architecture == null || this.architecture.isOn())
    }

    fun getLevel(): Int {
       return if (this.architecture == null) 1 else 2
    }

    companion object {
        fun current(): RuntimeOSType {
            return entries.filter { it.architecture != null }.find { it.isOn() }
                ?: entries.filter { it.architecture == null }.find { it.isOn() }
                ?: throw IllegalStateException("Not supported OS")
        }
    }

}