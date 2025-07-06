package com.redlimerl.mcsrlauncher.data.asset.rule

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AssetRuleAction {
    @SerialName("allow") ALLOW,
    @SerialName("disallow") DISALLOW
}