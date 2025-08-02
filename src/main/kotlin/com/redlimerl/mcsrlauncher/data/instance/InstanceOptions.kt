package com.redlimerl.mcsrlauncher.data.instance

import com.redlimerl.mcsrlauncher.MCSRLauncher
import com.redlimerl.mcsrlauncher.data.launcher.LauncherSharedOptions
import com.redlimerl.mcsrlauncher.launcher.InstanceManager
import kotlinx.serialization.Serializable

@Serializable
data class InstanceOptions(
    var useLauncherOption: Boolean = true,
    var autoModUpdates: Boolean = false,
    override var minMemory: Int = 512,
    override var maxMemory: Int = 2048,
    override var javaPath: String = "",
    override var jvmArguments: String = "",
    override var maximumResolution: Boolean = false,
    override var resolutionWidth: Int = 854,
    override var resolutionHeight: Int = 480
) : LauncherSharedOptions {

    fun save() {
        InstanceManager.save()
    }

    fun <T> getSharedValue(sharedOptions: (LauncherSharedOptions) -> T): T {
        return sharedOptions(if (useLauncherOption) MCSRLauncher.options else this)
    }

}
