package com.redlimerl.mcsrlauncher.data.asset.rule

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AssetRuleFeatures(
    @SerialName("is_demo_user")
    val isDemoUser: Boolean? = null,

    @SerialName("has_custom_resolution")
    val hasCustomResolution: Boolean? = null,

    @SerialName("has_quick_plays_support")
    val hasQuickPlaysSupport: Boolean? = null,

    @SerialName("is_quick_play_singleplayer")
    val isQuickPlaySingleplayer: Boolean? = null,

    @SerialName("is_quick_play_multiplayer")
    val isQuickPlayMultiplayer: Boolean? = null,

    @SerialName("is_quick_play_realms")
    val isQuickPlayRealms: Boolean? = null
)