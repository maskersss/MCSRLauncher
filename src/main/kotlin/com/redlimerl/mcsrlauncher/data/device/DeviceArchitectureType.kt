package com.redlimerl.mcsrlauncher.data.device

import com.redlimerl.mcsrlauncher.util.OSUtils
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DeviceArchitectureType(val bit: Int) {

    @SerialName("x86_64") X64(64),
    @SerialName("x86") X86(32),
    @SerialName("arm64") ARM64(64),
    @SerialName("arm32") ARM32(32);

    companion object {
        val CURRENT_ARCHITECTURE = let {
            val isArm = System.getProperty("os.arch").lowercase().let { it.startsWith("arm") || it.equals("aarch64", true) }
            val is64Bit = OSUtils.systemInfo.operatingSystem.bitness == 64
            if (isArm) {
                if (is64Bit) X64
                else X86
            } else {
                if (is64Bit) ARM64
                ARM32
            }
        }
    }

    fun isOn(): Boolean {
        return this == CURRENT_ARCHITECTURE
    }
}