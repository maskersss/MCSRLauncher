package com.redlimerl.mcsrlauncher.instance.mod

import kotlinx.serialization.Serializable

@Serializable
data class FabricModJson(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val icon: String? = null
)
