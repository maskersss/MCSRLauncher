package com.redlimerl.mcsrlauncher.data.instance

import com.redlimerl.mcsrlauncher.data.meta.IntermediaryType
import kotlinx.serialization.Serializable

@Serializable
data class FabricVersionData(
    val loaderVersion: String,
    val intermediaryType: IntermediaryType,
    val intermediaryVersion: String
)
