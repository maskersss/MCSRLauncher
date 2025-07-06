package com.redlimerl.mcsrlauncher.data.asset.rule

import kotlinx.serialization.Serializable

@Serializable
data class AssetRule(
    val action: AssetRuleAction,
    val os: AssetRuleOS? = null,
    val features: AssetRuleFeatures? = null
) {

    fun shouldAllow(): Boolean {
        var applied = true
        if (this.os != null && !this.os.apply()) applied = false

        return this.action == AssetRuleAction.ALLOW == applied
    }

}