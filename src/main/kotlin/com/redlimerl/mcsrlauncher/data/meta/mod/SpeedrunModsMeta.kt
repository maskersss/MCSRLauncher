package com.redlimerl.mcsrlauncher.data.meta.mod

import com.redlimerl.mcsrlauncher.data.meta.IntermediaryType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpeedrunModMeta(
    @SerialName("modid") val modId: String,
    val name: String,
    val description: String,
    val sources: String,
    val versions: List<SpeedrunModVersion>,
    val traits: List<SpeedrunModTrait> = listOf(),
    val incompatibilities: List<String>,
    val recommended: Boolean = true
)

@Serializable
data class SpeedrunModVersion(
    @SerialName("target_version") val gameVersions: List<String>,
    val version: String,
    val url: String,
    val hash: String,
    val intermediary: List<IntermediaryType>
)