package com.redlimerl.mcsrlauncher.data.asset.rule

import com.redlimerl.mcsrlauncher.data.device.DeviceArchitectureType
import com.redlimerl.mcsrlauncher.data.device.RuntimeOSType
import com.redlimerl.mcsrlauncher.util.OSUtils
import kotlinx.serialization.Serializable

@Serializable
class AssetRuleOS(
    val name: RuntimeOSType? = null,
    val version: String? = null,
    val arch: DeviceArchitectureType? = null
) {
    fun apply(): Boolean {
        if (this.arch != null && !this.arch.isOn()) return false
        if (this.name != null && !this.name.isOn()) return false
        if (this.version != null && !Regex(this.version).matches(OSUtils.getOSVersion())) return false
        return true
    }
}