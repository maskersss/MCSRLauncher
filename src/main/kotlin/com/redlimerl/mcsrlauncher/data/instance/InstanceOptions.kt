package com.redlimerl.mcsrlauncher.data.instance

import kotlinx.serialization.Serializable

@Serializable
data class InstanceOptions(
    var minMemory: Int = 512,
    var maxMemory: Int = 2048,
    var javaPath: String = "",
    var jvmArguments: String = ""
)
