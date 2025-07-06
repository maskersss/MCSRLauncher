package com.redlimerl.mcsrlauncher.data.asset

import kotlinx.serialization.Serializable

@Serializable
data class HashAssetObject(
    val hash: String,
    val size: Long
)