package com.redlimerl.mcsrlauncher.data.meta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class IntermediaryType(val intermediaryName: String, val recommendLevel: Int) {
    @SerialName("net.fabricmc:intermediary") FABRIC("Fabric Intermediary", Int.MAX_VALUE),
    @SerialName("net.legacyfabric:intermediary") LEGACY_FABRIC_V1("Legacy Fabric Intermediary", 10),
    @SerialName("net.legacyfabric.v2:intermediary") LEGACY_FABRIC_V2("Legacy Fabric Intermediary (v2)", 8),
    @SerialName("net.ornithemc:calamus-intermediary") ORNITHEMC_GEN1("Ornithemc Intermediary (Gen1)", 5),
    @SerialName("net.ornithemc:calamus-intermediary-gen2") ORNITHEMC_GEN2("Ornithemc Intermediary (Gen2)", 3);

    override fun toString(): String {
        return this.intermediaryName
    }
}